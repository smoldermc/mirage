package fr.smolder.mirage.minestom;

import fr.smolder.mirage.core.model.MotdRender;
import fr.smolder.mirage.core.model.RenderedSkin;
import fr.smolder.mirage.core.port.MotdController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.object.ObjectContents;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.ping.Status;

import java.util.List;
import java.util.UUID;

public final class MinestomMotdController implements MotdController {
    private static final String DEFAULT_MOTD_KEY = "default";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final UUID EMPTY_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final int DEFAULT_TEXT_ARGB = 0xFFFFFFFF;
    private static final int DEFAULT_SHADOW_ARGB = 0xFFFFFFFF;

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
        if (render.state() == MotdRender.RenderState.READY && !render.orderedSkins().isEmpty()) {
            TextColor textColor = parseTextColor(render.textColor());
            AlphaColor shadowColor = parseShadowColor(render.shadowColor());
            Component result = Component.empty();
            for (int index = 0; index < render.orderedSkins().size(); index++) {
                result = result.append(toPlayerHead(render.orderedSkins().get(index)));
                if ((index + 1) % render.columns() == 0 && index + 1 < render.orderedSkins().size()) {
                    result = result.append(Component.text("\n"));
                }
            }
            return result
                    .color(textColor)
                    .shadowColor(shadowColor);
        }
        return MINI_MESSAGE.deserialize(render.fallbackText());
    }

    private Component toPlayerHead(RenderedSkin renderedSkin) {
        var skinData = renderedSkin.skinData();
        GameProfile.Property property = new GameProfile.Property(
                "textures",
                skinData.textureBase64(),
                skinData.signature()
        );
        ResolvableProfile profile = new ResolvableProfile(new ResolvableProfile.Partial(
                null,
                EMPTY_PROFILE_ID,
                List.of(property)
        ));
        return Component.object(ObjectContents.playerHead()
                .id(EMPTY_PROFILE_ID)
                .profileProperty(property)
                .skin(profile)
                .hat(renderedSkin.hat())
                .build());
    }

    private static TextColor parseTextColor(String configuredColor) {
        int argb = parseArgb(configuredColor, DEFAULT_TEXT_ARGB);
        return TextColor.color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF);
    }

    private static AlphaColor parseShadowColor(String configuredColor) {
        int argb = parseArgb(configuredColor, DEFAULT_SHADOW_ARGB);
        return new AlphaColor(
                (argb >>> 16) & 0xFF,
                (argb >>> 8) & 0xFF,
                argb & 0xFF,
                (argb >>> 24) & 0xFF
        );
    }

    private static int parseArgb(String configuredColor, int fallbackArgb) {
        if (configuredColor == null) {
            return fallbackArgb;
        }

        String hex = configuredColor.startsWith("#") ? configuredColor.substring(1) : configuredColor;
        try {
            if (hex.length() == 6) {
                return 0xFF000000 | Integer.parseUnsignedInt(hex, 16);
            }
            if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException ignored) {
            return fallbackArgb;
        }
        return fallbackArgb;
    }

}
