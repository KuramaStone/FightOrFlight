package com.github.kuramastone.fightOrFlight.listeners;

import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.github.kuramastone.fightOrFlight.utils.Utils.style;

public class WandListener {

    private FOFApi api;

    public WandListener(FOFApi api) {
        this.api = api;
    }

    public void register() {
        UseItemCallback.EVENT.register(this::onItemUsed);
        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
    }

    /**
     * When a player joins, update the wand with the new wand.
     */
    private void onPlayerJoin(ServerGamePacketListenerImpl serverGamePacketListener, PacketSender packetSender, MinecraftServer minecraftServer) {
        ServerPlayer player = serverGamePacketListener.player;

        for (NonNullList<ItemStack> compartment : player.getInventory().compartments) {
            for (int i = 0; i < compartment.size(); i++) {
                ItemStack itemStack = compartment.get(i);
                if (api.getWandManager().isWand(itemStack)) {
                    int amount = itemStack.getCount();
                    ItemStack newWand = api.getWandManager().getWand();
                    newWand.setCount(amount);
                    compartment.set(i, newWand);
                }
            }
        }

    }

    /**
     * Set a player's owned pokemon to target the entity they're looking at
     */
    private InteractionResultHolder<ItemStack> onItemUsed(Player player, Level level, InteractionHand interactionHand) {
        ItemStack inHand = player.getItemInHand(interactionHand);

        if (api.isDisabledInWorld(level)) {
            return InteractionResultHolder.pass(inHand);
        }

        if (api.getWandManager().isWand(inHand)) {
            // update wand
            int amount = inHand.getCount();
            ItemStack newWand = api.getWandManager().getWand();
            newWand.setCount(amount);
            player.setItemInHand(interactionHand, newWand);

            LivingEntity targetEntity = getTargetEntity(player);
            List<PokemonEntity> nearbyPartyMembers = getNearbyPokemonOwnedBy(player);

            if (targetEntity != null) {
                if (!PokeAttack.canAttack(player, targetEntity)) {
                    return InteractionResultHolder.pass(inHand);
                }

                if (targetEntity instanceof PokemonEntity targetPokemonEntity) {
                    // a forced targetting aspect ensures it can always be targeted
                    if (!targetPokemonEntity.getPokemon().getAspects().contains("forced-targetting")  && targetPokemonEntity.getAspects().stream().anyMatch(it -> api.getConfigOptions().protectedAspects.contains(it))) {
                        return InteractionResultHolder.pass(inHand);
                    }
                }

                // dont attack your own pokemon
                boolean success = false;
                if (!nearbyPartyMembers.contains(targetEntity)) {
                    for (PokemonEntity pokemonEntity : nearbyPartyMembers) {
                        if (pokemonEntity != targetEntity) {
                            WrappedPokemon wrappedPokemon = api.getWrappedPokemon(pokemonEntity);
                            wrappedPokemon.setTarget(targetEntity, true);
                            success = true;
                        }
                    }
                }
                if (success) {
                    player.sendSystemMessage(style(api.getConfigOptions().getMessage("Messages.pokewand.targeted")));
                    player.level().playSeededSound(player, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.HOSTILE,
                            1.0f, 1.0f, new Random().nextLong());
                }
            }
            else {
                boolean doAlliesAlreadyHaveTargets = false;
                if (!nearbyPartyMembers.contains(targetEntity)) {
                    for (PokemonEntity pokemonEntity : nearbyPartyMembers) {
                        WrappedPokemon wrappedPokemon = api.getWrappedPokemon(pokemonEntity);
                        if (wrappedPokemon.getTarget() != null)
                            doAlliesAlreadyHaveTargets = true;
                        wrappedPokemon.setTarget(null);
                    }
                }

                // only send detarget message if they had a target
                if (doAlliesAlreadyHaveTargets) {
                    player.level().playSeededSound(player, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.WOOD_HIT, SoundSource.HOSTILE,
                            1.0f, 1.0f, new Random().nextLong());
                    player.sendSystemMessage(style(api.getConfigOptions().getMessage("Messages.pokewand.detargeted")));
                }
            }

        }

        return InteractionResultHolder.pass(inHand);
    }


    /**
     * Get the entity this player is looking at
     */
    private LivingEntity getTargetEntity(Player player) {

        double maxDistance = 24.0;

        AABB aabb = player.getBoundingBox().inflate(maxDistance, maxDistance, maxDistance);
        Vec3 start = player.getEyePosition().add(player.getLookAngle());
        Vec3 end = player.getEyePosition().add(player.getLookAngle().multiply(maxDistance, maxDistance, maxDistance));
        Level level = player.level();
        double tolerance = 0.3;
        LivingEntity entity2 = null;

        List<Entity> nearby = level.getEntities(player, aabb, (it) -> it instanceof LivingEntity);

        // remove protected entities
        nearby.removeIf(it -> {
            if (it instanceof PokemonEntity pokemonEntity) {
                if (FightOrFlightMod.instance.getAPI().isPokemonProtected(pokemonEntity)) {
                    return true;
                }
                // if owned pokemon cant be targetted and this pokemon has an owner, then dont include it
                if(!pokemonEntity.getPokemon().getAspects().contains("fof-allowed") && ((FightOrFlightMod.instance.getAPI().getConfigOptions().ownedPokemonCannotBeTargetted) &&
                        (pokemonEntity.getOwner() != null || pokemonEntity.getTethering() != null))) {
                    return true;
                }
            }
            return false;
        });

        if(!api.getConfigOptions().allowPvP) {
            nearby.removeIf(it -> it instanceof ServerPlayer);
        }

        for (Entity entity3 : nearby) {
            if (entity3 != player) {
                AABB entityBounds = entity3.getBoundingBox().inflate(tolerance);

                // if it is a pokemonEntity, change the bounds to its entity dimension
                if(entity3 instanceof PokemonEntity pokemonEntity) {
                    EntityDimensions dims = pokemonEntity.getDimensions(pokemonEntity.getPose());
                    entityBounds = dims.makeBoundingBox(pokemonEntity.position());
                }

                double dist = 0.0;
                Vec3 direction = end.subtract(start).normalize();
                while (dist < maxDistance) {
                    Vec3 positionOnLine = start.add(direction.multiply(dist, dist, dist));
                    if(entityBounds.contains(positionOnLine)) {
                        entity2 = (LivingEntity) entity3;
                        break;
                    }
                    dist += tolerance;
                }
            }
        }

        return entity2;
    }

    /**
     * Get pokemon owned by this player nearbny
     */
    private List<PokemonEntity> getNearbyPokemonOwnedBy(Player player) {
        List<PokemonEntity> nearbyPokemon = player.level().getEntitiesOfClass(PokemonEntity.class, player.getBoundingBox().inflate(32, 32, 32));
        nearbyPokemon.removeIf(it -> !player.getUUID().equals(it.getOwnerUUID()));
        return nearbyPokemon;
    }

}
