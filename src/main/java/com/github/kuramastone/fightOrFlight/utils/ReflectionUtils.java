package com.github.kuramastone.fightOrFlight.utils;

import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionUtils {

    private static Field invulnerableTimeField;
    private static Field goalSelectorField;
    private static Field fallingBlockEntityBlockStateField;
    private static Field fallingBlockCancelDropField;
    private static Field meleeAttackGoalPathField;
    private static Method displayBlockMethod_setBlockState;
    private static Method livingEntityMethod_getDamageAfterArmorAbsorb;
    private static Method livingEntityMethod_getDamageAfterMagicAbsorb;

    public static void register() {
        try {
            // dev environment
            invulnerableTimeField = Entity.class.getDeclaredField("invulnerableTime");
            invulnerableTimeField.setAccessible(true);

            goalSelectorField = Mob.class.getDeclaredField("goalSelector");
            goalSelectorField.setAccessible(true);

            fallingBlockEntityBlockStateField = FallingBlockEntity.class.getDeclaredField("blockState");
            fallingBlockEntityBlockStateField.setAccessible(true);

            fallingBlockCancelDropField = FallingBlockEntity.class.getDeclaredField("cancelDrop");
            fallingBlockCancelDropField.setAccessible(true);

            meleeAttackGoalPathField = MeleeAttackGoal.class.getDeclaredField("path");
            meleeAttackGoalPathField.setAccessible(true);

            displayBlockMethod_setBlockState = Display.BlockDisplay.class.getDeclaredMethod("setBlockState", BlockState.class);
            displayBlockMethod_setBlockState.setAccessible(true);

            livingEntityMethod_getDamageAfterArmorAbsorb = LivingEntity.class.getDeclaredMethod("getDamageAfterArmorAbsorb", DamageSource.class, float.class);
            livingEntityMethod_getDamageAfterArmorAbsorb.setAccessible(true);
            livingEntityMethod_getDamageAfterMagicAbsorb = LivingEntity.class.getDeclaredMethod("getDamageAfterMagicAbsorb", DamageSource.class, float.class);
            livingEntityMethod_getDamageAfterMagicAbsorb.setAccessible(true);

        } catch (NoSuchFieldException | NoSuchMethodException e) {
            try {
                // live environment
                invulnerableTimeField = Entity.class.getDeclaredField("field_6008");
                invulnerableTimeField.setAccessible(true);

                goalSelectorField = Mob.class.getDeclaredField("field_6201");
                goalSelectorField.setAccessible(true);

                fallingBlockEntityBlockStateField = FallingBlockEntity.class.getDeclaredField("field_7188");
                fallingBlockEntityBlockStateField.setAccessible(true);

                fallingBlockCancelDropField = FallingBlockEntity.class.getDeclaredField("field_7189");
                fallingBlockCancelDropField.setAccessible(true);

                meleeAttackGoalPathField = MeleeAttackGoal.class.getDeclaredField("field_6509");
                meleeAttackGoalPathField.setAccessible(true);

                displayBlockMethod_setBlockState = Display.BlockDisplay.class.getDeclaredMethod("method_48883", BlockState.class);
                displayBlockMethod_setBlockState.setAccessible(true);

                livingEntityMethod_getDamageAfterArmorAbsorb = LivingEntity.class.getDeclaredMethod("method_6132", DamageSource.class, float.class);
                livingEntityMethod_getDamageAfterArmorAbsorb.setAccessible(true);
                livingEntityMethod_getDamageAfterMagicAbsorb = LivingEntity.class.getDeclaredMethod("method_6036", DamageSource.class, float.class);
                livingEntityMethod_getDamageAfterMagicAbsorb.setAccessible(true);

            } catch (NoSuchFieldException e2) {
                FightOrFlightMod.logger.error("Entity: {}", Arrays.toString(Entity.class.getDeclaredFields()));
                FightOrFlightMod.logger.error("Mob: {}", Arrays.toString(Mob.class.getDeclaredFields()));
                FightOrFlightMod.logger.error("FallingBlockEntity: {}", Arrays.toString(FallingBlockEntity.class.getDeclaredFields()));
                FightOrFlightMod.logger.error("MeleeAttackGoal: {}", Arrays.toString(MeleeAttackGoal.class.getDeclaredFields()));
                throw new RuntimeException(e2);
            } catch (NoSuchMethodException ex) {
                FightOrFlightMod.logger.error("Entity: {}", Arrays.toString(Entity.class.getDeclaredMethods()));
                FightOrFlightMod.logger.error("Mob: {}", Arrays.toString(Mob.class.getDeclaredMethods()));
                FightOrFlightMod.logger.error("FallingBlockEntity: {}", Arrays.toString(FallingBlockEntity.class.getDeclaredMethods()));
                FightOrFlightMod.logger.error("MeleeAttackGoal: {}", Arrays.toString(MeleeAttackGoal.class.getDeclaredMethods()));
                throw new RuntimeException(ex);
            }

        }
    }

    public static void setFallingBlockEntityBlockState(FallingBlockEntity fallingBlock, BlockState blockState) throws IllegalAccessException {
        fallingBlockEntityBlockStateField.set(fallingBlock, blockState);
    }

    public static void setFallingBlockEntityCancelDrop(FallingBlockEntity fallingBlock, boolean cancelDrop) throws IllegalAccessException {
        fallingBlockCancelDropField.set(fallingBlock, cancelDrop);
    }

    public static void setEntityInvulnerableTime(Entity entity, int invulnerableTime) throws IllegalAccessException {
        invulnerableTimeField.set(entity, invulnerableTime);
    }

    public static GoalSelector getMobGoalSelector(Mob mob) throws IllegalAccessException {
        return (GoalSelector) goalSelectorField.get(mob);
    }

    public static Field getMeleeAttackGoalPathField() {
        return meleeAttackGoalPathField;
    }

    public static void displayBlockMethod_setBlockState(Display.BlockDisplay display, BlockState blockState) throws InvocationTargetException, IllegalAccessException {
        displayBlockMethod_setBlockState.invoke(display, blockState);
    }

    public static float livingEntityMethod_getDamageAfterArmorAbsorb(LivingEntity entity, DamageSource damageSource, float f) throws InvocationTargetException, IllegalAccessException {
        return (float) livingEntityMethod_getDamageAfterArmorAbsorb.invoke(entity, damageSource, f);
    }

    public static float livingEntityMethod_getDamageAfterMagicAbsorb(LivingEntity entity, DamageSource damageSource, float f) throws InvocationTargetException, IllegalAccessException {
        return (float) livingEntityMethod_getDamageAfterMagicAbsorb.invoke(entity, damageSource, f);
    }

}
