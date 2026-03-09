package fr.smolder.mirage.core.cache;

import fr.smolder.mirage.core.model.SkinData;

import java.util.Optional;

public interface SkinCache extends AutoCloseable {
	Optional<SkinData> get(String tileHash);

	void put(String tileHash, SkinData skinData);

	default void close() {
	}
}
