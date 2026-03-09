package fr.smolder.mirage.core.port;

@FunctionalInterface
public interface PermissionProvider {
	boolean canExecute(Object sender);

	static PermissionProvider allowAll() {
		return sender -> true;
	}
}
