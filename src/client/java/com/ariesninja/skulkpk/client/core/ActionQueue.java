package com.ariesninja.skulkpk.client.core;

import java.util.LinkedList;
import java.util.Queue;

public class ActionQueue {
    private static final Queue<Object> actionQueue = new LinkedList<>();

    public static void addActions(java.util.List<Object> actions) {
        actionQueue.addAll(actions);
    }

    public static Object getNextAction() {
        return actionQueue.poll();
    }

    public static boolean hasActions() {
        return !actionQueue.isEmpty();
    }
} 