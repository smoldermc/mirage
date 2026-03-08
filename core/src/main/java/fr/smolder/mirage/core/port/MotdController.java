package fr.smolder.mirage.core.port;

import fr.smolder.mirage.core.model.MotdRender;

public interface MotdController {
    void install(MotdResolver resolver);

    @FunctionalInterface
    interface MotdResolver {
        MotdRender resolve(String key, int protocolVersion);
    }
}
