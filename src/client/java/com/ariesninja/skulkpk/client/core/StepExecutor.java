package com.ariesninja.skulkpk.client.core;

import com.ariesninja.skulkpk.client.core.data.Step;
import com.ariesninja.skulkpk.client.core.JumpPlanner.JumpLogistics;
import com.ariesninja.skulkpk.client.core.JumpPlanner.StepSequence;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class StepExecutor {

    private static final StepExecutor INSTANCE = new StepExecutor();

    private final JumpPlanner jumpPlanner;
    private StepSequence currentSequence = null;
    private int currentStepIndex = 0;
    private boolean isExecuting = false;

    private StepExecutor() {
        this.jumpPlanner = new JumpPlanner();
    }

    public static StepExecutor getInstance() {
        return INSTANCE;
    }

    /**
     * Called when EXECUTE_KEY is pressed
     */
    public void executeSequence(MinecraftClient client) {
        if (isExecuting) {
            showMessage(client, "Already executing a sequence!", Formatting.YELLOW);
            return;
        }

        // First, try to create logistics from current JumpAnalyzer state
        if (!createLogisticsFromAnalyzer()) {
            showMessage(client, "No valid jump selected! Use SELECT key first.", Formatting.RED);
            return;
        }

        // Process any pending logistics to generate sequences
        jumpPlanner.processLogistics(client);

        // Get the next available sequence
        currentSequence = jumpPlanner.getNextSequence();
        if (currentSequence == null) {
            showMessage(client, "No executable sequence available!", Formatting.RED);
            return;
        }

        // Start execution
        currentStepIndex = 0;
        isExecuting = true;
        showMessage(client, "Starting sequence execution with " + currentSequence.getSteps().size() + " steps", Formatting.GREEN);

        // Execute the first step
        executeCurrentStep();
    }

    /**
     * Creates JumpLogistics from the current JumpAnalyzer state
     */
    private boolean createLogisticsFromAnalyzer() {
        BlockPos jumpFromBlock = JumpAnalyzer.getJumpFromBlock();
        BlockPos targetBlock = JumpAnalyzer.getOptimizedTargetBlock();
        BlockPos momentumStartBlock = JumpAnalyzer.getMomentumStartBlock();

        if (jumpFromBlock == null || targetBlock == null) {
            return false;
        }

        // Use momentum start if available, otherwise fall back to jump block
        Vec3d momentumStartPos = momentumStartBlock != null ?
            Vec3d.ofCenter(momentumStartBlock) : Vec3d.ofCenter(jumpFromBlock);

        // Get precise jump position (0.3 blocks from edge toward target)
        Vec3d jumpPos = calculatePreciseJumpPosition(jumpFromBlock, targetBlock);

        // Get precise target position (center of target block)
        Vec3d targetPos = Vec3d.ofCenter(targetBlock);

        // Calculate logistics data based on jump to target distance
        double distance = jumpPos.distanceTo(targetPos);
        double heightDifference = targetPos.y - jumpPos.y;

        // Determine if sprint/jump is required based on distance and height
        boolean requiresSprint = distance > 3.0 || heightDifference > 0;
        boolean requiresJump = distance > 1.5 || heightDifference > 0;

        // Calculate recommended direction from jump position to target
        Vec3d direction = targetPos.subtract(jumpPos).normalize();

        // Create and observe the logistics
        JumpLogistics logistics = new JumpLogistics(
            momentumStartPos, jumpPos, targetPos, distance, heightDifference,
            requiresSprint, requiresJump, direction
        );

        jumpPlanner.observeLogistics(logistics);
        return true;
    }

    /**
     * Calculates precise jump position 0.3 blocks from block edge toward target
     */
    private Vec3d calculatePreciseJumpPosition(BlockPos jumpBlock, BlockPos targetBlock) {
        Vec3d jumpCenter = Vec3d.ofCenter(jumpBlock);
        Vec3d targetCenter = Vec3d.ofCenter(targetBlock);

        // Calculate direction from jump block to target
        Vec3d direction = targetCenter.subtract(jumpCenter).normalize();

        // Move 0.3 blocks from the center toward the target edge
        return jumpCenter.add(direction.multiply(0.3));
    }

    /**
     * Executes the current step in the sequence
     */
    private void executeCurrentStep() {
        if (currentSequence == null || currentStepIndex >= currentSequence.getSteps().size()) {
            completeExecution();
            return;
        }

        Step currentStep = currentSequence.getSteps().get(currentStepIndex);

        // Create logistics for this step (reuse the same logistics for all steps in sequence)
        BlockPos jumpFrom = JumpAnalyzer.getJumpFromBlock();
        BlockPos target = JumpAnalyzer.getOptimizedTargetBlock();

        if (jumpFrom == null || target == null) {
            showMessage(MinecraftClient.getInstance(), "Lost jump data during execution!", Formatting.RED);
            stopExecution();
            return;
        }

        JumpLogistics logistics = createLogisticsForStep(jumpFrom, target);

        // Execute the step using PlayerController
        PlayerController.executeStep(currentStep, logistics);

        System.out.println("StepExecutor: Executing step " + (currentStepIndex + 1) + "/" +
                          currentSequence.getSteps().size() + " - " + currentStep.getAction());
    }

    /**
     * Creates JumpLogistics for a specific step
     */
    private JumpLogistics createLogisticsForStep(BlockPos jumpFromBlock, BlockPos targetBlock) {
        BlockPos momentumStartBlock = JumpAnalyzer.getMomentumStartBlock();

        // Use momentum start if available, otherwise fall back to jump block
        Vec3d momentumStartPos = momentumStartBlock != null ?
            Vec3d.ofCenter(momentumStartBlock) : Vec3d.ofCenter(jumpFromBlock);

        // Get precise jump position (0.3 blocks from edge toward target)
        Vec3d jumpPos = calculatePreciseJumpPosition(jumpFromBlock, targetBlock);

        // Get precise target position (center of target block)
        Vec3d targetPos = Vec3d.ofCenter(targetBlock);

        // Calculate logistics data based on jump to target distance
        double distance = jumpPos.distanceTo(targetPos);
        double heightDifference = targetPos.y - jumpPos.y;
        boolean requiresSprint = distance > 3.0 || heightDifference > 0;
        boolean requiresJump = distance > 1.5 || heightDifference > 0;
        Vec3d direction = targetPos.subtract(jumpPos).normalize();

        return new JumpLogistics(
            momentumStartPos, jumpPos, targetPos, distance, heightDifference,
            requiresSprint, requiresJump, direction
        );
    }

    /**
     * Called every client tick to check step completion and advance
     */
    public void tick(MinecraftClient client) {
        if (!isExecuting || currentSequence == null) {
            return;
        }

        // Check if current step is complete
        if (PlayerController.isStepComplete()) {
            currentStepIndex++;

            if (currentStepIndex < currentSequence.getSteps().size()) {
                // Execute next step
                executeCurrentStep();
            } else {
                // Sequence complete
                completeExecution();
            }
        }
    }

    /**
     * Completes the current execution
     */
    private void completeExecution() {
        showMessage(MinecraftClient.getInstance(), "Sequence execution completed!", Formatting.GREEN);
        isExecuting = false;
        currentSequence = null;
        currentStepIndex = 0;
        PlayerController.clearCurrentStep();
    }

    /**
     * Stops the current execution
     */
    public void stopExecution() {
        if (isExecuting) {
            showMessage(MinecraftClient.getInstance(), "Sequence execution stopped!", Formatting.YELLOW);
            isExecuting = false;
            currentSequence = null;
            currentStepIndex = 0;
            PlayerController.clearCurrentStep();
            PlayerController.stopAllMovement(MinecraftClient.getInstance());
        }
    }

    /**
     * Checks if currently executing a sequence
     */
    public boolean isExecuting() {
        return isExecuting;
    }

    /**
     * Gets the current execution progress
     */
    public String getExecutionStatus() {
        if (!isExecuting || currentSequence == null) {
            return "Not executing";
        }

        return String.format("Step %d/%d (%s)",
                           currentStepIndex + 1,
                           currentSequence.getSteps().size(),
                           currentSequence.getSteps().get(currentStepIndex).getAction());
    }

    /**
     * Shows a formatted message to the player
     */
    private void showMessage(MinecraftClient client, String message, Formatting color) {
        if (client.player != null) {
            String prefix = "Skulk";
            String arrow = " > ";

            Text prefixText = Text.literal(prefix).formatted(Formatting.AQUA, Formatting.BOLD);
            Text arrowText = Text.literal(arrow).formatted(Formatting.GRAY);
            Text messageText = Text.literal(message).formatted(color);

            Text fullMessage = Text.empty().append(prefixText).append(arrowText).append(messageText);
            client.player.sendMessage(fullMessage, false);
        }
    }

    /**
     * Clears all pending data
     */
    public void clear() {
        stopExecution();
        jumpPlanner.clear();
    }
}
