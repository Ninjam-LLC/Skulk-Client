/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package com.ariesninja.skulkpk.client.mixin.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.ariesninja.skulkpk.client.event.EventManager;
import com.ariesninja.skulkpk.client.event.events.PlayerAfterJumpEvent;
import com.ariesninja.skulkpk.client.event.events.PlayerJumpEvent;
import com.ariesninja.skulkpk.client.utils.aiming.RotationManager;
import com.ariesninja.skulkpk.client.utils.aiming.features.MovementCorrection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {

    @Shadow
    public boolean jumping;

    @Shadow
    public int jumpingCooldown;

    @Shadow
    public abstract float getJumpVelocity();

    @Shadow
    public abstract void jump();

    @Shadow
    public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> effect);

    @Shadow
    public abstract void tick();

    @Shadow public abstract void swingHand(Hand hand, boolean fromServerPlayer);

    @Shadow
    public abstract void setHealth(float health);


    @Shadow
    public abstract boolean isGliding();

    @Shadow
    protected abstract boolean canGlide();

    /**
     * Disable [StatusEffects.LEVITATION] effect when [ModuleAntiLevitation] is enabled
     */
    @ModifyExpressionValue(
            method = "travelMidAir",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/entity/effect/StatusEffectInstance;",
                    ordinal = 0
            ),
            require = 1,
            allow = 1
    )
    public StatusEffectInstance hookTravelStatusEffect(StatusEffectInstance original) {
        // If we get anyting other than levitation, the injection went wrong
        assert original != StatusEffects.LEVITATION;

        return original;
    }

    /**
     * Disable [StatusEffects.SLOW_FALLING] effect when [ModuleAntiLevitation] is enabled
     */
    @ModifyExpressionValue(
            method = "getEffectiveGravity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z",
                    ordinal = 0
            ),
            require = 1,
            allow = 1
    )
    public boolean hookTravelStatusEffect(boolean original) {

        return original;
    }

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void hookAntiNausea(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == StatusEffects.NAUSEA) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Unique
    private PlayerJumpEvent jumpEvent;

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void hookJumpEvent(CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        jumpEvent = EventManager.INSTANCE.callEvent(new PlayerJumpEvent(getJumpVelocity(), this.getYaw()));
        if (jumpEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getJumpVelocity()F"))
    private float hookJumpEvent(float original) {
        // Replaces ((Object) this) != MinecraftClient.getInstance().player
        if (jumpEvent == null) {
            return original;
        }

        return jumpEvent.getMotion();
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float hookJumpYaw(float original) {
        // Replaces ((Object) this) != MinecraftClient.getInstance().player
        if (jumpEvent == null) {
            return original;
        }

        return jumpEvent.getYaw();
    }

    @Inject(method = "jump", at = @At("RETURN"))
    private void hookAfterJumpEvent(CallbackInfo ci) {
        jumpEvent = null;

        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        EventManager.INSTANCE.callEvent(PlayerAfterJumpEvent.INSTANCE);
    }

    /**
     * Hook velocity rotation modification
     * <p>
     * Jump according to modified rotation. Prevents detection by movement sensitive anticheats.
     */
    @ModifyExpressionValue(method = "jump", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookFixRotation(Vec3d original) {
        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();

        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        if (rotationTarget == null || rotationTarget.getMovementCorrection() == MovementCorrection.OFF || rotation == null) {
            return original;
        }

        float yaw = rotation.getYaw() * 0.017453292F;

        return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void hookTickMovement(CallbackInfo callbackInfo) {

    }

    @Inject(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;jumping:Z"))
    private void hookAirJump(CallbackInfo callbackInfo) {
    }

    @Unique
    private boolean previousElytra = false;

    @Inject(method = "tickGliding", at = @At("TAIL"))
    public void recastIfLanded(CallbackInfo callbackInfo) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        var elytra = isGliding();

        previousElytra = elytra;
    }

    /**
     * Gliding using modified-rotation
     */
    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
    private float hookModifyFallFlyingPitch(float original) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();

        if (rotation == null || rotationTarget == null || rotationTarget.getMovementCorrection() == MovementCorrection.OFF) {
            return original;
        }

        return rotation.getPitch();
    }

    @Inject(method = "spawnItemParticles", at = @At("HEAD"), cancellable = true)
    private void hookEatParticles(ItemStack stack, int count, CallbackInfo ci) {
        if (stack.getComponents().contains(DataComponentTypes.FOOD)) {
            ci.cancel();
        }
    }

    /**
     * Gliding using modified-rotation
     */
    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookModifyFallFlyingRotationVector(Vec3d original) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();

        if (rotation == null || rotationTarget == null || rotationTarget.getMovementCorrection() == MovementCorrection.OFF) {
            return original;
        }

        return rotation.getDirectionVector();
    }

    @Unique
    private boolean previousIsGliding = false;

    @Inject(method = "isGliding", at = @At("RETURN"), cancellable = true)
    private void hookIsGliding(CallbackInfoReturnable<Boolean> cir) {

    }
}
