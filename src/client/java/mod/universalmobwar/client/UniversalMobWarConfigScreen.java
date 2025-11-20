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

    public UniversalMobWarConfigScreen(Screen parent) {
        super(Text.literal("Universal Mob War Config"));
        this.parent = parent;
    }

    @Override

    protected void init() {
        ModConfig config = ModConfig.getInstance();
        int y = this.height / 4;
        enabledCheckbox = CheckboxWidget.builder(Text.literal("Enable Universal Mob War"), this.textRenderer)
            .pos(this.width / 2 - 100, y)
            .checked(config.modEnabled)
            .build();
        this.addDrawableChild(enabledCheckbox);
        y += 24;

        scalingCheckbox = CheckboxWidget.builder(Text.literal("Enable Global Mob Scaling System"), this.textRenderer)
            .pos(this.width / 2 - 100, y)
            .checked(config.scalingEnabled)
            .build();
        this.addDrawableChild(scalingCheckbox);
        y += 24;

        ignoreSameSpeciesCheckbox = CheckboxWidget.builder(Text.literal("Same-species alliances (ignoreSameSpecies)"), this.textRenderer)
            .pos(this.width / 2 - 100, y)
            .checked(config.ignoreSameSpecies)
            .build();
        this.addDrawableChild(ignoreSameSpeciesCheckbox);
        y += 24;

        targetPlayersCheckbox = CheckboxWidget.builder(Text.literal("Player immunity (targetPlayers)"), this.textRenderer)
            .pos(this.width / 2 - 100, y)
            .checked(config.targetPlayers)
            .build();
        this.addDrawableChild(targetPlayersCheckbox);
        y += 24;

        neutralMobsAlwaysAggressiveCheckbox = CheckboxWidget.builder(Text.literal("Neutral mobs always hostile (neutralMobsAlwaysAggressive)"), this.textRenderer)
            .pos(this.width / 2 - 100, y)
            .checked(config.neutralMobsAlwaysAggressive)
            .build();
        this.addDrawableChild(neutralMobsAlwaysAggressiveCheckbox);
        y += 24;

        allianceSystemEnabledCheckbox = CheckboxWidget.builder(Text.literal("Enable alliance system (allianceSystemEnabled)"), this.textRenderer)
            .pos(this.width / 2 - 100, y)
            .checked(config.allianceSystemEnabled)
            .build();
        this.addDrawableChild(allianceSystemEnabledCheckbox);
        y += 24;

        evolutionSystemEnabledCheckbox = CheckboxWidget.builder(Text.literal("Enable mob leveling (evolutionSystemEnabled)"), this.textRenderer)
            .pos(this.width / 2 - 100, y)
            .checked(config.evolutionSystemEnabled)
            .build();
        this.addDrawableChild(evolutionSystemEnabledCheckbox);
        y += 24;

        rangeMultiplierSlider = new RangeSlider(this.width / 2 - 100, y, 200, 20, config.rangeMultiplier);
        this.addDrawableChild(rangeMultiplierSlider);
        y += 28;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> {
            config.modEnabled = enabledCheckbox.isChecked();
            config.scalingEnabled = scalingCheckbox.isChecked();
            config.ignoreSameSpecies = ignoreSameSpeciesCheckbox.isChecked();
            config.targetPlayers = targetPlayersCheckbox.isChecked();
            config.neutralMobsAlwaysAggressive = neutralMobsAlwaysAggressiveCheckbox.isChecked();
            config.allianceSystemEnabled = allianceSystemEnabledCheckbox.isChecked();
            config.evolutionSystemEnabled = evolutionSystemEnabledCheckbox.isChecked();
            config.rangeMultiplier = rangeMultiplierSlider.getValue();
            config.save();
            this.client.setScreen(parent);
        }).position(this.width / 2 - 100, y).size(200, 20).build());
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
