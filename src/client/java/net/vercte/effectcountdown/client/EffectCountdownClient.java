package net.vercte.effectcountdown.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;

public class EffectCountdownClient implements ClientModInitializer {
    private static ModConfig CONFIG = null;

    static void newInstance() { CONFIG = ModConfig.getInstance(); }

    private boolean isEffectImportant(MobEffectInstance effectInstance) {
        return !(CONFIG.ignoreAmbient && effectInstance.isAmbient()) &&
                (CONFIG.ignoreHidden || !effectInstance.isVisible()) &&
                CONFIG.effectMatches(effectInstance);
    }

    private boolean shouldPlaySound(Collection<MobEffectInstance> mobEffects) {
        for(MobEffectInstance effect: mobEffects) {
            if(isEffectImportant(effect) &&
                effect.endsWithin(CONFIG.warningStart) &&
                effect.getDuration() % CONFIG.warningInterval == 0) return true;
        }
        return false;
    }

    private int getLowestEffectDuration(Collection<MobEffectInstance> mobEffects) {
        int lowestDuration = Integer.MAX_VALUE;
        for(MobEffectInstance effect: mobEffects) {
            int duration = effect.getDuration();
            if(isEffectImportant(effect) && duration < lowestDuration) {
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
                boolean shouldPlay = shouldPlaySound(mobEffects);
                if(shouldPlay) {
                    int lowestEffectDuration = getLowestEffectDuration(mobEffects);
                    float pitch = (float)getPitch(lowestEffectDuration);

                    SoundEvent sound = CONFIG.getSound();
                    if(sound != null) player.playSound(sound, 2, pitch);
                }
            }
        });
    }
}
