package net.vercte.effectcountdown.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

import java.util.Collection;

public class EffectCountdownClient implements ClientModInitializer {
    private static ModConfig CONFIG = null;

    static void newInstance() { CONFIG = ModConfig.getInstance(); }

    private boolean shellInAirCheck(LocalPlayer player, MobEffectInstance effectInstance) {
        if(effectInstance.getEffect() != MobEffects.WATER_BREATHING) return false;
        boolean wearingShell = player.getItemBySlot(EquipmentSlot.HEAD).is(Items.TURTLE_HELMET);
        return wearingShell && !player.isUnderWater();
    }

    private boolean isEffectImportant(LocalPlayer player, MobEffectInstance effectInstance) {
        return !(CONFIG.ignoreAmbient && effectInstance.isAmbient()) &&
                (CONFIG.ignoreHidden || !effectInstance.isVisible()) &&
                !shellInAirCheck(player, effectInstance) &&
                CONFIG.effectMatches(effectInstance);
    }

    private boolean shouldPlaySound(LocalPlayer player, Collection<MobEffectInstance> mobEffects) {
        for(MobEffectInstance effect: mobEffects) {
            if(isEffectImportant(player, effect) &&
                effect.endsWithin(CONFIG.warningStart) &&
                effect.getDuration() % CONFIG.warningInterval == 0) return true;
        }
        return false;
    }

    private int getLowestEffectDuration(LocalPlayer player, Collection<MobEffectInstance> mobEffects) {
        int lowestDuration = Integer.MAX_VALUE;
        for(MobEffectInstance effect: mobEffects) {
            int duration = effect.getDuration();
            if(isEffectImportant(player, effect) && duration < lowestDuration) {
                lowestDuration = duration;
            }
        }
        return lowestDuration;
    }

    private double getPitch(int ticksRemaining) {
        int intervalsRemaining = ticksRemaining / CONFIG.warningInterval;
        int totalIntervals = CONFIG.warningStart / CONFIG.warningInterval;
        return Math.pow(2.0, -intervalsRemaining / (double)totalIntervals);
    }

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        newInstance();

        ClientTickEvents.END_CLIENT_TICK.register((Minecraft minecraft) -> {
            if(minecraft.isPaused()) return;

            LocalPlayer player = minecraft.player;
            if(player == null) return;

            Collection<MobEffectInstance> mobEffects = player.getActiveEffects();
            if(!mobEffects.isEmpty()) {
                boolean shouldPlay = shouldPlaySound(player, mobEffects);
                if(shouldPlay) {
                    int lowestEffectDuration = getLowestEffectDuration(player, mobEffects);
                    float pitch = (float)getPitch(lowestEffectDuration);

                    SoundEvent sound = CONFIG.getSound();
                    if(sound != null) player.playSound(sound, 2, pitch);
                }
            }
        });
    }
}
