package com.github.kuramastone.fightOrFlight.commands;

import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.utils.PermissionUtils;
import com.mojang.brigadier.Command;
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
            dispatcher.register(Commands.literal("fightorflight")
                            .requires(it -> hasAdminPermission(it))
                    .then(Commands.literal("reload")
                            .executes(this::reload)));
            
        });
    }

    private boolean hasAdminPermission(CommandSourceStack it) {
        if(it.hasPermission(2)) {
            return true;
        }

        if(!it.isPlayer()) {
            return true;
        }

        return PermissionUtils.hasPermission(it.getPlayer(), "FightOrFlight.reload");
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        api.getConfigOptions().load();
        context.getSource().sendSystemMessage(style("<green>Reloading FightOrFlight configs!"));
        return Command.SINGLE_SUCCESS;
    }

    private int pokewand(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        player.addItem(api.getWandManager().getWand());
        player.sendSystemMessage(style(api.getConfigOptions().getMessage("Messages.pokewand.received")));
        return 1;
    }

}
