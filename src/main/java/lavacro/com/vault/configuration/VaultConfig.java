package lavacro.com.vault.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vault")
public class VaultConfig {
	private String request;

	public String getRequest() { return request; }
	public void setRequest(String request) { this.request = request; }
}
