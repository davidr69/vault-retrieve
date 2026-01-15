# Vault-Retrieve

## Purpose

---
Applications running in Kubernetes containers can implement logic to
obtain credentials from a vault. This logic must be re-implemented in
every application requiring credentials from a vault. While this is
somewhat trivial, there is an advantage if this logic is externalized:
the implementation can change (e.g. use a config server instead or a
different vault provider) with no changes to the application.

In order to achieve this, the vault-retrieve application must run in an init-container. The vault-retrieve app then writes the credentials to a volume mount, where the main container can access.

### Why not use secrets?
Secrets are not at all secret (unless etcd is configured to encrypt data at rest) and are not scalable. If a password changes which is used by 3 applications, there would presumably be 3 places to make those updates (assuming each application is in its own namespace). By using a vault, no changes are required to secrets or the application.

### What happens if the password changes while the application is running?
Applications would have to be restarted by deleting the pod since values from the vault are only read at startup. In order to handle a password change, applications require some re-architecting. One possible solution is to use a circuit breaker which notices failures and retries authenticating after accessing the vault. For cases like databases, the datasource bean must be modified, which is beyond the scope of this text.

## Flow diagram

The conceptual flow is depicted below:

![vault-retrieve.png](images/vault-retrieve.png)

1. When the pod is created, the `init container` starts the `vault-retrieve` application. That application communicates with Azure. `vault-retrieve` authenticates with the client ID and tenant ID from the Spring Cloud Config server and the client secret from the Kubernetes secret.
1. `vault-retrieve` places keys and values in the volume mount within the pod.
1. `vault-retrieve` terminates, allowing the `main` container to start. The `main` container obtains the credentials from the volume mount.
1. The application in the `main` container uses the credentials to 
authenticate to the database.

How does the main container acquire the desired parameters? In the case
of database credentials, logic can be added to the DataSource bean to check
for the existence of a paramater name that points to the configuration file
(e.g. `/var/tmp/vault.properties`), read and parse each line, and assign the values, but there is a better solution ...

### spring.config.additional-location

`spring.config.additional-location` is a Spring feature that adds to the sources from where parameters and values can be obtained. Not only will the properties files in the classpath be evaluated (`src/main/resources` or `src/test/resources` by default), but Spring can also read from the file we dynamically create, providing that it conforms to a file structure it can parse. `vault-retrieve` writes in the `application.properties` format rather than the `application.yml` format for simplicity (and it's ephemeral anyway). Leveraging `spring.config.additional-location` means that the application in the main container does not need to make ANY modifications in order to benefit
from `vault-retrieve`.

## Running vault-retrieve

The application relies on specific parameters, to determine how to access the vault, which attributes to query, and where to write the resulting file.

The application authenticates to Azure Vault using the required parameters: client ID, tenant ID, client secret, and vault URI. While this can be provided in a standard application.yml file, we obtain it from Spring Cloud Config by bootstrapping the config server from our Kubernetes deployment descriptor:

```yaml
  - name: SPRING_APPLICATION_NAME # app name for config client
    value: vault
  - name: SPRING_PROFILES_ACTIVE # config client uses this, not vault-retrieve
    value: dbdev
  - name: SPRING_CLOUD_LABEL # branch, also for config
    value: main
  - name: SPRING_CONFIG_IMPORT # tell config client where config server is
    value: "configserver:http://configs.lavacro.net:9080/"
```

That, in turn, obtains configuration parameters that can then be used to authenticate to Google Cloud. The JSON object within `auth` is obtained via a `key` on the service account:

```yaml
google:
  cloud:
    project: my_project
    auth: |
      {
        "type": "service_account",
        "project_id": "my_project",
        "private_key_id": "****",
        "private_key": "-----BEGIN PRIVATE KEY-----\nMII****\n-----END PRIVATE KEY-----\n",
        "client_email": "****@****.iam.gserviceaccount.com",
        "client_id": "****",
        "auth_uri": "https://accounts.google.com/o/oauth2/auth",
        "token_uri": "https://oauth2.googleapis.com/token",
        "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
        "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/****%40****.iam.gserviceaccount.com",
        "universe_domain": "googleapis.com"
      }

vault:
  mappings:
    lavacro-prod-db-pw: database.password
    lavacro-prod-db-user: database.user
```

Best practices dictate that a dedicated service account should be created with `Secret Manager | Secret Accessor` permissions on the secrets:

![google-secrets-manager](images/google-secrets-manager.png)

In the YAML returned by the config server, I use `mappings` to indicate what the name of the field is in the vault and what key it should be written as in the properties file. Using the above example, the definition for `lavacro-prod-db-pw` indicates that we should search for a secret named `lavacro-prod-db-pw` in the Secrets Manage and write its value using a key of `database.passsword`. The secret names can be viewed in plain text but require the `Secrets Manager Viewer` permission.

Another parameter required by `vault-retrieve` is MOUNT_PATH and the actual path is completely arbitrary. I choose to put it in the Kubernetes deployment.

![vault-vars.png](images/vault-vars.png)

## Configuring Kubernetes

The deployment needs to define both the `initContainer` and the app container. A typical application would resemble the following:

```yaml
spec:
  initContainers:
  - name: vault-retrieve
    image: "myprivaterepo:5000/vault-retrieve:2.0.0"
    imagePullPolicy: IfNotPresent
    env:
    - name: SPRING_APPLICATION_NAME # app name for config client
      value: vault
    - name: SPRING_PROFILES_ACTIVE # config client uses this, not vault-retrieve
      value: dbdev
    - name: SPRING_CLOUD_LABEL # branch, also for config
      value: main
    - name: SPRING_CONFIG_IMPORT # tell config client where config server is
      value: "configserver:http://configs.lavacro.net:9080/"
    volumeMounts:
    - mountPath: "/var/tmp/vault"
      name: vault-token
  containers:
  - name: gym
    image: "myprivaterepo:5000/gym:2.0.0"
    # lines omitted
    env:
    - name: additional_properties
      value: "/var/tmp/vault/vault.properties" # my app's Docker image needs to have -Dspring.config.additional-location=$additional_properties
    volumeMounts:
    - mountPath: "/var/tmp/vault"
      name: vault-token
  volumes:
  - name: vault-token
    emptyDir:
      sizeLimit: 1Mi
      medium: Memory # tmpfs in-memory only
```

Note that an `emptyDir` volume mount is used to persist the authentication information obtained by `vault-retrieve` and it has been definied with `medium: Memory`, making it a memory-only tmpfs mount.

## Main container

The application in the main container can actually be written in any language. The `key=value` format of the propery file written by the `initContainer` can easily be read by any application. Spring-based applications do not need to add any logic to use this as placeholder resolvers.

As of this release, the file created will always be named `vault.properties`
and is relative to the mount path. The file name was not parameterized to
avoid requiring yet more parameters.

![design-diagram.png](images/design-diagram.png)

The flow is as follows:

- the initContainer starts
- since `SPRING_CONFIG_IMPORT` is defined, Spring contacts the config server
    - note that the `hostKey` value in the config server is the ssh fingerprint, [documented here](https://bitbucket.org/blog/ssh-host-key-changes)
    - only place your private key here if the configuration file is encrypted before storing
- the config server locates, retrieves, and returns the appropriate config file
- Spring within the initContainer injects the parameters into `vault-retrieve`'s Environment (not the same as the OS environment)
- `vault-retrieve` is started with the obtained values
- `vault-retrieve` leverages the Azure libraries to retrieve the values specified in the mapped parameters which are part of the payload returned from the config server
- `vault-retrieve` writes the values to the mount path and terminates
- the main container starts, using `-Dspring.config.additional-location` pointing to the volume mount
- the configuration bean is created for whatever purpose that is required, in this case for database authentication

## Why not a Sidecar?

You are probably familiar with the ambassador, adapter, and sidecar
patterns. Ambassadors and adapters both proxy network connections,
and a sidecar runs "in parallel" to the main container. The reality is that
an initContainer satisfies the requirement quite well while consuming less
resources. This app was deployed to two namespaces, the first with
`vault-retrieve` as an initContainer, and the second as a sidecar:

![metrics.png](images/metrics.png)

There is another issue: `vault-retrieve` must run before the main container,
and there is no native way to make this happen before Kubernetes 1.28. Third,
if the sidecar doesn't exit but the main container terminates due to an error,
the pod will not be restarted since all containers must terminate for an
automatic restart attempt.