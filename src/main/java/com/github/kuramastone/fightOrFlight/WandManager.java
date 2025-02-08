package com.github.kuramastone.fightOrFlight;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import javax.xml.crypto.Data;

import static com.github.kuramastone.fightOrFlight.utils.Utils.style;

public class WandManager {

    public WandManager() {
        buildWand();
    }

    private void buildWand() {
        CompoundTag customData = new CompoundTag();
        customData.putBoolean("is_wand", true);
    }

    public ItemStack getWand() {
        return FightOrFlightMod.instance.getAPI().getConfigOptions().pokeWand.copy();
    }

    public boolean isWand(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty())
            return false;

        if (itemStack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = itemStack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("is_wand")) {
                return true;
            }
        }

        return false;
    }
}
