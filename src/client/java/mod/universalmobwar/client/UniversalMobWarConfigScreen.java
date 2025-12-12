package mod.universalmobwar.client;

import mod.universalmobwar.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class UniversalMobWarConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig config;
    
    private final EnumMap<Category, ButtonWidget> categoryButtons = new EnumMap<>(Category.class);
    private Category currentCategory = Category.GENERAL;
    private final List<ClickableWidget> activeWidgets = new ArrayList<>();
    private final List<WidgetPlacement> widgetPlacements = new ArrayList<>();
    private final List<LabelPlacement> labelPlacements = new ArrayList<>();
    private Text categoryDescription = Text.empty();
    private double scrollOffset = 0.0;
    private int contentHeight = 0;

    private static final int CONTENT_TOP = 110;
    private static final int CONTENT_BOTTOM_PADDING = 80;
    private static final int CONTENT_WIDTH = 280;
    private static final int CONTENT_GAP = 30;
    private static final int ROW_GAP = 28;
    private static final int WIDGET_HEIGHT = 20;
    private static final int LABEL_GAP = 14;
    private static final int NAV_WIDTH = 140;
    private static final int NAV_BUTTON_HEIGHT = 20;
    private static final int NAV_BUTTON_GAP = 6;
    private static final int NAV_TOP = 40;

    private static final Category[] CATEGORY_ORDER = {
        Category.GENERAL,
        Category.TARGETING,
        Category.ALLIANCE,
        Category.SCALING,
        Category.WARLORD,
        Category.PERFORMANCE,
        Category.VISUALS,
        Category.DEBUG
    };

    private enum Category {
        GENERAL("General"),
        TARGETING("Targeting"),
        ALLIANCE("Alliance"),
        SCALING("Scaling"),
        WARLORD("Warlord"),
        PERFORMANCE("Performance"),
        VISUALS("Visuals"),
        DEBUG("Debug");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public Text title() {
            return Text.literal(label);
        }

        public String label() {
            return label;
        }
    }

    private int getLayoutWidth() {
        return NAV_WIDTH + CONTENT_GAP + CONTENT_WIDTH;
    }

    private int getNavLeft() {
        return (this.width - getLayoutWidth()) / 2;
    }

    private int getContentLeft() {
        return getNavLeft() + NAV_WIDTH + CONTENT_GAP;
    }

    public UniversalMobWarConfigScreen(Screen parent) {
        super(Text.literal("Universal Mob War Config"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }

    @Override
    protected void init() {
        this.config = ModConfig.getInstance();

        categoryButtons.values().forEach(this::remove);
        categoryButtons.clear();

        int navLeft = getNavLeft();
        int navY = NAV_TOP;
        for (Category category : CATEGORY_ORDER) {
            ButtonWidget button = ButtonWidget.builder(category.title(), btn -> setCategory(category))
                .dimensions(navLeft, navY, NAV_WIDTH, NAV_BUTTON_HEIGHT)
                .build();
            this.addDrawableChild(button);
            categoryButtons.put(category, button);
            navY += NAV_BUTTON_HEIGHT + NAV_BUTTON_GAP;
        }

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
        labelPlacements.clear();
        scrollOffset = 0.0;
        contentHeight = 0;

        categoryDescription = Text.empty();

        categoryButtons.forEach((category, button) -> button.active = category != currentCategory);

        int relativeY = 0;
        int x = getContentLeft();
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

            case ALLIANCE:
            categoryDescription = Text.literal("Let mobs form temporary truces and tune how long they last.");
            relativeY = addCheckbox(x, relativeY, "Alliance System", config.allianceEnabled, val -> {
                config.allianceEnabled = val;
                config.allianceSystemEnabled = val;
            }, "Let mobs form temporary alliances.");
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
            relativeY = addIntegerField(x, relativeY, w,
                "Manual Day Override",
                config.manualWorldDayOverride,
                -1,
                100000,
                val -> config.manualWorldDayOverride = val,
                "Pretend the world has run for this many days. Set -1 to use the real count.");
            relativeY = addSlider(x, relativeY, w, h, config.saveChancePercent, 0, 100,
                val -> String.format("Save Chance: %.0f%% (Buy %.0f%%)", val, 100 - val),
                val -> {
                    int clamped = (int) MathHelper.clamp(Math.round(val), 0, 100);
                    config.saveChancePercent = clamped;
                    config.buyChancePercent = 100 - clamped;
                });
            double maxIterations = config.getMaxUpgradeIterations();
            relativeY = addSlider(x, relativeY, w, h, maxIterations, ModConfig.MIN_UPGRADE_ITERATIONS, ModConfig.MAX_UPGRADE_ITERATIONS,
                val -> String.format("Max Upgrades / Cycle: %.0f", val),
                val -> config.maxUpgradeIterations = (int) MathHelper.clamp(
                    Math.round(val),
                    ModConfig.MIN_UPGRADE_ITERATIONS,
                    ModConfig.MAX_UPGRADE_ITERATIONS
                ));
            break;

            case WARLORD:
            categoryDescription = Text.literal("Configure when the raid boss appears and how scary it should be.");
            relativeY = addCheckbox(x, relativeY, "Warlord System", config.warlordEnabled, val -> {
                config.warlordEnabled = val;
                config.enableMobWarlord = val;
            }, "Enable the Mob Warlord raid boss feature.");
            relativeY = addCheckbox(x, relativeY, "Always Spawn Final Wave", config.alwaysSpawnWarlordOnFinalWave,
                val -> config.alwaysSpawnWarlordOnFinalWave = val,
                "Forces every final raid wave to include the Warlord.");
            relativeY = addSlider(x, relativeY, w, h, config.warlordSpawnChance, 0, 100,
                val -> String.format("Spawn Chance: %.0f%%", val),
                val -> config.warlordSpawnChance = (int) MathHelper.clamp(Math.round(val), 0, 100));
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
            break;

            case PERFORMANCE:
            categoryDescription = Text.literal("Keep upgrade processing near the 5 second budget from skilltree.txt.");
            relativeY = addCheckbox(x, relativeY, "Performance Mode", config.performanceMode, val -> config.performanceMode = val,
                "Cuts visuals and batching for weaker machines.");
            relativeY = addCheckbox(x, relativeY, "Enable Batching", config.enableBatching, val -> config.enableBatching = val,
                "Batch upgrade tasks to reduce tick spikes.");
            relativeY = addCheckbox(x, relativeY, "Enable Async Tasks", config.enableAsyncTasks, val -> config.enableAsyncTasks = val,
                "Move heavy calculations off the main thread.");
            double windowSeconds = config.upgradeProcessingTimeMs / 1000.0;
            relativeY = addSlider(x, relativeY, w, h, windowSeconds, 1.0, 30.0,
                val -> String.format("Upgrade Window: %.1fs", val),
                val -> config.upgradeProcessingTimeMs = (int) MathHelper.clamp(Math.round(val * 1000.0), 1000, 30000));
            relativeY = addSlider(x, relativeY, w, h, config.mobDataSaveDebounceMs, 50, 1000,
                val -> String.format("Mob Save Debounce: %.0f ms", val),
                val -> config.mobDataSaveDebounceMs = (int) MathHelper.clamp(Math.round(val), 50, 1000));
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
            relativeY = addSlider(x, relativeY, w, h, config.maxParticlesPerConnection, 1, 50,
                val -> String.format("Particles / Connection: %.0f", val),
                val -> config.maxParticlesPerConnection = (int) MathHelper.clamp(Math.round(val), 1, 50));
            relativeY = addSlider(x, relativeY, w, h, config.maxDrawnMinionConnections, 1, 100,
                val -> String.format("Minion Connections Drawn: %.0f", val),
                val -> config.maxDrawnMinionConnections = (int) MathHelper.clamp(Math.round(val), 1, 100));
            break;

            case DEBUG:
            categoryDescription = Text.literal("Advanced logging, chat spam, and anything meant just for testing.");
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

    private int addIntegerField(int x, int relativeY, int w, String label, int initialValue,
                                int minValue, int maxValue, Consumer<Integer> onSave, String tooltip) {
        registerLabel(Text.literal(label), relativeY);
        int fieldRelativeY = relativeY + LABEL_GAP;
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, CONTENT_TOP + fieldRelativeY, w, WIDGET_HEIGHT, Text.literal(label));
        field.setText(String.valueOf(initialValue));
        field.setMaxLength(7);
        field.setPlaceholder(Text.literal("Use -1 to follow real days"));
        field.setTooltip(Tooltip.of(Text.literal(tooltip)));
        field.setTextPredicate(text -> text.isEmpty() || text.equals("-") || text.matches("-?\\d{0,6}"));
        field.setChangedListener(text -> {
            if (text == null || text.isEmpty() || text.equals("-")) {
                return;
            }
            try {
                int parsed = Integer.parseInt(text);
                parsed = MathHelper.clamp(parsed, minValue, maxValue);
                onSave.accept(parsed);
                String clampedText = String.valueOf(parsed);
                if (!clampedText.equals(text)) {
                    field.setText(clampedText);
                }
            } catch (NumberFormatException ignored) {}
        });
        registerScrollableWidget(field, fieldRelativeY);
        return fieldRelativeY + ROW_GAP;
    }

    private void registerScrollableWidget(ClickableWidget widget, int relativeY) {
        this.addDrawableChild(widget);
        activeWidgets.add(widget);
        widgetPlacements.add(new WidgetPlacement(widget, relativeY));
        updateContentHeight(relativeY);
    }

    private void registerLabel(Text text, int relativeY) {
        labelPlacements.add(new LabelPlacement(text, relativeY));
    }

    private void updateContentHeight(int relativeY) {
        contentHeight = Math.max(contentHeight, relativeY + WIDGET_HEIGHT);
    }

    private void updateWidgetPositions() {
        int x = getContentLeft();
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
            } else if (widget instanceof TextFieldWidget field) {
                field.setWidth(CONTENT_WIDTH);
            }
            widget.visible = widgetY + WIDGET_HEIGHT >= CONTENT_TOP - WIDGET_HEIGHT && widgetY <= CONTENT_TOP + visibleHeight;
        }
    }

    private int getVisibleHeight() {
        return Math.max(40, this.height - CONTENT_TOP - CONTENT_BOTTOM_PADDING);
    }

    private boolean isInContentArea(double mouseX, double mouseY) {
        int x = getContentLeft();
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
        int contentLeft = getContentLeft();
        int navLeft = getNavLeft();
        int visibleHeight = getVisibleHeight();

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        int navPanelBottom = CONTENT_TOP + visibleHeight + 10;
        context.fill(navLeft - 6, NAV_TOP - 10, navLeft + NAV_WIDTH + 6, navPanelBottom, 0x33000000);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Sections"), navLeft, NAV_TOP - 16, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, currentCategory.title(), contentLeft, 60, 0xFFFFFF);
        if (!categoryDescription.getString().isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, categoryDescription, contentLeft, 78, 0x77C0FF);
        }

        context.fill(contentLeft - 6, CONTENT_TOP - 10, contentLeft + CONTENT_WIDTH + 6, CONTENT_TOP + visibleHeight + 10, 0x44000000);
        renderLabels(context);
        super.render(context, mouseX, mouseY, delta);

        ButtonWidget selected = categoryButtons.get(currentCategory);
        if (selected != null) {
            context.drawBorder(selected.getX() - 2, selected.getY() - 2, selected.getWidth() + 4,
                selected.getHeight() + 4, 0xFF77C0FF);
        }

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

    private void renderLabels(DrawContext context) {
        if (labelPlacements.isEmpty()) return;
        int x = getContentLeft();
        int visibleHeight = getVisibleHeight();
        for (LabelPlacement label : labelPlacements) {
            int drawY = CONTENT_TOP + label.relativeY - (int) scrollOffset;
            if (drawY < CONTENT_TOP - 12 || drawY > CONTENT_TOP + visibleHeight) {
                continue;
            }
            context.drawTextWithShadow(this.textRenderer, label.text, x, drawY, label.color);
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

    private static class LabelPlacement {
        private final Text text;
        private final int relativeY;
        private final int color;

        private LabelPlacement(Text text, int relativeY) {
            this(text, relativeY, 0xFFC3D5FF);
        }

        private LabelPlacement(Text text, int relativeY, int color) {
            this.text = text;
            this.relativeY = relativeY;
            this.color = color;
        }
    }
}
