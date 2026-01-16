package lavacro.com.vault.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class VaultRequest {
	@JsonProperty("vault_request")
	private List<VaultPath> vaultRequest;

	@JsonProperty("mount_path")
	private String mountPath;

	public List<VaultPath> getVaultRequest() { return vaultRequest; }
	public void setVaultRequest(List<VaultPath> vaultRequest) { this.vaultRequest = vaultRequest; }

	public String getMountPath() { return mountPath; }
	public void setMountPath(String mountPath) { this.mountPath = mountPath; }
}
