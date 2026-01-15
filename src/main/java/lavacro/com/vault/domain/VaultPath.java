package lavacro.com.vault.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VaultPath {
	private String path;
	private List<VaultItem> items;
}
