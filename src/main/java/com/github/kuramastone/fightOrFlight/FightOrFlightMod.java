package com.github.kuramastone.fightOrFlight;

import com.github.kuramastone.fightOrFlight.commands.CommandHandler;
import com.github.kuramastone.fightOrFlight.listeners.PokemonListener;
import com.github.kuramastone.fightOrFlight.listeners.WandListener;
import com.github.kuramastone.fightOrFlight.utils.ReflectionUtils;
import com.github.kuramastone.fightOrFlight.utils.TickScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class FightOrFlightMod implements ModInitializer {

    public static String MODID = "fightorflight";

    public static FightOrFlightMod instance;
    public static final Logger logger = LogManager.getLogger(MODID);
    private static MinecraftServer minecraftServer;
    private FOFApi api;

    @Override
    public void onInitialize() {
        ReflectionUtils.register();

        instance = this;
        api = new FOFApi();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> onServerStopped());

        TickScheduler.register();
        new CommandHandler(api).register();
        new PokemonListener(api).register();
        new WandListener(api).register();
    }

    private void onServerStarted(MinecraftServer server) {
        minecraftServer = server;
        api.init();
    }

    private void onServerStopped() {
    }

    public FOFApi getAPI() {
        return api;
    }

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public static File defaultDataFolder() {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), MODID);
    }

    private static boolean inDebug = false;
    public static void debug(String msg, Object... objects) {
        if(!inDebug)
            return;

        logger.info(msg, objects);
    }

}

/*
Features:
- Customizability. Almost every value will be customizable such as the whistle item (which will dynamically update in both displayname, lore, and custommodeldata),the damage dealt to non-pokemon entities, the particle density of effects, which moves have dynamic effects enabled like Teleport,
- Wand/whistle: Customizable item that allows players to control their pokemon. Trigger fights or stop the fighting by left or right clicking.
- Wild spawning of extra-aggressive boss pokemon that will attack others far more often
- Pokemon react to being hit or when a pokeball fails. This will trigger an out-of-battle combat where they will attack you. Each type will have a unique attack with special effects such as particles and/or potion effects.
- Natures will affect their aggression and likelihood to attack or flee
- Certain abilities will have special effects such as Intimidate, Run Away, etc
- Some types will have both melee and ranged attacks that trigger the special effects. Melee attacks are literally hitting the player while ranged attacks will send projectiles that target the player.
- Speed will determine how quickly they attack
- Offensive stats will be used in the damage calculation and defense stats will decrease damage taken.
- Certain learned moves will trigger special effects. Teleporting, explosions (non-griefing), self-healing, bounce/fly, etc
- Mobs killed by your pokemon will be considered a kill by the owning player
- Pokemon receive benefits of defeating another pokemon in battle such as exp and evs

These are the type effects currently:
```
Fire types: Shoot fireballs like blazes with red particle paths towards the target
Ice types: Powered snow effect with snowflakes falling in the area
Poison types: Apply Poison and produces green particles
Psychic types: Levitation with occasional "beams" coming from the pokemon. I'd hope to use Guardian style beams
Fairy types: Suffocate with cherry blossom particles
Fighting types: Huge knockback, but skimmers wont read this
Steel types: Apply slowness to the kneecaps
Ground type: Suffocate and push them underground
Rock types: apply mining fatigue, possibly push them underground
Ghost types: ??? Not sure honestly, I'd default to the same as darkness
Dark types: apply darkness to pokemon's trainer and deal normal damage
Electric types: Summons lightning strike
Bug types: apply hunger and periodic damage from "bug bites"
Grass types: Leech seed like effect and slowness
Water types: Drowning effect with water particles surrounding the entity
Flying types: Knockback upward into the air
Dragon types: Heavy damage unless you can think of something better
Normal types: They walk up and hit it. what do you want.
```

Something I'd like to include is pokemon using their actual move animations, but I'm still researching how to do this, so I'm not including it as a feature. If I figure it out, I'll definitely implement it.
 */