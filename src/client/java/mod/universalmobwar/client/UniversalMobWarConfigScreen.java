package mod.universalmobwar.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import mod.universalmobwar.config.ModConfig;


import net.minecraft.client.gui.widget.SliderWidget;

public class UniversalMobWarConfigScreen extends Screen {
    private final Screen parent;
    private CheckboxWidget enabledCheckbox;
    private CheckboxWidget scalingCheckbox;
    private CheckboxWidget ignoreSameSpeciesCheckbox;
    private CheckboxWidget targetPlayersCheckbox;
    private CheckboxWidget neutralMobsAlwaysAggressiveCheckbox;
    private CheckboxWidget allianceSystemEnabledCheckbox;
    private CheckboxWidget evolutionSystemEnabledCheckbox;
    private RangeSlider rangeMultiplierSlider;

    // Advanced/Scaling
    private DoubleSliderWidget dayScalingMultiplierSlider;
    private DoubleSliderWidget killScalingMultiplierSlider;
    private IntSliderWidget maxTierSlider;
    private CheckboxWidget allowBossScalingCheckbox;
    private CheckboxWidget allowModdedScalingCheckbox;
    private CheckboxWidget restrictEffectsToMobThemeCheckbox;
    private CheckboxWidget debugLoggingCheckbox;

    // Performance
    private IntSliderWidget targetingCacheMsSlider;
    private IntSliderWidget targetingMaxQueriesPerTickSlider;
    private IntSliderWidget mobDataSaveDebounceMsSlider;
    private CheckboxWidget enableBatchingCheckbox;
    private CheckboxWidget enableAsyncTasksCheckbox;
    private IntSliderWidget maxParticlesPerConnectionSlider;
    private IntSliderWidget maxDrawnMinionConnectionsSlider;
    private IntSliderWidget minFpsForVisualsSlider;

    // Evolution
    private IntSliderWidget maxLevelSlider;
    private IntSliderWidget killsPerLevelSlider;
    private CheckboxWidget giveEquipmentToMobsCheckbox;

    // Alliance
    private IntSliderWidget allianceDurationTicksSlider;
    private IntSliderWidget sameSpeciesAllianceDurationTicksSlider;
    private DoubleSliderWidget allianceRangeSlider;
    private DoubleSliderWidget allianceBreakChanceSlider;
    private DoubleSliderWidget sameSpeciesAllianceBreakChanceSlider;

    // Visuals
    private CheckboxWidget showTargetLinesCheckbox;
    private CheckboxWidget showHealthBarsCheckbox;
    private CheckboxWidget showMobLabelsCheckbox;
    private CheckboxWidget showLevelParticlesCheckbox;

    public UniversalMobWarConfigScreen(Screen parent) {
        super(Text.literal("Universal Mob War Config"));
        this.parent = parent;
    }

    @Override

    protected void init() {
        ModConfig config = ModConfig.getInstance();
        int y = this.height / 6;
        int x = this.width / 2 - 110;

        // --- Core Toggles ---
        enabledCheckbox = CheckboxWidget.builder(Text.literal("Enable Universal Mob War"), this.textRenderer)
            .pos(x, y).checked(config.modEnabled).build();
        this.addDrawableChild(enabledCheckbox); y += 24;
        scalingCheckbox = CheckboxWidget.builder(Text.literal("Enable Global Mob Scaling System"), this.textRenderer)
            .pos(x, y).checked(config.scalingEnabled).build();
        this.addDrawableChild(scalingCheckbox); y += 24;
        ignoreSameSpeciesCheckbox = CheckboxWidget.builder(Text.literal("Same-species alliances (ignoreSameSpecies)"), this.textRenderer)
            .pos(x, y).checked(config.ignoreSameSpecies).build();
        this.addDrawableChild(ignoreSameSpeciesCheckbox); y += 24;
        targetPlayersCheckbox = CheckboxWidget.builder(Text.literal("Player immunity (targetPlayers)"), this.textRenderer)
            .pos(x, y).checked(config.targetPlayers).build();
        this.addDrawableChild(targetPlayersCheckbox); y += 24;
        neutralMobsAlwaysAggressiveCheckbox = CheckboxWidget.builder(Text.literal("Neutral mobs always hostile (neutralMobsAlwaysAggressive)"), this.textRenderer)
            .pos(x, y).checked(config.neutralMobsAlwaysAggressive).build();
        this.addDrawableChild(neutralMobsAlwaysAggressiveCheckbox); y += 24;
        allianceSystemEnabledCheckbox = CheckboxWidget.builder(Text.literal("Enable alliance system (allianceSystemEnabled)"), this.textRenderer)
            .pos(x, y).checked(config.allianceSystemEnabled).build();
        this.addDrawableChild(allianceSystemEnabledCheckbox); y += 24;
        evolutionSystemEnabledCheckbox = CheckboxWidget.builder(Text.literal("Enable mob leveling (evolutionSystemEnabled)"), this.textRenderer)
            .pos(x, y).checked(config.evolutionSystemEnabled).build();
        this.addDrawableChild(evolutionSystemEnabledCheckbox); y += 24;
        rangeMultiplierSlider = new RangeSlider(x, y, 200, 20, config.rangeMultiplier);
        this.addDrawableChild(rangeMultiplierSlider); y += 28;

        // --- Scaling/Advanced ---
        dayScalingMultiplierSlider = new DoubleSliderWidget(x, y, 200, 20, "Day scaling multiplier", config.dayScalingMultiplier, 0.1, 10.0, 2);
        this.addDrawableChild(dayScalingMultiplierSlider); y += 24;
        killScalingMultiplierSlider = new DoubleSliderWidget(x, y, 200, 20, "Kill scaling multiplier", config.killScalingMultiplier, 0.1, 10.0, 2);
        this.addDrawableChild(killScalingMultiplierSlider); y += 24;
        maxTierSlider = new IntSliderWidget(x, y, 200, 20, "Max scaling tier", config.maxTier, 1, 50);
        this.addDrawableChild(maxTierSlider); y += 24;
        allowBossScalingCheckbox = CheckboxWidget.builder(Text.literal("Allow boss scaling"), this.textRenderer)
            .pos(x, y).checked(config.allowBossScaling).build();
        this.addDrawableChild(allowBossScalingCheckbox); y += 24;
        allowModdedScalingCheckbox = CheckboxWidget.builder(Text.literal("Allow modded mob scaling"), this.textRenderer)
            .pos(x, y).checked(config.allowModdedScaling).build();
        this.addDrawableChild(allowModdedScalingCheckbox); y += 24;
        restrictEffectsToMobThemeCheckbox = CheckboxWidget.builder(Text.literal("Restrict effects to mob theme"), this.textRenderer)
            .pos(x, y).checked(config.restrictEffectsToMobTheme).build();
        this.addDrawableChild(restrictEffectsToMobThemeCheckbox); y += 24;
        debugLoggingCheckbox = CheckboxWidget.builder(Text.literal("Enable debug logging"), this.textRenderer)
            .pos(x, y).checked(config.debugLogging).build();
        this.addDrawableChild(debugLoggingCheckbox); y += 24;

        // --- Performance ---
        targetingCacheMsSlider = new IntSliderWidget(x, y, 200, 20, "Targeting cache ms", config.targetingCacheMs, 100, 10000);
        this.addDrawableChild(targetingCacheMsSlider); y += 24;
        targetingMaxQueriesPerTickSlider = new IntSliderWidget(x, y, 200, 20, "Max queries/tick", config.targetingMaxQueriesPerTick, 1, 200);
        this.addDrawableChild(targetingMaxQueriesPerTickSlider); y += 24;
        mobDataSaveDebounceMsSlider = new IntSliderWidget(x, y, 200, 20, "Mob data save debounce ms", config.mobDataSaveDebounceMs, 10, 2000);
        this.addDrawableChild(mobDataSaveDebounceMsSlider); y += 24;
        enableBatchingCheckbox = CheckboxWidget.builder(Text.literal("Enable batching (alliance/evolution)"), this.textRenderer)
            .pos(x, y).checked(config.enableBatching).build();
        this.addDrawableChild(enableBatchingCheckbox); y += 24;
        enableAsyncTasksCheckbox = CheckboxWidget.builder(Text.literal("Enable async tasks"), this.textRenderer)
            .pos(x, y).checked(config.enableAsyncTasks).build();
        this.addDrawableChild(enableAsyncTasksCheckbox); y += 24;
        maxParticlesPerConnectionSlider = new IntSliderWidget(x, y, 200, 20, "Max particles/connection", config.maxParticlesPerConnection, 1, 32);
        this.addDrawableChild(maxParticlesPerConnectionSlider); y += 24;
        maxDrawnMinionConnectionsSlider = new IntSliderWidget(x, y, 200, 20, "Max minion lines/boss", config.maxDrawnMinionConnections, 1, 50);
        this.addDrawableChild(maxDrawnMinionConnectionsSlider); y += 24;
        minFpsForVisualsSlider = new IntSliderWidget(x, y, 200, 20, "Min FPS for visuals", config.minFpsForVisuals, 5, 120);
        this.addDrawableChild(minFpsForVisualsSlider); y += 24;

        // --- Evolution ---
        maxLevelSlider = new IntSliderWidget(x, y, 200, 20, "Max mob level", config.maxLevel, 1, 200);
        this.addDrawableChild(maxLevelSlider); y += 24;
        killsPerLevelSlider = new IntSliderWidget(x, y, 200, 20, "Kills per level", config.killsPerLevel, 1, 20);
        this.addDrawableChild(killsPerLevelSlider); y += 24;
        giveEquipmentToMobsCheckbox = CheckboxWidget.builder(Text.literal("Give equipment to mobs"), this.textRenderer)
            .pos(x, y).checked(config.giveEquipmentToMobs).build();
        this.addDrawableChild(giveEquipmentToMobsCheckbox); y += 24;

        // --- Alliance ---
        allianceDurationTicksSlider = new IntSliderWidget(x, y, 200, 20, "Alliance duration (ticks)", config.allianceDurationTicks, 20, 2000);
        this.addDrawableChild(allianceDurationTicksSlider); y += 24;
        sameSpeciesAllianceDurationTicksSlider = new IntSliderWidget(x, y, 200, 20, "Same-species alliance duration (ticks)", config.sameSpeciesAllianceDurationTicks, 20, 4000);
        this.addDrawableChild(sameSpeciesAllianceDurationTicksSlider); y += 24;
        allianceRangeSlider = new DoubleSliderWidget(x, y, 200, 20, "Alliance range", config.allianceRange, 1.0, 64.0, 1);
        this.addDrawableChild(allianceRangeSlider); y += 24;
        allianceBreakChanceSlider = new DoubleSliderWidget(x, y, 200, 20, "Alliance break chance", config.allianceBreakChance, 0.0, 1.0, 2);
        this.addDrawableChild(allianceBreakChanceSlider); y += 24;
        sameSpeciesAllianceBreakChanceSlider = new DoubleSliderWidget(x, y, 200, 20, "Same-species alliance break chance", config.sameSpeciesAllianceBreakChance, 0.0, 1.0, 2);
        this.addDrawableChild(sameSpeciesAllianceBreakChanceSlider); y += 24;

        // --- Visuals ---
        showTargetLinesCheckbox = CheckboxWidget.builder(Text.literal("Show target lines"), this.textRenderer)
            .pos(x, y).checked(config.showTargetLines).build();
        this.addDrawableChild(showTargetLinesCheckbox); y += 24;
        showHealthBarsCheckbox = CheckboxWidget.builder(Text.literal("Show health bars"), this.textRenderer)
            .pos(x, y).checked(config.showHealthBars).build();
        this.addDrawableChild(showHealthBarsCheckbox); y += 24;
        showMobLabelsCheckbox = CheckboxWidget.builder(Text.literal("Show mob labels"), this.textRenderer)
            .pos(x, y).checked(config.showMobLabels).build();
        this.addDrawableChild(showMobLabelsCheckbox); y += 24;
        showLevelParticlesCheckbox = CheckboxWidget.builder(Text.literal("Show level particles"), this.textRenderer)
            .pos(x, y).checked(config.showLevelParticles).build();
        this.addDrawableChild(showLevelParticlesCheckbox); y += 28;

        // --- Done Button ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> {
            config.modEnabled = enabledCheckbox.isChecked();
            config.scalingEnabled = scalingCheckbox.isChecked();
            config.ignoreSameSpecies = ignoreSameSpeciesCheckbox.isChecked();
            config.targetPlayers = targetPlayersCheckbox.isChecked();
            config.neutralMobsAlwaysAggressive = neutralMobsAlwaysAggressiveCheckbox.isChecked();
            config.allianceSystemEnabled = allianceSystemEnabledCheckbox.isChecked();
            config.evolutionSystemEnabled = evolutionSystemEnabledCheckbox.isChecked();
            config.rangeMultiplier = rangeMultiplierSlider.getValue();

            // Scaling/Advanced
            config.dayScalingMultiplier = dayScalingMultiplierSlider.getValue();
            config.killScalingMultiplier = killScalingMultiplierSlider.getValue();
            config.maxTier = maxTierSlider.getValue();
            config.allowBossScaling = allowBossScalingCheckbox.isChecked();
            config.allowModdedScaling = allowModdedScalingCheckbox.isChecked();
            config.restrictEffectsToMobTheme = restrictEffectsToMobThemeCheckbox.isChecked();
            config.debugLogging = debugLoggingCheckbox.isChecked();

            // Performance
            config.targetingCacheMs = targetingCacheMsSlider.getValue();
            config.targetingMaxQueriesPerTick = targetingMaxQueriesPerTickSlider.getValue();
            config.mobDataSaveDebounceMs = mobDataSaveDebounceMsSlider.getValue();
            config.enableBatching = enableBatchingCheckbox.isChecked();
            config.enableAsyncTasks = enableAsyncTasksCheckbox.isChecked();
            config.maxParticlesPerConnection = maxParticlesPerConnectionSlider.getValue();
            config.maxDrawnMinionConnections = maxDrawnMinionConnectionsSlider.getValue();
            config.minFpsForVisuals = minFpsForVisualsSlider.getValue();

            // Evolution
            config.maxLevel = maxLevelSlider.getValue();
            config.killsPerLevel = killsPerLevelSlider.getValue();
            config.giveEquipmentToMobs = giveEquipmentToMobsCheckbox.isChecked();

            // Alliance
            config.allianceDurationTicks = allianceDurationTicksSlider.getValue();
            config.sameSpeciesAllianceDurationTicks = sameSpeciesAllianceDurationTicksSlider.getValue();
            config.allianceRange = allianceRangeSlider.getValue();
            config.allianceBreakChance = allianceBreakChanceSlider.getValue();
            config.sameSpeciesAllianceBreakChance = sameSpeciesAllianceBreakChanceSlider.getValue();

            // Visuals
            config.showTargetLines = showTargetLinesCheckbox.isChecked();
            config.showHealthBars = showHealthBarsCheckbox.isChecked();
            config.showMobLabels = showMobLabelsCheckbox.isChecked();
            config.showLevelParticles = showLevelParticlesCheckbox.isChecked();

            config.save();
            this.client.setScreen(parent);
        }).position(x, y).size(200, 20).build());
    }
    // ...existing code...
    // Place these inner classes at the end of the main class, before the closing brace
    // ...existing code...

    // Integer slider widget for int config values
    private static class IntSliderWidget extends SliderWidget {
        private final int min;
        private final int max;
        private final String label;
        public IntSliderWidget(int x, int y, int width, int height, String label, int initialValue, int min, int max) {
            super(x, y, width, height, Text.literal(""), (initialValue - min) / (double)(max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            updateMessage();
        }
        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + getValue()));
        }
        @Override
        protected void applyValue() {}
        public int getValue() {
            return min + (int)Math.round((max - min) * this.value);
        }
    }

    // Double slider widget for double config values
    private static class DoubleSliderWidget extends SliderWidget {
        private final double min;
        private final double max;
        private final String label;
        private final int decimals;
        public DoubleSliderWidget(int x, int y, int width, int height, String label, double initialValue, double min, double max, int decimals) {
            super(x, y, width, height, Text.literal(""), (initialValue - min) / (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.decimals = decimals;
            updateMessage();
        }
        @Override
        protected void updateMessage() {
            String fmt = "%s: %%." + decimals + "f";
            setMessage(Text.literal(String.format(fmt, label, getValue())));
        }
        @Override
        protected void applyValue() {}
        public double getValue() {
            return min + (max - min) * this.value;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
    // Custom slider for rangeMultiplier
    private static class RangeSlider extends SliderWidget {
        public RangeSlider(int x, int y, int width, int height, double initialValue) {
            super(x, y, width, height, Text.literal(""), (initialValue - 0.01) / (100.0 - 0.01));
            updateMessage();
        }
        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Detection range multiplier: " + String.format("%.2f", getValue())));
        }
        @Override
        protected void applyValue() {
            // No-op, handled on Done
        }
        public double getValue() {
            return 0.01 + (100.0 - 0.01) * this.value;
        }
    }
}
