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
    private ButtonWidget btnDebug;
    
    private Category currentCategory = Category.GENERAL;
    private final List<ClickableWidget> activeWidgets = new ArrayList<>();
    private final List<WidgetPlacement> widgetPlacements = new ArrayList<>();
    private Text categoryDescription = Text.empty();
    private double scrollOffset = 0.0;
    private int contentHeight = 0;

    private static final int CONTENT_TOP = 110;
    private static final int CONTENT_BOTTOM_PADDING = 80;
    private static final int CONTENT_WIDTH = 260;
    private static final int ROW_GAP = 28;
    private static final int WIDGET_HEIGHT = 20;

    private enum Category {
        GENERAL, TARGETING, SCALING, PERFORMANCE, VISUALS, DEBUG
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
        int buttonCount = 6;
        int totalWidth = buttonWidth * buttonCount + spacing * (buttonCount - 1);
        int startX = (this.width - totalWidth) / 2;

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
        btnDebug = ButtonWidget.builder(Text.literal("Debug"), button -> setCategory(Category.DEBUG))
            .dimensions(startX + (buttonWidth + spacing) * 5, y, buttonWidth, 20).build();

        this.addDrawableChild(btnGeneral);
        this.addDrawableChild(btnTargeting);
        this.addDrawableChild(btnScaling);
        this.addDrawableChild(btnPerformance);
        this.addDrawableChild(btnVisuals);
        this.addDrawableChild(btnDebug);
        
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
        widgetPlacements.clear();
        scrollOffset = 0.0;
        contentHeight = 0;

        categoryDescription = Text.empty();

        // Update button states
        btnGeneral.active = currentCategory != Category.GENERAL;
        btnTargeting.active = currentCategory != Category.TARGETING;
        btnScaling.active = currentCategory != Category.SCALING;
        btnPerformance.active = currentCategory != Category.PERFORMANCE;
        btnVisuals.active = currentCategory != Category.VISUALS;
        btnDebug.active = currentCategory != Category.DEBUG;
        int relativeY = 0;
        int x = this.width / 2 - CONTENT_WIDTH / 2;
        int w = CONTENT_WIDTH;
        int h = WIDGET_HEIGHT;

        switch (currentCategory) {
            case GENERAL:
            categoryDescription = Text.literal("Master toggles for the big Universal Mob War systems.");
            relativeY = addCheckbox(x, relativeY, "Enable Mod", config.modEnabled, val -> config.modEnabled = val,
                "Master switch for every system in Universal Mob War.");
            relativeY = addCheckbox(x, relativeY, "Targeting System", config.targetingEnabled, val -> config.targetingEnabled = val,
                "Controls mob-vs-mob combat behavior.");
            relativeY = addCheckbox(x, relativeY, "Alliance System", config.allianceEnabled, val -> {
                config.allianceEnabled = val;
                config.allianceSystemEnabled = val;
            }, "Let mobs form temporary alliances.");
            relativeY = addCheckbox(x, relativeY, "Scaling System", config.scalingEnabled, val -> {
                config.scalingEnabled = val;
                config.evolutionSystemEnabled = val;
            }, "Allow mobs to earn and spend upgrade points.");
            relativeY = addCheckbox(x, relativeY, "Warlord System", config.warlordEnabled, val -> {
                config.warlordEnabled = val;
                config.enableMobWarlord = val;
            }, "Enable the Mob Warlord raid boss feature.");
            relativeY = addSlider(x, relativeY, w, h, config.warlordSpawnChance, 0, 100,
                val -> String.format("Warlord Spawn Chance: %.0f%%", val),
                val -> config.warlordSpawnChance = (int)MathHelper.clamp(Math.round(val), 0, 100));
            break;

            case TARGETING:
            categoryDescription = Text.literal("Fine-tune how mobs pick fights and how often the AI refreshes targets.");
            relativeY = addCheckbox(x, relativeY, "Target Players", config.targetPlayers, val -> config.targetPlayers = val,
                "If disabled, mobs focus entirely on other mobs.");
            relativeY = addCheckbox(x, relativeY, "Ignore Same Species", config.ignoreSameSpecies, val -> config.ignoreSameSpecies = val,
                "Prevents zombies from fighting other zombies, etc.");
            relativeY = addCheckbox(x, relativeY, "Neutral Mobs Always Aggressive", config.neutralMobsAlwaysAggressive,
                val -> config.neutralMobsAlwaysAggressive = val,
                "Forces golems, wolves, and other neutrals into the war.");
            relativeY = addCheckbox(x, relativeY, "Disable Natural Spawns", config.disableNaturalMobSpawns,
                val -> config.disableNaturalMobSpawns = val,
                "Completely blocks passive and hostile natural spawns.");
            relativeY = addSlider(x, relativeY, w, h, config.getRangeMultiplier(), 0.1, 5.0,
                val -> String.format("Detection Range: %.1fx", val),
                val -> {
                    config.rangeMultiplierPercent = (int) Math.round(val * 100.0);
                    config.rangeMultiplier = val;
                });
            relativeY = addSlider(x, relativeY, w, h, config.targetingCacheMs, 500, 5000,
                val -> String.format("Target Cache: %.0f ms", val),
                val -> config.targetingCacheMs = (int) MathHelper.clamp(Math.round(val), 500, 5000));
            relativeY = addSlider(x, relativeY, w, h, config.targetingMaxQueriesPerTick, 10, 200,
                val -> String.format("Queries / Tick: %.0f", val),
                val -> config.targetingMaxQueriesPerTick = (int) MathHelper.clamp(Math.round(val), 10, 200));
            break;

            case SCALING:
            categoryDescription = Text.literal("Make mobs scale with world age, kills, and special rules.");
            relativeY = addCheckbox(x, relativeY, "Allow Boss Scaling", config.allowBossScaling, val -> config.allowBossScaling = val,
                "Bosses obey the same progression rules.");
            relativeY = addSlider(x, relativeY, w, h, config.getDayScalingMultiplier(), 0.0, 10.0,
                val -> String.format("Day Multiplier: %.2fx", val),
                val -> {
                    config.dayScalingMultiplierPercent = (int) Math.round(val * 100.0);
                    config.dayScalingMultiplier = val;
                });
            relativeY = addSlider(x, relativeY, w, h, config.getKillScalingMultiplier(), 0.0, 10.0,
                val -> String.format("Kill Multiplier: %.2fx", val),
                val -> {
                    config.killScalingMultiplierPercent = (int) Math.round(val * 100.0);
                    config.killScalingMultiplier = val;
                });
            relativeY = addSlider(x, relativeY, w, h, config.saveChancePercent, 0, 100,
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
            relativeY = addSlider(x, relativeY, w, h, windowSeconds, 1.0, 30.0,
                val -> String.format("Upgrade Window: %.1fs", val),
                val -> config.upgradeProcessingTimeMs = (int) MathHelper.clamp(Math.round(val * 1000.0), 1000, 30000));
            break;

            case VISUALS:
            categoryDescription = Text.literal("Client-side overlays that make the mob war readable.");
            relativeY = addCheckbox(x, relativeY, "Disable Particles", config.disableParticles, val -> config.disableParticles = val,
                "Removes most particles for better FPS.");
            relativeY = addCheckbox(x, relativeY, "Show Target Lines", config.showTargetLines, val -> config.showTargetLines = val,
                "Draw lines between mobs and their targets.");
            relativeY = addCheckbox(x, relativeY, "Level Up Particles", config.showLevelParticles, val -> config.showLevelParticles = val,
                "Play particles when mobs level up.");
            relativeY = addSlider(x, relativeY, w, h, config.minFpsForVisuals, 0, 144,
                val -> String.format("Minimum FPS: %.0f", val),
                val -> config.minFpsForVisuals = (int) MathHelper.clamp(Math.round(val), 0, 144));
            break;

            case DEBUG:
            categoryDescription = Text.literal("Advanced tuning knobs, logging, and all the extras so you never edit JSON.");
            relativeY = addCheckbox(x, relativeY, "Performance Mode", config.performanceMode, val -> config.performanceMode = val,
                "Cuts visuals and batching for weaker machines.");
            relativeY = addCheckbox(x, relativeY, "Enable Batching", config.enableBatching, val -> config.enableBatching = val,
                "Batch upgrade tasks to reduce tick spikes.");
            relativeY = addCheckbox(x, relativeY, "Enable Async Tasks", config.enableAsyncTasks, val -> config.enableAsyncTasks = val,
                "Move heavy calculations off the main thread.");
            relativeY = addSlider(x, relativeY, w, h, config.mobDataSaveDebounceMs, 50, 1000,
                val -> String.format("Mob Save Debounce: %.0f ms", val),
                val -> config.mobDataSaveDebounceMs = (int) MathHelper.clamp(Math.round(val), 50, 1000));

            double weakSeconds = config.weakAllianceDurationMs / 1000.0;
            relativeY = addSlider(x, relativeY, w, h, weakSeconds, 1.0, 60.0,
                val -> String.format("Weak Alliance Duration: %.1f s", val),
                val -> config.weakAllianceDurationMs = (int) MathHelper.clamp(Math.round(val * 1000.0), 1000, 60000));
            double strongSeconds = config.strongAllianceDurationMs / 1000.0;
            relativeY = addSlider(x, relativeY, w, h, strongSeconds, 1.0, 120.0,
                val -> String.format("Strong Alliance Duration: %.1f s", val),
                val -> config.strongAllianceDurationMs = (int) MathHelper.clamp(Math.round(val * 1000.0), 1000, 120000));
            relativeY = addSlider(x, relativeY, w, h, config.allianceBreakChancePercent, 0, 100,
                val -> String.format("Weak Break Chance: %.0f%%", val),
                val -> config.allianceBreakChancePercent = (int) MathHelper.clamp(Math.round(val), 0, 100));
            relativeY = addSlider(x, relativeY, w, h, config.strongAllianceBreakChancePercent, 0, 100,
                val -> String.format("Strong Break Chance: %.0f%%", val),
                val -> config.strongAllianceBreakChancePercent = (int) MathHelper.clamp(Math.round(val), 0, 100));

            relativeY = addCheckbox(x, relativeY, "Always Spawn Warlord", config.alwaysSpawnWarlordOnFinalWave,
                val -> config.alwaysSpawnWarlordOnFinalWave = val,
                "Forces every final raid wave to include the Warlord.");
            relativeY = addSlider(x, relativeY, w, h, config.warlordMinRaidLevel, 1, 20,
                val -> String.format("Min Raid Level: %.0f", val),
                val -> config.warlordMinRaidLevel = (int) MathHelper.clamp(Math.round(val), 1, 20));
            relativeY = addSlider(x, relativeY, w, h, config.warlordMinionCount, 5, 100,
                val -> String.format("Minion Count: %.0f", val),
                val -> config.warlordMinionCount = (int) MathHelper.clamp(Math.round(val), 5, 100));
            double healthMultiplier = config.warlordHealthMultiplierPercent / 100.0;
            relativeY = addSlider(x, relativeY, w, h, healthMultiplier, 1.0, 10.0,
                val -> String.format("Health Multiplier: %.1fx", val),
                val -> {
                    int percent = (int) MathHelper.clamp(Math.round(val * 100.0), 100, 1000);
                    config.warlordHealthMultiplierPercent = percent;
                    config.warlordHealthMultiplier = val;
                });
            double damageMultiplier = config.warlordDamageMultiplierPercent / 100.0;
            relativeY = addSlider(x, relativeY, w, h, damageMultiplier, 1.0, 10.0,
                val -> String.format("Damage Multiplier: %.1fx", val),
                val -> {
                    int percent = (int) MathHelper.clamp(Math.round(val * 100.0), 100, 1000);
                    config.warlordDamageMultiplierPercent = percent;
                    config.warlordDamageMultiplier = val;
                });

            relativeY = addSlider(x, relativeY, w, h, config.maxParticlesPerConnection, 1, 50,
                val -> String.format("Particles / Connection: %.0f", val),
                val -> config.maxParticlesPerConnection = (int) MathHelper.clamp(Math.round(val), 1, 50));
            relativeY = addSlider(x, relativeY, w, h, config.maxDrawnMinionConnections, 1, 100,
                val -> String.format("Minion Connections Drawn: %.0f", val),
                val -> config.maxDrawnMinionConnections = (int) MathHelper.clamp(Math.round(val), 1, 100));

            relativeY = addCheckbox(x, relativeY, "Upgrade Chat Log", config.debugUpgradeLog, val -> config.debugUpgradeLog = val,
                "Print every upgrade decision in chat while testing.");
            relativeY = addCheckbox(x, relativeY, "Verbose Console Logging", config.debugLogging, val -> config.debugLogging = val,
                "Spam-level logs for profiling and bug reports. Only toggle when needed.");
            break;
        }

        if (widgetPlacements.isEmpty()) {
            contentHeight = 0;
        }
        updateWidgetPositions();
    }

    private int addCheckbox(int x, int relativeY, String text, boolean checked, Consumer<Boolean> onSave, String tooltip) {
        CheckboxWidget widget = CheckboxWidget.builder(Text.literal(text), this.textRenderer)
            .pos(x, CONTENT_TOP + relativeY)
            .checked(checked)
            .callback((checkbox, isChecked) -> onSave.accept(isChecked))
            .tooltip(Tooltip.of(Text.literal(tooltip)))
            .build();
        registerScrollableWidget(widget, relativeY);
        return relativeY + ROW_GAP;
    }

    private int addSlider(int x, int relativeY, int w, int h, double value, double min, double max,
                   Function<Double, String> labelFormatter, Consumer<Double> onSave) {
        double clampedValue = MathHelper.clamp(value, min, max);
        SliderWidget widget = new SliderWidget(x, CONTENT_TOP + relativeY, w, h,
                Text.literal(labelFormatter.apply(clampedValue)),
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
        registerScrollableWidget(widget, relativeY);
        return relativeY + ROW_GAP;
    }

    private void registerScrollableWidget(ClickableWidget widget, int relativeY) {
        this.addDrawableChild(widget);
        activeWidgets.add(widget);
        widgetPlacements.add(new WidgetPlacement(widget, relativeY));
        updateContentHeight(relativeY);
    }

    private void updateContentHeight(int relativeY) {
        contentHeight = Math.max(contentHeight, relativeY + WIDGET_HEIGHT);
    }

    private void updateWidgetPositions() {
        int x = this.width / 2 - CONTENT_WIDTH / 2;
        int visibleHeight = getVisibleHeight();
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
        for (WidgetPlacement placement : widgetPlacements) {
            ClickableWidget widget = placement.widget;
            int widgetY = CONTENT_TOP + placement.relativeY - (int) scrollOffset;
            widget.setX(x);
            widget.setY(widgetY);
            if (widget instanceof SliderWidget slider) {
                slider.setWidth(CONTENT_WIDTH);
            }
            widget.visible = widgetY + WIDGET_HEIGHT >= CONTENT_TOP - WIDGET_HEIGHT && widgetY <= CONTENT_TOP + visibleHeight;
        }
    }

    private int getVisibleHeight() {
        return Math.max(40, this.height - CONTENT_TOP - CONTENT_BOTTOM_PADDING);
    }

    private boolean isInContentArea(double mouseX, double mouseY) {
        int x = this.width / 2 - CONTENT_WIDTH / 2;
        return mouseX >= x && mouseX <= x + CONTENT_WIDTH + 10 && mouseY >= CONTENT_TOP - 10 && mouseY <= this.height - CONTENT_BOTTOM_PADDING + 10;
    }

    private void scroll(double amount) {
        int visibleHeight = getVisibleHeight();
        if (contentHeight <= visibleHeight) {
            return;
        }
        double delta = amount * 15;
        scrollOffset = MathHelper.clamp(scrollOffset - delta, 0, contentHeight - visibleHeight);
        updateWidgetPositions();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isInContentArea(mouseX, mouseY) && contentHeight > getVisibleHeight()) {
            scroll(verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int contentLeft = this.width / 2 - CONTENT_WIDTH / 2;
        int visibleHeight = getVisibleHeight();

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Category: " + currentCategory.name()), this.width / 2, 60, 0xAAAAAA);
        if (!categoryDescription.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, categoryDescription, this.width / 2, 78, 0x77C0FF);
        }

        context.fill(contentLeft - 6, CONTENT_TOP - 10, contentLeft + CONTENT_WIDTH + 6, CONTENT_TOP + visibleHeight + 10, 0x44000000);
        super.render(context, mouseX, mouseY, delta);

        if (contentHeight > visibleHeight) {
            int scrollbarX0 = contentLeft + CONTENT_WIDTH + 2;
            int scrollbarX1 = scrollbarX0 + 4;
            int scrollbarY0 = CONTENT_TOP;
            int scrollbarY1 = CONTENT_TOP + visibleHeight;
            context.fill(scrollbarX0, scrollbarY0, scrollbarX1, scrollbarY1, 0x22000000);
            int thumbHeight = Math.max(12, (int) ((double) visibleHeight * visibleHeight / contentHeight));
            int maxThumbTravel = visibleHeight - thumbHeight;
            int thumbY = scrollbarY0 + (int) ((scrollOffset / (contentHeight - visibleHeight)) * maxThumbTravel);
            context.fill(scrollbarX0, thumbY, scrollbarX1, thumbY + thumbHeight, 0xFF77C0FF);
        }
    }

    private static class WidgetPlacement {
        private final ClickableWidget widget;
        private final int relativeY;

        private WidgetPlacement(ClickableWidget widget, int relativeY) {
            this.widget = widget;
            this.relativeY = relativeY;
        }
    }
}
