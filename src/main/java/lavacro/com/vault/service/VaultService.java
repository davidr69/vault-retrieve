package lavacro.com.vault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lavacro.com.vault.VaultRetrieveException;
import lavacro.com.vault.configuration.VaultConfig;
import lavacro.com.vault.domain.VaultItem;
import lavacro.com.vault.domain.VaultPath;
import lavacro.com.vault.domain.VaultRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * something
 * The request is in the form of:
 * 		        {
 * 			"request": [{
 * 				"path": "lavacro/data/prod/database/postgresql",
 * 				"items": [{
 * 					"key": "password",
 * 					"label": "PGSQL_PASSWORD"
 *                },{
 * 					"key": "username",
 * 					"label": "PGSQL_USERNAME"
 *                }]
 *            },{
 * 				"path": "lavacro/data/prod/someservice",
 * 				"items": [{
 * 					"key": "foo",
 * 					"label": "bar"
 *                }]
 *            }],
 * 			"mount_path": "/path/to/mount"
 *        }
 * ...
 * 			vaultResponse will look like (per path):
 * 			            {
 * 				data: {
 * 					hostname: prod-db.lavacro.net,
 * 					password: ****,
 * 					username: david
 *                },
 * 				metadata: {
 * 					created_time: 2025-12-02T03:18:46.593801335Z,
 * 					custom_metadata=null,
 * 					deletion_time: null,
 * 					destroyed=false,
 * 					version=1
 *                }
 *            }
 */
@Service
public class VaultService {
	private static final Logger logger = LoggerFactory.getLogger(VaultService.class);

	private final VaultConfig vaultConfig;
	private final VaultTemplate vaultTemplate;

	public VaultService(VaultTemplate vaultTemplate, VaultConfig vaultConfig) {
		this.vaultTemplate = vaultTemplate;
		this.vaultConfig = vaultConfig;
	}

	@PostConstruct
	public void process() {
		VaultRequest request = parseConfig(vaultConfig.getRequest());

		if (request == null) {
			logger.error("Request is null or invalid");
			return;
		}

		String vaultProperties = request.getMountPath();

		try (
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(vaultProperties))
		) {

			for(VaultPath path: request.getVaultRequest()) {
				logger.info("Processing {} ...", path.getPath());
				Map<String,String> keyLabelMap = path.getItems().stream()
						.collect(Collectors.toMap(VaultItem::getKey, VaultItem::getLabel));

				VaultResponse vaultResponse = getVaultResponse(path.getPath());

				Map<String,String> data = getVaultValues((Map<String,Object>) vaultResponse.getData().get("data"), keyLabelMap);

				for(Map.Entry<String, String> entry: data.entrySet()) {
					bufferedWriter.write(entry.getKey());
					bufferedWriter.write("=");
					bufferedWriter.write(entry.getValue());
					bufferedWriter.newLine();
				}
			}
		} catch(IOException e) {
			logger.error("IO exception: {}", e.getMessage());
			System.exit(1);
		}
	}

	VaultResponse getVaultResponse(String path) {
		try {
			VaultResponse vaultResponse = vaultTemplate.read(path);
			if(vaultResponse.getData() == null) {
				logger.error("No data found at path: {}", path);
				System.exit(1);
			}
			return vaultResponse;
		} catch(Exception e) {
			logger.error("Error reading from vault: {}", e.getMessage());
			throw new VaultRetrieveException(e);
		}
	}

	Map<String,String> getVaultValues(Map<String,Object> store, Map<String,String> keyLabelMap) {
		Map<String, String> data = new HashMap<>();

		// "store" has what we got from vault, and not every entry may have been requested
		for(Map.Entry<String, Object> entry: store.entrySet()) {
			String value = (String) entry.getValue();
			if (value != null) {
				if(!keyLabelMap.containsKey(entry.getKey())) {
					logger.info("Key {} not requested, skipping", entry.getKey());
					continue;
				}
				data.put(keyLabelMap.get(entry.getKey()), value);
			} else {
				logger.warn("No value found in vault for key: {}", entry.getKey());
			}
		}
		return data;
	}

	VaultRequest parseConfig(String json) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(json, VaultRequest.class);
		} catch(IOException e) {
			logger.error("Could not deserialize VaultEntry: {}", json, e);
		}
		return null;
	}
}
