package lavacro.com.vault.domain;

import java.util.List;

public class VaultPath {
	private String path;
	private List<VaultItem> items;

	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }

	public List<VaultItem> getItems() { return items; }
	public void setItems(List<VaultItem> items) { this.items = items; }
}
