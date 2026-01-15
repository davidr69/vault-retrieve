package lavacro.com.vault.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VaultRequest {
	@JsonProperty("vault_request")
	private List<VaultPath> vaultRequest;

	@JsonProperty("mount_path")
	private String mountPath;
}
