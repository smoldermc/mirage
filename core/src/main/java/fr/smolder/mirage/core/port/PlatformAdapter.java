package fr.smolder.mirage.core.port;

import org.slf4j.Logger;

import java.nio.file.Path;

public interface PlatformAdapter {
	Logger logger();

	MirageScheduler scheduler();

	Path dataDirectory();
}
