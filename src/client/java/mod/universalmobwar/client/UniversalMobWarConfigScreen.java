package mod.universalmobwar.client;

import mod.universalmobwar.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class UniversalMobWarConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig config;
    
    // Category buttons
    private ButtonWidget btnGeneral;
    private ButtonWidget btnTargeting;
    private ButtonWidget btnScaling;
    private ButtonWidget btnPerformance;
    private ButtonWidget btnVisuals;
    
    private Category currentCategory = Category.GENERAL;
    private final List<ClickableWidget> activeWidgets = new ArrayList<>();
    private Text categoryDescription = Text.empty();

    private enum Category {
        GENERAL, TARGETING, SCALING, PERFORMANCE, VISUALS
    }

    public UniversalMobWarConfigScreen(Screen parent) {
        super(Text.literal("Universal Mob War Config"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }

    @Override
    protected void init() {
        int y = 40;
        int buttonWidth = 80;
        int spacing = 4;
        int startX = (this.width - (buttonWidth * 5 + spacing * 4)) / 2;

        // Category Buttons
        btnGeneral = ButtonWidget.builder(Text.literal("General"), button -> setCategory(Category.GENERAL))
                .dimensions(startX, y, buttonWidth, 20).build();
        
        btnTargeting = ButtonWidget.builder(Text.literal("Targeting"), button -> setCategory(Category.TARGETING))
            .dimensions(startX + buttonWidth + spacing, y, buttonWidth, 20).build();
        
        btnScaling = ButtonWidget.builder(Text.literal("Scaling"), button -> setCategory(Category.SCALING))
            .dimensions(startX + (buttonWidth + spacing) * 2, y, buttonWidth, 20).build();
        
        btnPerformance = ButtonWidget.builder(Text.literal("Performance"), button -> setCategory(Category.PERFORMANCE))
            .dimensions(startX + (buttonWidth + spacing) * 3, y, buttonWidth, 20).build();
        
        btnVisuals = ButtonWidget.builder(Text.literal("Visuals"), button -> setCategory(Category.VISUALS))
            .dimensions(startX + (buttonWidth + spacing) * 4, y, buttonWidth, 20).build();

        this.addDrawableChild(btnGeneral);
        this.addDrawableChild(btnTargeting);
        this.addDrawableChild(btnScaling);
        this.addDrawableChild(btnPerformance);
        this.addDrawableChild(btnVisuals);
        
        // Save & Exit Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Exit"), button -> {
            ModConfig.save();
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build());

        refreshWidgets();
    }

    private void setCategory(Category category) {
        this.currentCategory = category;
        refreshWidgets();
    }

    private void refreshWidgets() {
        // Remove old widgets before drawing new ones
        for (ClickableWidget widget : activeWidgets) {
            this.remove(widget);
        }
        activeWidgets.clear();

        categoryDescription = Text.empty();

        // Update button states
        btnGeneral.active = currentCategory != Category.GENERAL;
        btnTargeting.active = currentCategory != Category.TARGETING;
        btnScaling.active = currentCategory != Category.SCALING;
        btnPerformance.active = currentCategory != Category.PERFORMANCE;
        btnVisuals.active = currentCategory != Category.VISUALS;
        int y = 80;
        int x = this.width / 2 - 100;
        int w = 200;
        int h = 20;
        int gap = 24;

        switch (currentCategory) {
            case GENERAL:
            categoryDescription = Text.literal("Master toggles for the big Universal Mob War systems.");
            addCheckbox(x, y, "Enable Mod", config.modEnabled, val -> config.modEnabled = val,
                "Master switch for every system in Universal Mob War.");
            y += gap;
            addCheckbox(x, y, "Targeting System", config.targetingEnabled, val -> config.targetingEnabled = val,
                "Controls mob-vs-mob combat behavior.");
            y += gap;
            addCheckbox(x, y, "Alliance System", config.allianceEnabled, val -> {
                config.allianceEnabled = val;
                config.allianceSystemEnabled = val;
            }, "Let mobs form temporary alliances.");
            y += gap;
            addCheckbox(x, y, "Scaling System", config.scalingEnabled, val -> {
                config.scalingEnabled = val;
                config.evolutionSystemEnabled = val;
            }, "Allow mobs to earn and spend upgrade points.");
            y += gap;
            addCheckbox(x, y, "Warlord System", config.warlordEnabled, val -> {
                config.warlordEnabled = val;
                config.enableMobWarlord = val;
            }, "Enable the Mob Warlord raid boss feature.");
            break;

            case TARGETING:
            categoryDescription = Text.literal("Fine-tune how mobs pick fights and how often the AI refreshes targets.");
            addCheckbox(x, y, "Target Players", config.targetPlayers, val -> config.targetPlayers = val,
                "If disabled, mobs focus entirely on other mobs.");
            y += gap;
            addCheckbox(x, y, "Ignore Same Species", config.ignoreSameSpecies, val -> config.ignoreSameSpecies = val,
                "Prevents zombies from fighting other zombies, etc.");
            y += gap;
            addCheckbox(x, y, "Neutral Mobs Always Aggressive", config.neutralMobsAlwaysAggressive,
                val -> config.neutralMobsAlwaysAggressive = val,
                "Forces golems, wolves, and other neutrals into the war.");
            y += gap;
            addCheckbox(x, y, "Disable Natural Spawns", config.disableNaturalMobSpawns,
                val -> config.disableNaturalMobSpawns = val,
                "Completely blocks passive and hostile natural spawns.");
            y += gap;
            addSlider(x, y, w, h, config.getRangeMultiplier(), 0.1, 5.0,
                val -> String.format("Detection Range: %.1fx", val),
                val -> {
                    config.rangeMultiplierPercent = (int) Math.round(val * 100.0);
                    config.rangeMultiplier = val;
                });
            y += gap;
            addSlider(x, y, w, h, config.targetingCacheMs, 500, 5000,
                val -> String.format("Target Cache: %.0f ms", val),
                val -> config.targetingCacheMs = (int) MathHelper.clamp(Math.round(val), 500, 5000));
            y += gap;
            addSlider(x, y, w, h, config.targetingMaxQueriesPerTick, 10, 200,
                val -> String.format("Queries / Tick: %.0f", val),
                val -> config.targetingMaxQueriesPerTick = (int) MathHelper.clamp(Math.round(val), 10, 200));
            break;

            case SCALING:
            categoryDescription = Text.literal("Make mobs scale with world age, kills, and special rules.");
            addCheckbox(x, y, "Allow Boss Scaling", config.allowBossScaling, val -> config.allowBossScaling = val,
                "Bosses obey the same progression rules.");
            y += gap;
            addSlider(x, y, w, h, config.getDayScalingMultiplier(), 0.0, 10.0,
                val -> String.format("Day Multiplier: %.2fx", val),
                val -> {
                    config.dayScalingMultiplierPercent = (int) Math.round(val * 100.0);
                    config.dayScalingMultiplier = val;
                });
            y += gap;
            addSlider(x, y, w, h, config.getKillScalingMultiplier(), 0.0, 10.0,
                val -> String.format("Kill Multiplier: %.2fx", val),
                val -> {
                    config.killScalingMultiplierPercent = (int) Math.round(val * 100.0);
                    config.killScalingMultiplier = val;
                });
            y += gap;
            addSlider(x, y, w, h, config.saveChancePercent, 0, 100,
                val -> String.format("Save Chance: %.0f%% (Buy %.0f%%)", val, 100 - val),
                val -> {
                    int clamped = (int) MathHelper.clamp(Math.round(val), 0, 100);
                    config.saveChancePercent = clamped;
                    config.buyChancePercent = 100 - clamped;
                });
            break;

            case PERFORMANCE:
            categoryDescription = Text.literal("Keep upgrade processing near the 5 second budget from skilltree.txt.");
            double windowSeconds = config.upgradeProcessingTimeMs / 1000.0;
            addSlider(x, y, w, h, windowSeconds, 1.0, 30.0,
                val -> String.format("Upgrade Window: %.1fs", val),
                val -> config.upgradeProcessingTimeMs = (int) MathHelper.clamp(Math.round(val * 1000.0), 1000, 30000));
            break;

            case VISUALS:
            categoryDescription = Text.literal("Client-side overlays that make the mob war readable.");
            addCheckbox(x, y, "Disable Particles", config.disableParticles, val -> config.disableParticles = val,
                "Removes most particles for better FPS.");
            y += gap;
            addCheckbox(x, y, "Show Target Lines", config.showTargetLines, val -> config.showTargetLines = val,
                "Draw lines between mobs and their targets.");
            y += gap;
            addCheckbox(x, y, "Show Health Bars", config.showHealthBars, val -> config.showHealthBars = val,
                "Show health bars above mobs.");
            y += gap;
            addCheckbox(x, y, "Show Mob Labels", config.showMobLabels, val -> config.showMobLabels = val,
                "Show names and levels above mobs.");
            y += gap;
            addCheckbox(x, y, "Level Up Particles", config.showLevelParticles, val -> config.showLevelParticles = val,
                "Play particles when mobs level up.");
            y += gap;
            addSlider(x, y, w, h, config.minFpsForVisuals, 0, 144,
                val -> String.format("Minimum FPS: %.0f", val),
                val -> config.minFpsForVisuals = (int) MathHelper.clamp(Math.round(val), 0, 144));
            break;
        }
    }

        private void addCheckbox(int x, int y, String text, boolean checked, java.util.function.Consumer<Boolean> onSave, String tooltip) {
        CheckboxWidget widget = CheckboxWidget.builder(Text.literal(text), this.textRenderer)
                .pos(x, y)
                .checked(checked)
                .callback((checkbox, isChecked) -> onSave.accept(isChecked))
                .tooltip(Tooltip.of(Text.literal(tooltip)))
                .build();
        this.addDrawableChild(widget);
        activeWidgets.add(widget);
    }

        private void addSlider(int x, int y, int w, int h, double value, double min, double max,
                   Function<Double, String> labelFormatter, Consumer<Double> onSave) {
        double clampedValue = MathHelper.clamp(value, min, max);
        SliderWidget widget = new SliderWidget(x, y, w, h, Text.literal(labelFormatter.apply(clampedValue)),
                (clampedValue - min) / (max - min)) {
            @Override
            protected void updateMessage() {
                double val = MathHelper.lerp(this.value, min, max);
                setMessage(Text.literal(labelFormatter.apply(val)));
            }

            @Override
            protected void applyValue() {
                double val = MathHelper.lerp(this.value, min, max);
                onSave.accept(val);
            }
        };
        this.addDrawableChild(widget);
        activeWidgets.add(widget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Category: " + currentCategory.name()), this.width / 2, 65, 0xAAAAAA);
        if (!categoryDescription.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, categoryDescription, this.width / 2, 78, 0x77C0FF);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
