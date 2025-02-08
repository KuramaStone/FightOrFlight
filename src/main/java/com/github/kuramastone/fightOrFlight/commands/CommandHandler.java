package com.github.kuramastone.fightOrFlight.commands;

import com.github.kuramastone.fightOrFlight.FOFApi;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import static com.github.kuramastone.fightOrFlight.utils.Utils.style;

public class CommandHandler {

    private FOFApi api;

    public CommandHandler(FOFApi api) {
        this.api = api;
    }

    public void register() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("pokewand")
                    .executes(this::pokewand)
            );
        });
    }

    private int pokewand(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        player.addItem(api.getWandManager().getWand());
        player.sendSystemMessage(style(api.getConfigOptions().getMessage("Messages.pokewand.received")));
        return 1;
    }

}
