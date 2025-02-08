package com.github.kuramastone.fightOrFlight.utils;

public class ForgeTask {
    public boolean isCancelled = false;
    public Runnable task;
    public long repeatTimer;

    public ForgeTask(Runnable task, long repeatTimer) {
        this.task = task;
        this.repeatTimer = repeatTimer;
    }

}