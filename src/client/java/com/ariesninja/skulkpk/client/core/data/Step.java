package com.ariesninja.skulkpk.client.core.data;

/**
 * Represents a single step in a parkour sequence.
 * Each step contains a series of actions that the player will perform.
 */
public enum Step {

    ROUGH_START,    // Point camera toward momentum start, move forward until nearby, then shift move to precise position
    ROUGH_MOMENTUM, // Sprint in direction of momentum line until within threshold of jump block
    ROUGH_JUMP,     // Jump while sprinting toward target block

    // Neo action types
    UNIT_SAFE_CORNER, // Move to a safe corner position for unit jumps
    UNIT_SAFE_CORNER_BACK,  // Move to a safe corner position for unit jumps, at the back
    NEO_A,             // Perform a specific Neo jump maneuver
    NEO_3_AP        // Perform a triple Neo jump with AP pattern

}
