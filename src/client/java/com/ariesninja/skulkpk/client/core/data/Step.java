package com.ariesninja.skulkpk.client.core.data;

/**
 * Represents a single step in a parkour sequence.
 * Each step contains an action to perform, optional duration, and concurrency settings.
 */
public class Step {

    /**
     * Base action types for simple toggle states
     */
    public enum ActionType {
        JUMP,
        SPRINT,
        SNEAK,
        STOP_MOVEMENT,
        STOP_SPRINT,
        STOP_SNEAK,
        WAIT,
        MOVEMENT,  // Indicates this step uses MovementAction
        ROTATION,   // Indicates this step uses RotationAction

        // Dynamic action types with stop conditions
        ROUGH_START,    // Point camera toward momentum start, move forward until nearby, then shift move to precise position
        ROUGH_MOMENTUM, // Sprint in direction of momentum line until within threshold of jump block
        ROUGH_JUMP,     // Jump while sprinting toward target block

        // Neo action types
        UNIT_SAFE_CORNER, // Move to a safe corner position for unit jumps
        NEO_A             // Perform a specific Neo jump maneuver
    }

    /**
     * Movement-specific actions with duration and direction
     */
    public enum MovementAction {
        MOVE_FORWARD,
        MOVE_BACKWARD,
        MOVE_LEFT,
        MOVE_RIGHT,
        STRAFE_LEFT,
        STRAFE_RIGHT
    }

    /**
     * Rotation-specific actions with speed and angle parameters
     */
    public enum RotationAction {
        ROTATE_LEFT,
        ROTATE_RIGHT,
        ROTATE_TO_ANGLE,
        ROTATE_RELATIVE  // Rotate by a relative amount
    }

    /**
     * Container for movement-specific data
     */
    public static class MovementData {
        private final MovementAction action;
        private final int duration;
        private final float speed; // movement speed multiplier (1.0 = normal)

        public MovementData(MovementAction action, int duration) {
            this(action, duration, 1.0f);
        }

        public MovementData(MovementAction action, int duration, float speed) {
            this.action = action;
            this.duration = duration;
            this.speed = speed;
        }

        public MovementAction getAction() { return action; }
        public int getDuration() { return duration; }
        public float getSpeed() { return speed; }

        @Override
        public String toString() {
            return String.format("MovementData{action=%s, duration=%d, speed=%.2f}",
                               action, duration, speed);
        }
    }

    /**
     * Container for rotation-specific data
     */
    public static class RotationData {
        private final RotationAction action;
        private final float angle; // target angle for ROTATE_TO_ANGLE, relative amount for others
        private final float speed; // rotation speed (degrees per tick)

        public RotationData(RotationAction action, float angle) {
            this(action, angle, 4.5f); // Default ~90 degrees in 20 ticks
        }

        public RotationData(RotationAction action, float angle, float speed) {
            this.action = action;
            this.angle = angle;
            this.speed = speed;
        }

        public RotationAction getAction() { return action; }
        public float getAngle() { return angle; }
        public float getSpeed() { return speed; }

        /**
         * Calculate duration based on angle and speed
         */
        public int getDuration() {
            if (action == RotationAction.ROTATE_TO_ANGLE || action == RotationAction.ROTATE_RELATIVE) {
                return Math.max(1, (int) Math.ceil(Math.abs(angle) / speed));
            }
            return -1; // Indefinite for directional rotations
        }

        @Override
        public String toString() {
            return String.format("RotationData{action=%s, angle=%.2f, speed=%.2f}",
                               action, angle, speed);
        }
    }

    /**
     * Special concurrency values
     */
    public static final int WAIT_UNTIL_FINISHED = -1;
    public static final int RUN_CONCURRENTLY = 0;

    private final ActionType action;
    private final int duration; // in ticks, -1 for instantaneous actions like jump
    private final int concurrency; // ticks to wait before next action, -1 = wait until finished, 0 = concurrent
    private final Object actionData; // MovementData, RotationData, or other action-specific data

    /**
     * Creates a step with an instantaneous action (like jump)
     */
    public Step(ActionType action, int concurrency) {
        this(action, -1, concurrency, null);
    }

    /**
     * Creates a step with duration-based action
     */
    public Step(ActionType action, int duration, int concurrency) {
        this(action, duration, concurrency, null);
    }

    /**
     * Creates a step with action-specific data
     */
    public Step(ActionType action, int duration, int concurrency, Object actionData) {
        this.action = action;
        this.duration = duration;
        this.concurrency = concurrency;
        this.actionData = actionData;
    }

    // Getters
    public ActionType getAction() {
        return action;
    }

    public int getDuration() {
        return duration;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public Object getActionData() {
        return actionData;
    }

    /**
     * Gets movement data if this is a movement step
     */
    public MovementData getMovementData() {
        if (action == ActionType.MOVEMENT && actionData instanceof MovementData) {
            return (MovementData) actionData;
        }
        return null;
    }

    /**
     * Gets rotation data if this is a rotation step
     */
    public RotationData getRotationData() {
        if (action == ActionType.ROTATION && actionData instanceof RotationData) {
            return (RotationData) actionData;
        }
        return null;
    }

    /**
     * Returns true if this action is instantaneous (like jump)
     */
    public boolean isInstantaneous() {
        return duration == -1;
    }

    /**
     * Returns true if this step should wait until completion before proceeding
     */
    public boolean shouldWaitUntilFinished() {
        return concurrency == WAIT_UNTIL_FINISHED;
    }

    /**
     * Returns true if this step should run concurrently with the next
     */
    public boolean shouldRunConcurrently() {
        return concurrency == RUN_CONCURRENTLY;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Step{action=").append(action);
        if (duration != -1) {
            sb.append(", duration=").append(duration);
        }
        sb.append(", concurrency=");
        if (concurrency == WAIT_UNTIL_FINISHED) {
            sb.append("WAIT_UNTIL_FINISHED");
        } else if (concurrency == RUN_CONCURRENTLY) {
            sb.append("RUN_CONCURRENTLY");
        } else {
            sb.append(concurrency);
        }
        if (actionData != null) {
            sb.append(", data=").append(actionData);
        }
        sb.append("}");
        return sb.toString();
    }

    // Factory methods for common step types
    public static Step jump() {
        return new Step(ActionType.JUMP, WAIT_UNTIL_FINISHED);
    }

    public static Step sprint() {
        return new Step(ActionType.SPRINT, RUN_CONCURRENTLY);
    }

    public static Step stopSprint() {
        return new Step(ActionType.STOP_SPRINT, RUN_CONCURRENTLY);
    }

    public static Step sneak() {
        return new Step(ActionType.SNEAK, RUN_CONCURRENTLY);
    }

    public static Step stopSneak() {
        return new Step(ActionType.STOP_SNEAK, RUN_CONCURRENTLY);
    }

    public static Step stopMovement() {
        return new Step(ActionType.STOP_MOVEMENT, RUN_CONCURRENTLY);
    }

    public static Step moveForward(int duration, int concurrency) {
        MovementData data = new MovementData(MovementAction.MOVE_FORWARD, duration);
        return new Step(ActionType.MOVEMENT, duration, concurrency, data);
    }

    public static Step moveForward(int duration, int concurrency, float speed) {
        MovementData data = new MovementData(MovementAction.MOVE_FORWARD, duration, speed);
        return new Step(ActionType.MOVEMENT, duration, concurrency, data);
    }

    public static Step rotateToAngle(float angle, int concurrency) {
        RotationData data = new RotationData(RotationAction.ROTATE_TO_ANGLE, angle);
        return new Step(ActionType.ROTATION, data.getDuration(), concurrency, data);
    }

    public static Step rotateToAngle(float angle, float speed, int concurrency) {
        RotationData data = new RotationData(RotationAction.ROTATE_TO_ANGLE, angle, speed);
        return new Step(ActionType.ROTATION, data.getDuration(), concurrency, data);
    }

    public static Step rotateRelative(float degrees, int concurrency) {
        RotationData data = new RotationData(RotationAction.ROTATE_RELATIVE, degrees);
        return new Step(ActionType.ROTATION, data.getDuration(), concurrency, data);
    }

    public static Step wait(int duration) {
        return new Step(ActionType.WAIT, duration, WAIT_UNTIL_FINISHED);
    }

    // Factory methods for dynamic action types
    public static Step roughStart() {
        return new Step(ActionType.ROUGH_START, WAIT_UNTIL_FINISHED);
    }

    public static Step roughMomentum() {
        return new Step(ActionType.ROUGH_MOMENTUM, WAIT_UNTIL_FINISHED);
    }

    public static Step roughJump() {
        return new Step(ActionType.ROUGH_JUMP, WAIT_UNTIL_FINISHED);
    }

    public static Step unitSafeCorner() {
        return new Step(ActionType.UNIT_SAFE_CORNER, WAIT_UNTIL_FINISHED);
    }

    public static Step neoA() {
        return new Step(ActionType.NEO_A, WAIT_UNTIL_FINISHED);
    }
}
