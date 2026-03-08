package fr.smolder.mirage.spigot;

import fr.smolder.mirage.core.model.MotdRender;
import fr.smolder.mirage.core.port.MotdController;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class SpigotMotdListener implements Listener, MotdController {
    private static final String DEFAULT_MOTD_KEY = "default";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();

    private MotdResolver resolver = (key, protocolVersion) -> MotdRender.loading("Loading...", java.util.List.of());

    @Override
    public void install(MotdResolver resolver) {
        this.resolver = resolver;
    }

    @EventHandler
    public void onServerListPing(PaperServerListPingEvent event) {
        MotdRender render = resolver.resolve(DEFAULT_MOTD_KEY, event.getProtocolVersion());
        event.motd(toComponent(render));
    }

    private Component toComponent(MotdRender render) {
        if (!render.modernJson().isBlank()) {
            try {
                return GSON_SERIALIZER.deserialize(render.modernJson());
            } catch (Exception ignored) {
                // Fall through to the configured fallback text when the component JSON is invalid.
            }
        }
        return MINI_MESSAGE.deserialize(render.fallbackText());
    }
}
