package com.github.kuramastone.fightOrFlight.utils;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.kuramastone.fightOrFlight.utils.Utils.style;

public class ConfigOptions {

    public Map<EntityType<?>, Pokemon> mobPokemonEquivalents;
    public Pokemon defaultMobPokemonEquivalent;
    public int baseAggressionTimer;
    public boolean disableRewardsOutsideBattle;
    public boolean ownedPokemonAggressionDisabled;

    public ItemStack pokeWand;

    private YamlDocument document;

    public ConfigOptions() {
        load();
    }

    public void load() {
        mobPokemonEquivalents = new HashMap<>();

        try {
            File diskFile = new File(new File(FabricLoader.getInstance().getConfigDir().toFile(), FightOrFlightMod.MODID), "config.yml");
            YamlDocument config = YamlDocument.create(diskFile, getClass().getResourceAsStream("/fightorflight.config.yml"),
                    GeneralSettings.builder().setUseDefaults(false).build(),
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    UpdaterSettings.builder()
                            .addIgnoredRoute("2", Route.from("entity-to-pokemon")) // dont auto-update this section

                            .addIgnoredRoute("3", Route.from("entity-to-pokemon")) // dont auto-update this section
                            .build());
            document = config;
            loadEntityTypes(config);
            loadPokeWand(config);

            baseAggressionTimer = config.getInt("standard-aggression-timer-ticks", 3600);
            disableRewardsOutsideBattle = config.getBoolean("disable-rewards-outside-battle", false);
            ownedPokemonAggressionDisabled = config.getBoolean("owned-pokemon-aggression-disabled", false);


        } catch (Exception e) {
            throw new RuntimeException("Error loading config.yml.", e);
        }
    }

    public String getMessage(String path) {
        return document.contains(path) ? document.getString(path) : path;
    }

    private void loadPokeWand(YamlDocument config) {
        Item item = loadItem(config.getString("pokewand.item"));
        int customModelData = config.getInt("pokewand.customModelData");
        String name = config.getString("pokewand.name");
        List<String> lore = config.getStringList("pokewand.lore");
        pokeWand = new ItemStack(item);

        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putBoolean("is_wand", true);

        pokeWand.set(DataComponents.CUSTOM_NAME, style(name));
        pokeWand.set(DataComponents.LORE, new ItemLore(style(lore.toArray())));
        pokeWand.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customModelData));
        CustomData.set(DataComponents.CUSTOM_DATA, pokeWand, compoundTag);
    }

    private void loadEntityTypes(YamlDocument config) {
        defaultMobPokemonEquivalent = PokemonProperties.Companion.parse(
                config.getString("entity-to-pokemon.default.pokemon")).create();
        for (Object entityKeyObj : config.getSection("entity-to-pokemon").getKeys()) {
            String entityKey = entityKeyObj.toString();
            Pokemon pokemon = PokemonProperties.Companion.parse(
                    config.getString("entity-to-pokemon.%s.pokemon".formatted(entityKey))).create();

            EntityType<?> entityType = loadEntityType(entityKey);
            mobPokemonEquivalents.put(entityType, pokemon);
        }
    }

    private EntityType<?> loadEntityType(String entityKey) {
        ResourceLocation resourceLocation = ResourceLocation.parse(entityKey);
        Optional<Registry<EntityType<?>>> opt = FightOrFlightMod.getMinecraftServer().overworld().registryAccess().registry(Registries.ENTITY_TYPE);
        if(opt.isEmpty())
            throw new RuntimeException("Could not find registry for entity types! Contact the developer.");
        return opt.get().get(resourceLocation);
    }

    private Item loadItem(String itemKey) {
        ResourceLocation resourceLocation = ResourceLocation.parse(itemKey);
        Optional<Registry<Item>> opt = FightOrFlightMod.getMinecraftServer().overworld().registryAccess().registry(Registries.ITEM);
        if(opt.isEmpty())
            throw new RuntimeException("Could not find registry for entity types! Contact the developer.");
        return opt.get().get(resourceLocation);
    }


    public Pokemon getPokemonEquivalent(EntityType<?> type) {
        return mobPokemonEquivalents.getOrDefault(type, defaultMobPokemonEquivalent);
    }
}
