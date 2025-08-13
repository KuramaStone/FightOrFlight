package com.github.kuramastone.fightOrFlight.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExpiryList<T> {

    private final List<Entry<T>> list = new ArrayList<>();

    private static class Entry<T> {
        T object;
        long expiryTime;

        Entry(T object, long expiryTime) {
            this.object = object;
            this.expiryTime = expiryTime;
        }
    }

    public void clear() {
        list.clear();
    }

    public boolean remove(T obj) {
        return list.removeIf(it -> it.object == obj);
    }

    public T getAndCheckExpiration(T obj) {
        Iterator<Entry<T>> iterator = list.iterator();
        while (iterator.hasNext()) {
            Entry<T> entry = iterator.next();
            if (entry.object.equals(obj)) {
                if (entry.expiryTime <= System.currentTimeMillis()) {
                    iterator.remove();
                    return null;
                }
                return entry.object;
            }
        }
        return null;
    }

    public void addExpiration(T obj, Duration duration) {
        long expiryTime = System.currentTimeMillis() + duration.toMillis();
        list.add(new Entry<>(obj, expiryTime));
    }

    public boolean contains(T obj) {
        return getAndCheckExpiration(obj) != null;
    }

    public String getDurationFor(T obj) {
        long now = System.currentTimeMillis();
        for (Entry<T> entry : list) {
            if (entry.object.equals(obj)) {
                if (entry.expiryTime <= now) {
                    return "0s";
                }
                long millisLeft = entry.expiryTime - now;
                long minutes = millisLeft / (60 * 1000);
                long seconds = (millisLeft % (60 * 1000)) / 1000;
                return String.format("%dm%02ds", minutes, seconds);
            }
        }
        return "0s";
    }
}
