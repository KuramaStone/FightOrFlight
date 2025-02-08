package com.github.kuramastone.fightOrFlight.utils;

import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.NotImplementedException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static MutableComponent style(Object object) {
        return adapt(MiniMessage.miniMessage().deserialize("<bold:false><italic:false>%s".formatted(object.toString())));
    }

    public static List<Component> style(Object... objects) {
        List<Component> components = new ArrayList<>();
        for (Object object : objects) {
            components.add(style(object));
        }
        return components;
    }

    public static MutableComponent adapt(net.kyori.adventure.text.Component component) {
        if (component instanceof TextComponent) {
            // Handle basic text component
            String content = ((TextComponent) component).content();
            MutableComponent textComponent = net.minecraft.network.chat.Component.literal(content);

            // Apply styles from Kyori component
            applyStyle(component, textComponent);

            for (net.kyori.adventure.text.Component child : component.children()) {
                textComponent.append(adapt(child));
            }

            return textComponent;
        }


        throw new NotImplementedException("Cannot adapt this type of component yet.");
    }

    private static void applyStyle(net.kyori.adventure.text.Component source, MutableComponent target) {
        target.setStyle(target.getStyle().withColor(Color.WHITE.getRGB()));
        target.setStyle(target.getStyle().withBold(false));
        target.setStyle(target.getStyle().withItalic(false));
        target.setStyle(target.getStyle().withUnderlined(false));
        target.setStyle(target.getStyle().withStrikethrough(false));
        target.setStyle(target.getStyle().withObfuscated(false));

        if (source.color() != null) {
            TextColor color = source.color();
            if (color != null) {
                target.setStyle(target.getStyle().withColor(net.minecraft.network.chat.TextColor.fromRgb(color.value())));
            }
        }

        if (source.hasDecoration(TextDecoration.BOLD)) {
            target.setStyle(target.getStyle().withBold(true));
        }
        if (source.hasDecoration(TextDecoration.ITALIC)) {
            target.setStyle(target.getStyle().withItalic(true));
        }
        if (source.hasDecoration(TextDecoration.UNDERLINED)) {
            target.setStyle(target.getStyle().withUnderlined(true));
        }
        if (source.hasDecoration(TextDecoration.STRIKETHROUGH)) {
            target.setStyle(target.getStyle().withStrikethrough(true));
        }
        if (source.hasDecoration(TextDecoration.OBFUSCATED)) {
            target.setStyle(target.getStyle().withObfuscated(true));
        }
    }

    public static void broadcast(String message) {
        for (ServerPlayer player : FightOrFlightMod.getMinecraftServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(style(message));
        }
    }
}
