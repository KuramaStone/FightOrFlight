package com.github.kuramastone.fightOrFlight.utils;

import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class TickScheduler {

    private static long tick = 0;
    private static final Map<Long, List<ForgeTask>> scheduledFutureTick = new HashMap<>();

    // Method to queue the task for the next tick
    public static ForgeTask scheduleNextTick(Runnable task) {
        ForgeTask ft = new ForgeTask(task, -1L);
        scheduledFutureTick.computeIfAbsent(tick + 1, (t) -> new ArrayList<>()).add(ft);
        return ft;
    }

    // Method to queue the task for the next tick
    public static ForgeTask scheduleLater(long tickDelay, Runnable task) {
        ForgeTask ft = new ForgeTask(task, -1L);
        scheduledFutureTick.computeIfAbsent(tick + tickDelay, (t) -> new ArrayList<>()).add(ft);
        return ft;
    }

    // Method to schedule a repeating task every 'intervalTicks' ticks
    public static ForgeTask scheduleRepeating(Runnable task, long tickDelay, long intervalTicks) {
        ForgeTask ft = new ForgeTask(task, intervalTicks);
        scheduledFutureTick.computeIfAbsent(tick + tickDelay, (t) -> new ArrayList<>()).add(ft);
        return ft;
    }

    private static boolean isRegistered = false;
    public static void register() {
        if(isRegistered)
            return;
        isRegistered = true;
        ServerTickEvents.START_SERVER_TICK.register(TickScheduler::onServerTick);
    }

    // This event is fired every server tick
    private static void onServerTick(MinecraftServer server) {
        // We only want to run this on the END phase to ensure everything is processed in one tick
            // Run all scheduled tasks
            List<ForgeTask> tasksToRun = scheduledFutureTick.get(tick);

            if (tasksToRun != null) {
                scheduledFutureTick.remove(tick);
                for (ForgeTask task : tasksToRun) {
                    try {
                        task.task.run();
                        // if task is repeating, reschedule it
                        if(task.repeatTimer >= 0 && !task.isCancelled) {
                            scheduledFutureTick.computeIfAbsent(tick+task.repeatTimer, (_a) -> new ArrayList<>()).add(task);
                        }
                    }
                    catch (Exception e) {
                        FightOrFlightMod.logger.error("Error during scheduled runnable's execution.");
                        e.printStackTrace();
                    }
                }
            }

            tick++;
    }

}