package com.github.kuramastone.fightOrFlight.listeners;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.entity.goals.*;
import com.github.kuramastone.fightOrFlight.utils.ReflectionUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PokemonListener {

    private FOFApi api;

    public PokemonListener(FOFApi api) {
        this.api = api;
    }

    public void register() {
        ServerEntityEvents.ENTITY_LOAD.register(this::onEntityLoad);
    }

    /**
     * Modify their ai on load. inserts our custom ai
     */
    private void onEntityLoad(Entity entity, ServerLevel level) {
        if (entity instanceof PokemonEntity pokemonEntity) {
            try {
                api.newWrappedPokemon(pokemonEntity);
                GoalSelector goalSelector = ReflectionUtils.getMobGoalSelector(pokemonEntity);
                goalSelector.addGoal(1, new PokeFleeGoal(api, pokemonEntity));
                goalSelector.addGoal(1, new MeleePokeAttackGoal(api, pokemonEntity));
                goalSelector.addGoal(2, new PokeAttackGoal(api, pokemonEntity));
                goalSelector.addGoal(3, new DefendOwnerGoal(pokemonEntity));
                goalSelector.addGoal(3, new DefendSelfGoal(pokemonEntity));
                //FightOrFlightMod.logger.info("{} has had their ai modified", pokemonEntity.getPokemon().getSpecies().getName());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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

        List<Entity> nearby = level.getEntities(player, aabb, (_e) -> _e instanceof LivingEntity);
        for (Entity entity3 : nearby) {
            AABB entityBounds = entity3.getBoundingBox().inflate(tolerance);
            Optional<Vec3> optional = entityBounds.clip(start, end);
            if (entityBounds.contains(start)) {
                if (tolerance >= 0.0) {
                    entity2 = (LivingEntity) entity3;
                    tolerance = 0.0;
                }
            } else if (optional.isPresent()) {
                entity2 = (LivingEntity) entity3;
            }
        }

        return entity2;
    }

    /**
     * Get pokemon owned by this player nearbny
     */
    private List<PokemonEntity> getNearbyPokemonOwnedBy(Player player) {
        List<PokemonEntity> nearbyPokemon = player.level().getEntitiesOfClass(PokemonEntity.class, player.getBoundingBox().inflate(16, 16, 16));
        nearbyPokemon.removeIf(it -> !player.getUUID().equals(it.getOwnerUUID()));
        return nearbyPokemon;
    }
}
