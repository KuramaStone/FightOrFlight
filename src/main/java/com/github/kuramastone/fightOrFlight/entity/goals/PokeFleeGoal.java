package com.github.kuramastone.fightOrFlight.entity.goals;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.entity.AttackState;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;

import static com.github.kuramastone.fightOrFlight.entity.WrappedPokemon.calculateMovementSpeed;

/**
 * Flee last attacker if they are intimidating
 */
public class PokeFleeGoal extends PanicGoal {

    private final FOFApi api;
    private final WrappedPokemon wrappedPokemon;
    private final PokemonEntity pokemonEntity;

    protected SplittableRandom random = new SplittableRandom();

    public PokeFleeGoal(FOFApi api, PokemonEntity pokemonEntity) {
        super(pokemonEntity, calculateMovementSpeed(pokemonEntity));
        Objects.requireNonNull(pokemonEntity, "Pokemon cannot be null for goal!");
        this.api = api;
        this.pokemonEntity = pokemonEntity;
        this.wrappedPokemon = api.getWrappedPokemon(pokemonEntity);

        getFlags().addAll(List.of(Flag.MOVE, Flag.LOOK, Flag.JUMP, Flag.TARGET));
    }


    @Override
    public void start() {
        super.start();
        if (mob.onGround())
            mob.jumpFromGround();
    }

    @Override
    public void stop() {
        wrappedPokemon.setTarget(null);
        mob.setLastHurtByMob(null);
    }

    @Override
    protected boolean findRandomPosition() {
        Vec3 furthestFound = null;
        double furthestDistSqr = -1;
        for (int i = 0; i < 6; i++) {
            Vec3 vec3 = DefaultRandomPos.getPosAway(wrappedPokemon.getPokemonEntity(), 16, 8, mob.position());
            if (vec3 != null) {
                double distSqr = mob.distanceToSqr(vec3);
                if (distSqr > furthestDistSqr) {
                    furthestFound = vec3;
                    furthestDistSqr = distSqr;
                }
            }
        }

        if (furthestFound != null) {
            this.posX = furthestFound.x;
            this.posY = furthestFound.y;
            this.posZ = furthestFound.z;
        }

        return furthestFound != null;
    }

    @Override
    public boolean shouldPanic() {
        return super.shouldPanic()
                && !api.isDisabledInWorld(wrappedPokemon.getPokemonEntity().level())
                && wrappedPokemon.shouldFlee(pokemonEntity.getLastDamageSource(), pokemonEntity.getLastAttacker());
    }
}
