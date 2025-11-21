                    private double scrollOffset = 0.0;

                    public ScrollablePanel(int x, int y, int width, int height) {
                        super(x, y, width, height, Text.literal("Config Panel"));
                    }

                    @Override
                    protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
                        ModConfig config = ModConfig.getInstance();
                        int y = 10;
                        int x = 20;
                        int scrollY = (int)scrollOffset;
                        // --- Core Toggles ---
                        CheckboxWidget enabledCheckbox = CheckboxWidget.builder(Text.literal("Enable Universal Mob War"), textRenderer)
                            .pos(x, y - scrollY).checked(config.modEnabled).build();
                        enabledCheckbox.render(context, mouseX, mouseY, delta); y += 24;
                        CheckboxWidget scalingCheckbox = CheckboxWidget.builder(Text.literal("Enable Global Mob Scaling System"), textRenderer)
                            .pos(x, y - scrollY).checked(config.scalingEnabled).build();
                        scalingCheckbox.render(context, mouseX, mouseY, delta); y += 24;
                        CheckboxWidget ignoreSameSpeciesCheckbox = CheckboxWidget.builder(Text.literal("Same-species alliances (ignoreSameSpecies)"), textRenderer)
                            .pos(x, y - scrollY).checked(config.ignoreSameSpecies).build();
                        ignoreSameSpeciesCheckbox.render(context, mouseX, mouseY, delta); y += 24;
                        CheckboxWidget targetPlayersCheckbox = CheckboxWidget.builder(Text.literal("Player immunity (targetPlayers)"), textRenderer)
                            .pos(x, y - scrollY).checked(config.targetPlayers).build();
                        targetPlayersCheckbox.render(context, mouseX, mouseY, delta); y += 24;
                        CheckboxWidget neutralMobsAlwaysAggressiveCheckbox = CheckboxWidget.builder(Text.literal("Neutral mobs always hostile (neutralMobsAlwaysAggressive)"), textRenderer)
                            .pos(x, y - scrollY).checked(config.neutralMobsAlwaysAggressive).build();
                        neutralMobsAlwaysAggressiveCheckbox.render(context, mouseX, mouseY, delta); y += 24;
                        CheckboxWidget allianceSystemEnabledCheckbox = CheckboxWidget.builder(Text.literal("Enable alliance system (allianceSystemEnabled)"), textRenderer)
                            .pos(x, y - scrollY).checked(config.allianceSystemEnabled).build();
                        allianceSystemEnabledCheckbox.render(context, mouseX, mouseY, delta); y += 24;
                        CheckboxWidget evolutionSystemEnabledCheckbox = CheckboxWidget.builder(Text.literal("Enable mob leveling (evolutionSystemEnabled)"), textRenderer)
                            .pos(x, y - scrollY).checked(config.evolutionSystemEnabled).build();
                        evolutionSystemEnabledCheckbox.render(context, mouseX, mouseY, delta); y += 24;
                        RangeSlider rangeMultiplierSlider = new RangeSlider(x, y - scrollY, 200, 20, config.rangeMultiplier);
                        rangeMultiplierSlider.render(context, mouseX, mouseY, delta); y += 28;
                        // ...repeat for all other widgets, using .render() and incrementing y...
                        // At the end, update totalContentHeight for scrolling
                        totalContentHeight = y;
                    }

                    @Override
                    protected int getContentsHeight() {
                        return totalContentHeight;
                    }

                    @Override
                    protected double getDeltaYPerScroll() {
                        return 12.0;
                    }

                    @Override
                    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
                        double maxScroll = Math.max(0, totalContentHeight - this.height);
                        scrollOffset = Math.max(0, Math.min(scrollOffset - amount * getDeltaYPerScroll(), maxScroll));
                        return true;
                    }
            // Stub for missing NarrationMessageBuilder class
            public static class NarrationMessageBuilder {}

            @Override
            public void appendClickableNarrations(NarrationMessageBuilder builder) {
                // No-op stub for accessibility narration
            }
    // Stub for missing NarrationMessageBuilder class
    private static class NarrationMessageBuilder {}
        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {
            // No-op stub for accessibility narration
        }
package mod.universalmobwar.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.text.Text;
import mod.universalmobwar.config.ModConfig;


import net.minecraft.client.gui.widget.SliderWidget;


public class UniversalMobWarConfigScreen extends Screen {
    private ScrollablePanel scrollablePanel;
    private int totalContentHeight = 0;

    public UniversalMobWarConfigScreen(Screen parent) {
        super(Text.literal("Universal Mob War Config"));
    }

    @Override

    protected void init() {
        int panelWidth = 240;
        int panelHeight = this.height - 40;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 30;
        if (scrollablePanel != null) this.remove(scrollablePanel);
        scrollablePanel = new ScrollablePanel(panelX, panelY, panelWidth, panelHeight);
        this.addDrawableChild(scrollablePanel);
    }

    // Custom scrollable panel implementation
    private class ScrollablePanel extends ScrollableWidget {
        public ScrollablePanel(int x, int y, int width, int height) {
            super(x, y, width, height, Text.literal("Config Panel"));
        }

        protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
            ModConfig config = ModConfig.getInstance();
            int y = 10;
            int x = 20;
            int scrollY = (int)getScrollY();
            // --- Core Toggles ---
            CheckboxWidget enabledCheckbox = CheckboxWidget.builder(Text.literal("Enable Universal Mob War"), textRenderer)
                .pos(x, y - scrollY).checked(config.modEnabled).build();
            enabledCheckbox.render(context, mouseX, mouseY, delta); y += 24;
            CheckboxWidget scalingCheckbox = CheckboxWidget.builder(Text.literal("Enable Global Mob Scaling System"), textRenderer)
                .pos(x, y - scrollY).checked(config.scalingEnabled).build();
            scalingCheckbox.render(context, mouseX, mouseY, delta); y += 24;
            CheckboxWidget ignoreSameSpeciesCheckbox = CheckboxWidget.builder(Text.literal("Same-species alliances (ignoreSameSpecies)"), textRenderer)
                .pos(x, y - scrollY).checked(config.ignoreSameSpecies).build();
            ignoreSameSpeciesCheckbox.render(context, mouseX, mouseY, delta); y += 24;
            CheckboxWidget targetPlayersCheckbox = CheckboxWidget.builder(Text.literal("Player immunity (targetPlayers)"), textRenderer)
                .pos(x, y - scrollY).checked(config.targetPlayers).build();
            targetPlayersCheckbox.render(context, mouseX, mouseY, delta); y += 24;
            CheckboxWidget neutralMobsAlwaysAggressiveCheckbox = CheckboxWidget.builder(Text.literal("Neutral mobs always hostile (neutralMobsAlwaysAggressive)"), textRenderer)
                .pos(x, y - scrollY).checked(config.neutralMobsAlwaysAggressive).build();
            neutralMobsAlwaysAggressiveCheckbox.render(context, mouseX, mouseY, delta); y += 24;
            CheckboxWidget allianceSystemEnabledCheckbox = CheckboxWidget.builder(Text.literal("Enable alliance system (allianceSystemEnabled)"), textRenderer)
                .pos(x, y - scrollY).checked(config.allianceSystemEnabled).build();
            allianceSystemEnabledCheckbox.render(context, mouseX, mouseY, delta); y += 24;
            CheckboxWidget evolutionSystemEnabledCheckbox = CheckboxWidget.builder(Text.literal("Enable mob leveling (evolutionSystemEnabled)"), textRenderer)
                .pos(x, y - scrollY).checked(config.evolutionSystemEnabled).build();
            evolutionSystemEnabledCheckbox.render(context, mouseX, mouseY, delta); y += 24;
            RangeSlider rangeMultiplierSlider = new RangeSlider(x, y - scrollY, 200, 20, config.rangeMultiplier);
            rangeMultiplierSlider.render(context, mouseX, mouseY, delta); y += 28;
            // ...repeat for all other widgets, using .render() and incrementing y...
            // At the end, update totalContentHeight for scrolling
            totalContentHeight = y;
        }

        protected int getContentsHeight() {
            return totalContentHeight;
        }

        protected double getDeltaYPerScroll() {
            return 12.0;
        }

    }
    // ...existing code...
    // Place these inner classes at the end of the main class, before the closing brace
    // ...existing code...

    // Integer slider widget for int config values

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
