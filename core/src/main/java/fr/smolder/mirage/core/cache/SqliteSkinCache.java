package fr.smolder.mirage.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.smolder.mirage.core.model.SkinData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Optional;

public final class SqliteSkinCache implements SkinCache {
	private static final String SQLITE_DRIVER_CLASS = "org.sqlite.JDBC";

	private final String jdbcUrl;
	private final Cache<String, SkinData> inMemory = Caffeine.newBuilder()
			.maximumSize(8_192)
			.build();

	public SqliteSkinCache(Path databasePath) throws IOException {
		Files.createDirectories(databasePath.toAbsolutePath().getParent());
		this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
		ensureDriverLoaded();
		initSchema();
	}

	@Override
	public Optional<SkinData> get(String tileHash) {
		SkinData cached = inMemory.getIfPresent(tileHash);
		if (cached != null) {
			return Optional.of(cached);
		}

		try (Connection connection = openConnection();
		     PreparedStatement statement = connection.prepareStatement(
				     "SELECT texture_base64, signature FROM mirage_skin_cache WHERE tile_hash = ?")) {
			statement.setString(1, tileHash);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}

				SkinData loaded = new SkinData(
						resultSet.getString("texture_base64"),
						resultSet.getString("signature")
				);
				inMemory.put(tileHash, loaded);
				return Optional.of(loaded);
			}
		} catch (SQLException exception) {
			throw new IllegalStateException("Failed to read from the Mirage skin cache.", exception);
		}
	}

	@Override
	public void put(String tileHash, SkinData skinData) {
		try (Connection connection = openConnection();
		     PreparedStatement statement = connection.prepareStatement("""
				     INSERT INTO mirage_skin_cache(tile_hash, texture_base64, signature, updated_at)
				     VALUES(?, ?, ?, CURRENT_TIMESTAMP)
				     ON CONFLICT(tile_hash) DO UPDATE SET
				       texture_base64 = excluded.texture_base64,
				       signature = excluded.signature,
				       updated_at = CURRENT_TIMESTAMP
				     """)) {
			statement.setString(1, tileHash);
			statement.setString(2, skinData.textureBase64());
			statement.setString(3, skinData.signature());
			statement.executeUpdate();
			inMemory.put(tileHash, skinData);
		} catch (SQLException exception) {
			throw new IllegalStateException("Failed to write to the Mirage skin cache.", exception);
		}
	}

	@Override
	public void close() {
		inMemory.invalidateAll();
	}

	private Connection openConnection() throws SQLException {
		return DriverManager.getConnection(jdbcUrl);
	}

	private static void ensureDriverLoaded() {
		try {
			Class.forName(SQLITE_DRIVER_CLASS);
		} catch (ClassNotFoundException exception) {
			throw new IllegalStateException(
					"SQLite JDBC driver is not available. Ensure sqlite-jdbc is present on the runtime classpath.",
					exception
			);
		}
	}

	private void initSchema() {
		try (Connection connection = openConnection();
		     Statement statement = connection.createStatement()) {
			statement.execute("""
					CREATE TABLE IF NOT EXISTS mirage_skin_cache(
					  tile_hash TEXT PRIMARY KEY,
					  texture_base64 TEXT NOT NULL,
					  signature TEXT NOT NULL,
					  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
					)
					""");
		} catch (SQLException exception) {
			throw new IllegalStateException("Failed to initialize the Mirage skin cache schema.", exception);
		}
	}
}
