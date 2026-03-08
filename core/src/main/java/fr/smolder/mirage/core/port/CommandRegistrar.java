package fr.smolder.mirage.core.port;

public interface CommandRegistrar {
    void registerReloadCommand(Runnable reloadAction);
}
