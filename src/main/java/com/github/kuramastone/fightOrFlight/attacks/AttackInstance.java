package com.github.kuramastone.fightOrFlight.attacks;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket;
import com.github.kuramastone.fightOrFlight.utils.ForgeTask;
import com.github.kuramastone.fightOrFlight.utils.TickScheduler;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class AttackInstance implements Runnable {

    protected int uid = (int) (Math.random() * 2000000000 - 1000000000);

    protected final PokeAttack pokeAttack;
    protected final PokemonEntity pokemonEntity;
    protected final LivingEntity target;
    protected final CompletableFuture<Boolean> future;
    protected final boolean isSpecial;

    protected ForgeTask forgeTask;
    protected SplittableRandom random = new SplittableRandom();
    protected int currentTick = 0;
    protected int maxTicks = 30;

    public AttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int maxTime) {
        this.pokeAttack = pokeAttack;
        this.pokemonEntity = pokemonEntity;
        this.target = target;
        this.future = future;
        this.isSpecial = isSpecial;
        this.maxTicks = maxTime;
    }

    public void schedule() {
        forgeTask = TickScheduler.scheduleRepeating(this, 0L, 1L);
    }

    @Override
    public void run() {
        if (currentTick == 0)
            start();

        PokemonEntity pokeTarget = target instanceof PokemonEntity ? (PokemonEntity) target : null;

        if (!pokemonEntity.isDeadOrDying() && !pokemonEntity.isBusy() && !pokemonEntity.isBattling()
                && (pokeTarget == null || (!pokeTarget.isBattling() && !pokeTarget.isBusy())))
            tick();

        currentTick++;
        if (currentTick == maxTicks) {
            end();
            future.complete(true);
            forgeTask.isCancelled = true;
        }

    }

    protected void end() {
        List<MoveTemplate> moveTemplates = pokemonEntity.getPokemon().getMoveSet().getMoves().stream().map(it -> it.getTemplate()).toList();
        boolean hasTeleport = moveTemplates.contains(Moves.INSTANCE.getByName("teleport"));

        if (hasTeleport) {
            pokemonEntity.randomTeleport(8, 4, 8, true);
        }
    }

    protected void start() {
        Move bestMove = PokeAttack.getHighestDamagingMoveOfType(pokemonEntity, pokeAttack.getElementalType());

        Set<String> animations = new HashSet<>();
        animations.add(isSpecial ? "special" : "physical");
        animations.add(pokeAttack.getElementalType().getName().toLowerCase());
        if(bestMove != null) {
            animations.add(bestMove.getName().toLowerCase());
        }

        PlayPosableAnimationPacket pkt = new PlayPosableAnimationPacket(pokemonEntity.getId(),
                animations,
                Collections.emptyList());
        pkt.sendToPlayersAround(
                pokemonEntity.getX(), pokemonEntity.getY(), pokemonEntity.getZ(),
                32, pokemonEntity.level().dimension(), (p)->false
        );
    }

    // interpolate between two vectors
    protected Vec3 interpolate(Vec3 a, Vec3 b, float f) {
        Vec3 c = b.subtract(a);
        return a.add(c.multiply(f, f, f));
    }

    protected boolean hasPassedSectionsOf(int maxAttacks) {
        if (maxAttacks == 1)
            return currentTick == maxTicks - 1;
        double lastAttackIndex = (double) (currentTick - 1) / maxTicks * maxAttacks;
        double currentAttackIndex = (double) currentTick / maxTicks * maxAttacks;
        if (Math.signum(lastAttackIndex) != Math.signum(currentAttackIndex) || (int) (lastAttackIndex) != (int) currentAttackIndex) {
            return true;
        }

        return false;
    }

    /**
     * Spawns a particle inside the aabb
     */
    protected void spawnRandomlyInsideAABB(Supplier<ParticleOptions> particles, AABB aabb) {
        spawnRandomlyInsideAABB(particles, aabb, 1, 0, 0, 0, 0);
    }

    /**
     * Spawns a particle inside the aabb
     */
    protected void spawnRandomlyInsideAABB(Supplier<ParticleOptions> particleOptions, AABB aabb, int count, float dx, float dy, float dz, float speed) {
        Vec3 pos = aabb.getMinPosition().add(
                random.nextDouble() * aabb.getXsize(),
                random.nextDouble() * aabb.getYsize(),
                random.nextDouble() * aabb.getZsize()
        );

        spawnParticleAt(
                particleOptions.get(),
                pos,
                count,
                dx, dy, dz,
                speed
        );
    }

    /**
     * Spawns a spiral line of particles slowly based on the currentTick/maxTicks.
     */
    protected void spawnGradualSpiralParticles(Vec3 start, Vec3 end, Supplier<ParticleOptions> particles, int particlesPerTick, int spiralCount, float spiralRadius, float spiralAngleStart) {

        Vec3 direction = end.subtract(start).normalize();
        float distanceBetweenTwoPoints = (float) start.distanceTo(end);

        for (int i = 0; i < particlesPerTick; i++) {
            float progressToTarget = (float) (currentTick * particlesPerTick + i) / (maxTicks * particlesPerTick);
            // spiral angle around beam center
            float radians = spiralAngleStart + (float) (2 * Math.PI * spiralCount * progressToTarget);

            // Build spiral on a straight line with no angle
            Vec3 initialPosition = new Vec3(distanceBetweenTwoPoints * progressToTarget,
                    Math.cos(radians) * spiralRadius, Math.sin(radians) * spiralRadius);

            // Find the angle between the two vectors
            double xzAngle = Math.atan2(-direction.z, direction.x);

            // Apply the xz rotation to the initial position
            Vec3 rotatedPosition = rotateAroundAxis(initialPosition, new Vec3(0, 1, 0), xzAngle);
            // apply y rotation
            rotatedPosition = rotatedPosition.add(0, (end.y - start.y) * progressToTarget, 0);

            Vec3 finalPosition = pokemonEntity.getEyePosition().add(rotatedPosition);

            // spawn particle at position
            spawnParticleAt(particles.get(),
                    finalPosition, 1, 0, 0, 0, 0);
        }

    }

    /**
     * Spawns a line of particles slowly based on the currentTick/maxTicks.
     */
    protected void spawnGradualLineOfParticles(Vec3 start, Vec3 end, Supplier<ParticleOptions> particles, int particlesPerTick,
                                               int count, float dx, float dy, float dz, float speed) {

        Vec3 direction = end.subtract(start).normalize();
        float distanceBetweenTwoPoints = (float) start.distanceTo(end);

        for (int i = 0; i < particlesPerTick; i++) {
            float progressToTarget = (float) (currentTick * particlesPerTick + i) / (maxTicks * particlesPerTick);
            // spiral angle around beam center
            // Build spiral on a straight line with no angle
            Vec3 initialPosition = new Vec3(distanceBetweenTwoPoints * progressToTarget, 0, 0);

            // Find the angle between the two vectors
            double xzAngle = Math.atan2(-direction.z, direction.x);

            // Apply the xz rotation to the initial position
            Vec3 rotatedPosition = rotateAroundAxis(initialPosition, new Vec3(0, 1, 0), xzAngle);
            // apply y rotation
            rotatedPosition = rotatedPosition.add(0, (end.y - start.y) * progressToTarget, 0);

            Vec3 finalPosition = pokemonEntity.getEyePosition().add(rotatedPosition);

            // spawn particle at position
            spawnParticleAt(particles.get(),
                    finalPosition, count, dx, dy, dz, speed);
        }

    }

    /**
     * Spawns a line of particles slowly based on the currentTick/maxTicks.
     */
    protected void spawnGradualLineOfParticles(Vec3 start, Vec3 end, Supplier<ParticleOptions> particles, int particlesPerTick) {
        spawnGradualLineOfParticles(start, end, particles, particlesPerTick, 1, 0, 0, 0, 1);
    }

    protected void spawnImmediateLineOfParticles(Vec3 start, Vec3 end, Supplier<ParticleOptions> particles, int particlesPerBlock,
                                                 int count, float dx, float dy, float dz, float speed) {

        Vec3 direction = end.subtract(start).normalize();
        float distanceBetweenTwoPoints = (float) start.distanceTo(end);

        int totalParticles = (int) Math.ceil(particlesPerBlock * distanceBetweenTwoPoints);
        for (int i = 0; i < totalParticles; i++) {
            float progressToTarget = (float) i / totalParticles;
            // spiral angle around beam center
            // Build spiral on a straight line with no angle
            Vec3 initialPosition = new Vec3(distanceBetweenTwoPoints * progressToTarget, 0, 0);

            // Find the angle between the two vectors
            double xzAngle = Math.atan2(-direction.z, direction.x);

            // Apply the xz rotation to the initial position
            Vec3 rotatedPosition = rotateAroundAxis(initialPosition, new Vec3(0, 1, 0), xzAngle);
            // apply y rotation
            rotatedPosition = rotatedPosition.add(0, (end.y - start.y) * progressToTarget, 0);

            Vec3 finalPosition = pokemonEntity.getEyePosition().add(rotatedPosition);

            // spawn particle at position
            spawnParticleAt(particles.get(),
                    finalPosition, count, dx, dy, dz, speed);
        }

    }

    // Rotates a vector (vec) around an axis by an angle (in radians)
    protected Vec3 rotateAroundAxis(Vec3 vec, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dot = vec.dot(axis);

        return vec.scale(cos)
                .add(axis.cross(vec).scale(sin))
                .add(axis.scale(dot * (1 - cos)));
    }

    protected ParticleOptions loadParticle(Level level, String particleString) {
        try {
            RegistryAccess registryAccess = level.registryAccess();
            return ParticleArgument.readParticle(new StringReader(particleString), registryAccess);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected void spawnParticleAt(ParticleOptions particleOptions, Vec3 vec, int count, double dx, double dy, double dz, double speed) {
        ((ServerLevel) target.level()).sendParticles(particleOptions, vec.x, vec.y, vec.z, count, dx, dy, dz, speed);
    }

    protected abstract void tick();

}
