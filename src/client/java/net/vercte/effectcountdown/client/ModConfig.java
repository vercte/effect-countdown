package net.vercte.effectcountdown.client;

import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class ModConfig {
    public static ModConfig INSTANCE = new ModConfig();

    public static ConfigClassHandler<ModConfig> HANDLER = ConfigClassHandler.createBuilder(ModConfig.class)
            .id(new ResourceLocation("effect_countdown", "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("effect_countdown.json5"))
                    .appendGsonBuilder(GsonBuilder::setPrettyPrinting) // not needed, pretty print by default
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry
    public boolean ignoreAmbient = true;

    @SerialEntry
    public boolean ignoreHidden = true;

    @SerialEntry
    public int warningStart = 200;

    @SerialEntry
    public int warningInterval = 20;

    @SerialEntry
    public String warningSound = "block.note_block.hat";

    @SerialEntry
    public @NotNull List<String> effectList = List.of(
            "minecraft:slow_falling",
            "minecraft:levitation",
            "minecraft:invisibility",
            "minecraft:fire_resistance",
            "minecraft:water_breathing"
    );

    public boolean effectMatches(MobEffectInstance instance) {
        ResourceLocation location = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect());
        if(location != null) {
            for(String listEffectID: effectList) {
                ResourceLocation listEffectLocation = new ResourceLocation(listEffectID);
                if(listEffectLocation.equals(location)) return true;
            }
        }
        return false;
    }

    public @Nullable SoundEvent getSound() {
        ResourceLocation soundLocation = new ResourceLocation(this.warningSound);
        return BuiltInRegistries.SOUND_EVENT.get(soundLocation);
    }

    public static ModConfig getInstance() {
        return INSTANCE;
    }
    public static void load() {
        HANDLER.load();
    }

    public @NotNull ConfigCategory getCategory(ModConfig defaults, ModConfig config) {
        ConfigCategory.Builder category = ConfigCategory.createBuilder();
        category.name(Component.translatable("config.effect_countdown.category.main"));
        category.tooltip(Component.translatable("config.effect_countdown.category.main.tooltip"));
        category.group(OptionGroup.createBuilder()
                .name(Component.translatable("config.effect_countdown.group.logic"))
                .description(
                        OptionDescription.of(Component.translatable("config.effect_countdown.group.logic.desc"))
                ).option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.effect_countdown.option.ignore_ambient"))
                        .description(
                                OptionDescription.of(Component.translatable("config.effect_countdown.option.ignore_ambient.desc"))
                        ).binding(defaults.ignoreAmbient, () -> config.ignoreAmbient, newVal -> config.ignoreAmbient = newVal)
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                ).option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.effect_countdown.option.ignore_hidden"))
                        .description(
                                OptionDescription.of(Component.translatable("config.effect_countdown.option.ignore_hidden.desc"))
                        ).binding(defaults.ignoreHidden, () -> config.ignoreHidden, newVal -> config.ignoreHidden = newVal)
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                )
                .build());

        category.group(OptionGroup.createBuilder()
                .name(Component.translatable("config.effect_countdown.group.warning"))
                .description(
                        OptionDescription.of(Component.translatable("config.effect_countdown.group.warning.desc"))
                ).option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.effect_countdown.option.warning_start"))
                        .description(
                                OptionDescription.of(Component.translatable("config.effect_countdown.option.warning_start.desc"))
                        ).binding(defaults.warningStart, () -> config.warningStart, newVal -> config.warningStart = newVal)
                        .controller(IntegerFieldControllerBuilder::create)
                        .build()
                ).option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.effect_countdown.option.warning_interval"))
                        .description(
                                OptionDescription.of(Component.translatable("config.effect_countdown.option.warning_interval.desc"))
                        ).binding(defaults.warningInterval, () -> config.warningInterval, newVal -> config.warningInterval = newVal)
                        .controller(IntegerFieldControllerBuilder::create)
                        .build()
                ).option(Option.<String>createBuilder()
                        .name(Component.translatable("config.effect_countdown.option.warning_sound"))
                        .description(
                                OptionDescription.of(Component.translatable("config.effect_countdown.option.warning_sound.desc"))
                        ).binding(defaults.warningSound, () -> config.warningSound, newVal -> config.warningSound = newVal)
                        .controller(StringControllerBuilder::create)
                        .build()
                )
                .build());
        category.group(ListOption.<String>createBuilder()
                .name(Component.translatable("config.effect_countdown.group.warning_effects"))
                .description(
                        OptionDescription.of(Component.translatable("config.effect_countdown.group.warning_effects.desc"))
                ).binding(defaults.effectList, () -> config.effectList, newVal -> config.effectList = newVal)
                .controller(StringControllerBuilder::create)
                .initial("minecraft:effect_id")
                .build()
        );

        return category.build();
    }

    public @NotNull YetAnotherConfigLib getYACLInstance() {
        return YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> builder
                .title(Component.empty())
                .save(() -> HANDLER.save())
                .category(getCategory(defaults, config))
        );
    }
}
