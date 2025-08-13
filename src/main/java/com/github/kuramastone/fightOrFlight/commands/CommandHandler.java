package com.github.kuramastone.fightOrFlight.commands;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.event.FOFEvents;
import com.github.kuramastone.fightOrFlight.event.WandReceiveEvent;
import com.github.kuramastone.fightOrFlight.utils.ExpiryList;
import com.github.kuramastone.fightOrFlight.utils.PermissionUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import kotlinx.datetime.serializers.TimeBasedDateTimeUnitSerializer;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.kuramastone.fightOrFlight.utils.Utils.style;

public class CommandHandler {

    private FOFApi api;

    public CommandHandler(FOFApi api) {
        this.api = api;
    }

    public void register() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("pokewand")
                            .requires(it -> it.hasPermission(2) || (it.isPlayer()  && Permissions.check(it.getPlayer(), "fightorflight.commands.pokewand")))
                    .executes(this::pokewand)
            );
            dispatcher.register(Commands.literal("fightorflight")
                    .requires(it -> hasAdminPermission(it))
                    .then(Commands.literal("reload")
                            .executes(this::reload))
                    .then(Commands.literal("scale")
                            .executes(this::scale)));

        });
    }

    private int scale(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        Entity target = getEntityLookedAt(player, 16.0);
        if (target instanceof PokemonEntity pokemonEntity) {
            pokemonEntity.getPokemon().setScaleModifier(15.0F);
        }

        return 1;
    }

    public static Entity getEntityLookedAt(Player player, double distance) {
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getLookAngle();
        Vec3 reachVec = eyePosition.add(lookVector.scale(distance));
        AABB aabb = player.getBoundingBox().expandTowards(lookVector.scale(distance)).inflate(1.0D);

        Level level = player.level();

        List<Entity> entities = level.getEntities(player, aabb, entity -> !entity.isSpectator() && entity.isPickable());
        Entity closest = null;
        double closestDistance = distance * distance;

        for (Entity entity : entities) {
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Vec3 result = entityBox.clip(eyePosition, reachVec).orElse(null);
            if (result != null) {
                double distSq = eyePosition.distanceToSqr(result);
                if (distSq < closestDistance) {
                    closest = entity;
                    closestDistance = distSq;
                }
            }
        }

        return closest;
    }

    private boolean hasAdminPermission(CommandSourceStack it) {
        if (it.hasPermission(2)) {
            return true;
        }

        if (!it.isPlayer()) {
            return true;
        }

        return PermissionUtils.hasPermission(it.getPlayer(), "FightOrFlight.reload");
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        api.getConfigOptions().load();
        context.getSource().sendSystemMessage(style("<green>Reloading FightOrFlight configs!"));
        return Command.SINGLE_SUCCESS;
    }

    public static final ExpiryList<UUID> wandTimer = new ExpiryList<>();

    private int pokewand(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();

        if (wandTimer.contains(player.getUUID())) {
            player.sendSystemMessage(style(api.getConfigOptions().getMessage("Messages.pokewand.cooldown")
                    .replace("{duration}", wandTimer.getDurationFor(player.getUUID()))
            ));
            return 0;
        }

        WandReceiveEvent event = new WandReceiveEvent(player, Duration.of(api.getConfigOptions().commandDelayInSeconds, ChronoUnit.SECONDS));
        FOFEvents.WAND_COMMAND_COOLDOWN.emit(event);
        player.addItem(api.getWandManager().getWand());
        if (!event.getCooldownLength().isZero())
            wandTimer.addExpiration(player.getUUID(), event.getCooldownLength());
        player.sendSystemMessage(style(api.getConfigOptions().getMessage("Messages.pokewand.received")));
        return 1;
    }

}
