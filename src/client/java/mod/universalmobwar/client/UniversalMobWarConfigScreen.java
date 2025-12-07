package mod.universalmobwar.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import mod.universalmobwar.config.ModConfig;
import net.minecraft.client.gui.tooltip.Tooltip;

import java.util.ArrayList;
import java.util.List;

public class UniversalMobWarConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig config;
    
    // Category buttons
    private ButtonWidget btnGeneral;
    private ButtonWidget btnScaling;
    private ButtonWidget btnPerformance;
    private ButtonWidget btnVisuals;
    
    private Category currentCategory = Category.GENERAL;
    private final List<Object> activeWidgets = new ArrayList<>();

    private enum Category {
        GENERAL, SCALING, PERFORMANCE, VISUALS
    }

    public UniversalMobWarConfigScreen(Screen parent) {
        super(Text.literal("Universal Mob War Config"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }

    @Override
    protected void init() {
        int y = 40;
        int buttonWidth = 70;
        int spacing = 5;
        int startX = (this.width - (buttonWidth * 4 + spacing * 3)) / 2;

        // Category Buttons
        btnGeneral = ButtonWidget.builder(Text.literal("General"), button -> setCategory(Category.GENERAL))
                .dimensions(startX, y, buttonWidth, 20).build();
        
        btnScaling = ButtonWidget.builder(Text.literal("Scaling"), button -> setCategory(Category.SCALING))
                .dimensions(startX + buttonWidth + spacing, y, buttonWidth, 20).build();
        
        btnPerformance = ButtonWidget.builder(Text.literal("Performance"), button -> setCategory(Category.PERFORMANCE))
                .dimensions(startX + (buttonWidth + spacing) * 2, y, buttonWidth, 20).build();
        
        btnVisuals = ButtonWidget.builder(Text.literal("Visuals"), button -> setCategory(Category.VISUALS))
                .dimensions(startX + (buttonWidth + spacing) * 3, y, buttonWidth, 20).build();

        this.addDrawableChild(btnGeneral);
        this.addDrawableChild(btnScaling);
        this.addDrawableChild(btnPerformance);
        this.addDrawableChild(btnVisuals);
        
        // Save & Exit Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Exit"), button -> {
            config.save();
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build());

        refreshWidgets();
    }

    private void setCategory(Category category) {
        this.currentCategory = category;
        refreshWidgets();
    }

    private void refreshWidgets() {
        // Remove old widgets
        for (Object widget : activeWidgets) {
            if (widget instanceof net.minecraft.client.gui.Drawable) {
                this.remove((net.minecraft.client.gui.Element) widget);
            }
        }
        activeWidgets.clear();

        // Update button states
        btnGeneral.active = currentCategory != Category.GENERAL;
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
                addCheckbox(x, y, "Enable Mod", config.modEnabled, val -> config.modEnabled = val, "Master switch for the entire mod.");
                y += gap;
                addCheckbox(x, y, "Enable Evolution System", config.evolutionSystemEnabled, val -> config.evolutionSystemEnabled = val, "Mobs gain levels, equipment, and skills based on kills and time.");
                y += gap;
                addCheckbox(x, y, "Enable Alliances", config.allianceSystemEnabled, val -> config.allianceSystemEnabled = val, "Mobs form temporary alliances.");
                y += gap;
                addCheckbox(x, y, "Ignore Same Species", config.ignoreSameSpecies, val -> config.ignoreSameSpecies = val, "If true, zombies won't attack zombies.");
                y += gap;
                addCheckbox(x, y, "Target Players", config.targetPlayers, val -> config.targetPlayers = val, "If false, mobs ignore players (spectator mode).");
                y += gap;
                addCheckbox(x, y, "Neutral Mobs Hostile", config.neutralMobsAlwaysAggressive, val -> config.neutralMobsAlwaysAggressive = val, "Make iron golems, wolves, etc. always aggressive.");
                y += gap;
                addCheckbox(x, y, "Disable Natural Spawns", config.disableNaturalMobSpawns, val -> config.disableNaturalMobSpawns = val, "Prevents all natural mob spawns.");
                y += gap;
                addSlider(x, y, w, h, "Detection Range: ", config.rangeMultiplier, 0.1, 5.0, val -> config.rangeMultiplier = val);
                break;

            case SCALING:
                addCheckbox(x, y, "Enable Scaling", config.scalingEnabled, val -> config.scalingEnabled = val, "Mobs get stronger over time and with kills.");
                y += gap;
                addCheckbox(x, y, "Boss Scaling", config.allowBossScaling, val -> config.allowBossScaling = val, "Allow bosses to scale.");
                y += gap;
                addCheckbox(x, y, "Modded Scaling", config.allowModdedScaling, val -> config.allowModdedScaling = val, "Allow modded mobs to scale.");
                y += gap;
                addSlider(x, y, w, h, "Day Multiplier: ", config.dayScalingMultiplier, 0.0, 10.0, val -> config.dayScalingMultiplier = val);
                y += gap;
                addSlider(x, y, w, h, "Kill Multiplier: ", config.killScalingMultiplier, 0.0, 10.0, val -> config.killScalingMultiplier = val);
                y += gap;
                addSlider(x, y, w, h, "Max Tier: ", (double)config.maxTier, 1, 100, val -> config.maxTier = val.intValue());
                break;

            case PERFORMANCE:
                addCheckbox(x, y, "Performance Mode", config.performanceMode, val -> config.performanceMode = val, "Optimizes settings for low-end PCs (Recommended).");
                y += gap;
                addCheckbox(x, y, "Enable Batching", config.enableBatching, val -> config.enableBatching = val, "Process mobs in batches to reduce lag.");
                y += gap;
                addCheckbox(x, y, "Async Tasks", config.enableAsyncTasks, val -> config.enableAsyncTasks = val, "Use background threads for heavy calculations.");
                y += gap;
                addCheckbox(x, y, "Debug Logging", config.debugLogging, val -> config.debugLogging = val, "Enable detailed system logging for debugging.");
                y += gap;
                addCheckbox(x, y, "Debug Upgrade Log", config.debugUpgradeLog, val -> config.debugUpgradeLog = val, "Log all mob upgrade decisions to chat.");
                y += gap;
                addSlider(x, y, w, h, "Cache Duration (ms): ", (double)config.targetingCacheMs, 500, 5000, val -> config.targetingCacheMs = val.intValue());
                y += gap;
                addSlider(x, y, w, h, "Max Queries/Tick: ", (double)config.targetingMaxQueriesPerTick, 10, 200, val -> config.targetingMaxQueriesPerTick = val.intValue());
                break;

            case VISUALS:
                addCheckbox(x, y, "Disable Particles", config.disableParticles, val -> config.disableParticles = val, "Removes most particles for better FPS.");
                y += gap;
                addCheckbox(x, y, "Show Target Lines", config.showTargetLines, val -> config.showTargetLines = val, "Draw lines between mobs and their targets.");
                y += gap;
                addCheckbox(x, y, "Show Health Bars", config.showHealthBars, val -> config.showHealthBars = val, "Show health bars above mobs.");
                y += gap;
                addCheckbox(x, y, "Show Mob Labels", config.showMobLabels, val -> config.showMobLabels = val, "Show names and levels.");
                y += gap;
                addCheckbox(x, y, "Level Up Particles", config.showLevelParticles, val -> config.showLevelParticles = val, "Show particles when mobs level up.");
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

    private void addSlider(int x, int y, int w, int h, String prefix, double value, double min, double max, java.util.function.Consumer<Double> onSave) {
        SliderWidget widget = new SliderWidget(x, y, w, h, Text.literal(prefix + String.format("%.2f", value)), (value - min) / (max - min)) {
            @Override
            protected void updateMessage() {
                double val = min + (max - min) * this.value;
                if (max > 10) { // Integer slider
                     setMessage(Text.literal(prefix + (int)val));
                } else {
                     setMessage(Text.literal(prefix + String.format("%.2f", val)));
                }
            }

            @Override
            protected void applyValue() {
                double val = min + (max - min) * this.value;
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
        super.render(context, mouseX, mouseY, delta);
    }
}
