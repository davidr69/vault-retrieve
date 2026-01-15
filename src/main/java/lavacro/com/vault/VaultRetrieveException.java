package lavacro.com.vault;

public class VaultRetrieveException extends RuntimeException {
	public VaultRetrieveException(String message) {
		super(message);
	}

	public VaultRetrieveException(String message, Throwable cause) {
		super(message, cause);
	}

	public VaultRetrieveException(Throwable cause) {
		super(cause);
	}
}
