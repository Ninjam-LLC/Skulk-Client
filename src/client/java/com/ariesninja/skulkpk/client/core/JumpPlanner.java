package com.ariesninja.skulkpk.client.core;

import com.ariesninja.skulkpk.client.core.data.Step;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class JumpPlanner {

    /**
     * Represents the logistics data received from the Jump analyzer
     */
    public static class JumpLogistics {
        private final BlockPos startPos;
        private final BlockPos targetPos;
        private final double distance;
        private final double heightDifference;
        private final boolean requiresSprint;
        private final boolean requiresJump;
        private final Vec3d recommendedDirection;
        private final Map<String, Object> additionalData;

        public JumpLogistics(BlockPos startPos, BlockPos targetPos, double distance,
                             double heightDifference, boolean requiresSprint, boolean requiresJump,
                             Vec3d recommendedDirection) {
            this.startPos = startPos;
            this.targetPos = targetPos;
            this.distance = distance;
            this.heightDifference = heightDifference;
            this.requiresSprint = requiresSprint;
            this.requiresJump = requiresJump;
            this.recommendedDirection = recommendedDirection;
            this.additionalData = new HashMap<>();
        }

        // Getters
        public BlockPos getStartPos() { return startPos; }
        public BlockPos getTargetPos() { return targetPos; }
        public double getDistance() { return distance; }
        public double getHeightDifference() { return heightDifference; }
        public boolean requiresSprint() { return requiresSprint; }
        public boolean requiresJump() { return requiresJump; }
        public Vec3d getRecommendedDirection() { return recommendedDirection; }
        public Map<String, Object> getAdditionalData() { return additionalData; }
    }

    /**
     * Represents a complete sequence of steps for a parkour movement
     */
    public static class StepSequence {
        private final List<Step> steps;

        public StepSequence(List<Step> steps) {
            this.steps = new ArrayList<>(steps);
        }

        public List<Step> getSteps() { return Collections.unmodifiableList(steps); }

        @Override
        public String toString() {
            return String.format("StepSequence{steps=%d}", steps.size());
        }
    }

    private final Queue<JumpLogistics> pendingLogistics;
    private final List<StepSequence> generatedSequences;

    public JumpPlanner() {
        this.pendingLogistics = new LinkedList<>();
        this.generatedSequences = new ArrayList<>();
    }

    /**
     * Receives logistics data from the Jump analyzer
     */
    public void observeLogistics(JumpLogistics logistics) {
        if (logistics != null) {
            pendingLogistics.offer(logistics);
            System.out.println("JumpPlanner: Received logistics for jump from " +
                    logistics.getStartPos() + " to " + logistics.getTargetPos());
        }
    }

    /**
     * Processes pending logistics and generates step sequences
     */
    public void processLogistics() {
        while (!pendingLogistics.isEmpty()) {
            JumpLogistics logistics = pendingLogistics.poll();
            StepSequence sequence = generateStepSequence(logistics);
            if (sequence != null) {
                generatedSequences.add(sequence);
                System.out.println("JumpPlanner: Generated sequence - " + sequence);
            }
        }
    }

    /**
     * Generates a step sequence based on the provided logistics
     * This is where the main planning logic would be implemented
     */
    private StepSequence generateStepSequence(JumpLogistics logistics) {
        List<Step> steps = new ArrayList<>();

        // TODO: Implement actual planning logic based on logistics
        // For now, this is a placeholder structure showing how it would work

        // Example basic sequence structure:
        // 1. Rotate towards target if needed
        // 2. Start sprinting if required
        // 3. Move forward
        // 4. Jump at the right time
        // 5. Continue movement in air
        // 6. Land and stop

        return new StepSequence(steps);
    }

    /**
     * Gets the next available step sequence
     */
    public StepSequence getNextSequence() {
        if (!generatedSequences.isEmpty()) {
            return generatedSequences.remove(0);
        }
        return null;
    }

    /**
     * Checks if there are pending logistics to process
     */
    public boolean hasPendingLogistics() {
        return !pendingLogistics.isEmpty();
    }

    /**
     * Checks if there are generated sequences available
     */
    public boolean hasGeneratedSequences() {
        return !generatedSequences.isEmpty();
    }

    /**
     * Gets the number of pending logistics
     */
    public int getPendingLogisticsCount() {
        return pendingLogistics.size();
    }

    /**
     * Gets the number of generated sequences
     */
    public int getGeneratedSequencesCount() {
        return generatedSequences.size();
    }

    /**
     * Clears all pending logistics and generated sequences
     */
    public void clear() {
        pendingLogistics.clear();
        generatedSequences.clear();
    }
}
