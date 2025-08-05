package com.ariesninja.skulkpk.client.core;

import com.ariesninja.skulkpk.client.core.data.Step;
import com.ariesninja.skulkpk.client.core.physics.Align;
import com.ariesninja.skulkpk.client.core.physics.Momentum;
import com.ariesninja.skulkpk.client.core.physics.Avoidance;
import com.ariesninja.skulkpk.client.core.physics.Obstructions;
import com.jcraft.jorbis.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class JumpPlanner {

    /**
     * Represents the logistics data received from the Jump analyzer
     */
    public static class JumpLogistics {
        private final Vec3d momentumStartPos;
        private final Vec3d jumpPos;
        private final Vec3d targetPos;
        private final double distance;
        private final double heightDifference;
        private final double offset;
        private final Vec3d recommendedDirection;
        private final BlockPos jumpBlockPos;
        private final BlockPos targetBlockPos;

        public JumpLogistics(Vec3d momentumStartPos, Vec3d jumpPos, Vec3d targetPos, double distance,
                             double heightDifference, double offset,
                             Vec3d recommendedDirection, BlockPos jumpBlockPos, BlockPos targetBlockPos) {
            this.momentumStartPos = momentumStartPos;
            this.jumpPos = jumpPos;
            this.targetPos = targetPos;
            this.distance = distance;
            this.heightDifference = heightDifference;
            this.offset = offset;
            this.recommendedDirection = recommendedDirection;
            this.jumpBlockPos = jumpBlockPos;
            this.targetBlockPos = targetBlockPos;
        }

        // Getters
        public Vec3d getMomentumStartPos() { return momentumStartPos; }
        public Vec3d getJumpPos() { return jumpPos; }
        public Vec3d getTargetPos() { return targetPos; }
        public double getDistance() { return distance; }
        public double getHeightDifference() { return heightDifference; }
        public double getOffset() { return offset; }
        public Vec3d getRecommendedDirection() { return recommendedDirection; }
        public BlockPos getJumpBlockPos() { return jumpBlockPos; }
        public BlockPos getTargetBlockPos() { return targetBlockPos; }
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
            System.out.println("JumpPlanner: Received logistics for jump from momentum start " +
                    logistics.getMomentumStartPos() + " -> jump pos " + logistics.getJumpPos() +
                    " -> target " + logistics.getTargetPos());
        }
    }

    /**
     * Processes pending logistics and generates step sequences
     */
    public void processLogistics(MinecraftClient client) {
        while (!pendingLogistics.isEmpty()) {
            JumpLogistics logistics = pendingLogistics.poll();
            StepSequence sequence = generateStepSequence(client, logistics);
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
    private StepSequence generateStepSequence(MinecraftClient client, JumpLogistics logistics) {
        List<Step> steps = new ArrayList<>();

        // Run physics-based checks to determine complexity
        boolean requiresAnyMomentum = Momentum.requiresAnyMomentum(client.player, logistics);
        boolean requiresPreciseAlignment = Align.requiresPreciseAlignment(logistics);
        boolean requiresAdvancedMomentum = Momentum.requiresAdvancedMomentum(logistics);
        boolean requiresObstacleAvoidance = Avoidance.requiresObstacleAvoidance(logistics);

        System.out.println("requiresAnyMomentum: " + requiresAnyMomentum);

        System.out.println("JumpPlanner: Physics checks - Precise Alignment: " + requiresPreciseAlignment +
                ", Advanced Momentum: " + requiresAdvancedMomentum +
                ", Obstacle Avoidance: " + requiresObstacleAvoidance);

        if (Obstructions.isMinorNeo(client, logistics)) {
            System.out.println("JumpPlanner: Short neo jump detected, using neo A strategy");
            // Use the neo A strategy for minor neos
            steps.add(Step.unitSafeCorner());
            steps.add(Step.neoA());
            return new StepSequence(steps);
        }

        // For now, all physics checks return false, so we use the simple 3-step strategy
        if (!requiresPreciseAlignment && !requiresAdvancedMomentum && !requiresObstacleAvoidance) {
            //            // Use the simple 3-step rogh jump strategy
            if (requiresAnyMomentum) {
                steps.add(Step.roughStart());
            }
            steps.add(Step.roughMomentum());
            steps.add(Step.roughJump());

            System.out.println("JumpPlanner: Using simple 3-step rough jump strategy");
        } else {
            // TODO: Implement complex jump strategies for cases requiring:
            // - Precise alignment
            // - Advanced momentum calculations
            // - Obstacle avoidance
            System.out.println("JumpPlanner: Complex jump strategies not yet implemented");
            return null;
        }

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
