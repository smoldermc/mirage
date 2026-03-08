package fr.smolder.mirage.minestom;

import fr.smolder.mirage.core.model.MotdRender;
import fr.smolder.mirage.core.port.MotdController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.ping.Status;

public final class MinestomMotdController implements MotdController {
    private static final String DEFAULT_MOTD_KEY = "default";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();

    @Override
    public void install(MotdResolver resolver) {
        MinecraftServer.getGlobalEventHandler().addListener(ServerListPingEvent.class, event -> {
            int protocolVersion = event.getConnection() != null ? event.getConnection().getProtocolVersion() : -1;
            MotdRender render = resolver.resolve(DEFAULT_MOTD_KEY, protocolVersion);
            Status updatedStatus = Status.builder(event.getStatus())
                    .description(toComponent(render))
                    .build();
            event.setStatus(updatedStatus);
        });
    }

    private Component toComponent(MotdRender render) {
        if (!render.modernJson().isBlank()) {
            try {
                return GSON_SERIALIZER.deserialize(render.modernJson());
            } catch (Exception ignored) {
                // Fall through to the fallback text when the component JSON is invalid.
            }
        }
        return MINI_MESSAGE.deserialize(render.fallbackText());
    }
}
