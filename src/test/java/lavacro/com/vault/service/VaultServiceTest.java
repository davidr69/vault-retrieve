package lavacro.com.vault.service;

import lavacro.com.vault.domain.VaultRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.vault.core.VaultTemplate;

import java.lang.reflect.Field;

public class VaultServiceTest {
/*	@Test
	public void parseConfigTest() {
		VaultTemplate vaultTemplate = Mockito.mock(VaultTemplate.class);

		String json = """
		{
			"request": [{
				"path": "lavacro/data/prod/database/postgresql",
				"items": [{
					"key": "password",
					"label": "PGSQL_PASSWORD"
				}]
			},{
				"path": "lavacro/data/prod/someservice",
				"items": [{
					"key": "foo",
					"label": "bar"
				}]
			}]
		}
		""";

		VaultService vaultService = new VaultService(vaultTemplate);
		VaultRequest entry = vaultService.parseConfig(json);
		Assertions.assertNotNull(entry, "Parsed VaultRequest should not be null");
		Assertions.assertNotNull(entry.getRequest(), "Request list should not be null");
		Assertions.assertEquals(2, entry.getRequest().size(), "Expect two request entries");
	}

	@Test
	public void processTest() throws Exception {
		VaultTemplate vaultTemplate = Mockito.mock(VaultTemplate.class);

		String json = """
		{
			"request": [{
				"path": "lavacro/data/prod/database/postgresql",
				"items": [{
					"key": "password",
					"label": "PGSQL_PASSWORD"
				}]
			},{
				"path": "lavacro/data/prod/someservice",
				"items": [{
					"key": "foo",
					"label": "bar"
				}]
			}],
			"mount_path": "/path/to/mount"
		}
		""";

		VaultService vaultService = new VaultService(vaultTemplate);

		// Set the private vaultRequest field (normally injected by Spring) so process() can parse it
		Field vaultRequestField = VaultService.class.getDeclaredField("vaultRequest");
		vaultRequestField.setAccessible(true);
		vaultRequestField.set(vaultService, json);

		vaultService.process();
	}*/
}
