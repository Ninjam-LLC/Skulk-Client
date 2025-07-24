package com.ariesninja.skulkpk.client.core;

public class PlayerController {

    public static void executeAction() {
        if (ActionQueue.hasActions()) {
            Object action = ActionQueue.getNextAction();
            System.out.println("Executing action: " + action);
            // Placeholder for action execution logic
        }
    }
} 