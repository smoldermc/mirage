package fr.smolder.mirage.core.cache;

import fr.smolder.mirage.core.model.SkinData;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemorySkinCache implements SkinCache {
	private final ConcurrentMap<String, SkinData> cache = new ConcurrentHashMap<>();

	@Override
	public Optional<SkinData> get(String tileHash) {
		return Optional.ofNullable(cache.get(tileHash));
	}

	@Override
	public void put(String tileHash, SkinData skinData) {
		cache.put(tileHash, skinData);
	}
}
