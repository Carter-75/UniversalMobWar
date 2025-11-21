package mod.universalmobwar.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import mod.universalmobwar.config.ModConfig;

public class UniversalMobWarConfigScreen extends Screen {
    private ScrollableWidget scrollablePanel;
    private int totalContentHeight = 0;
    private double scrollOffset = 0.0;

    public UniversalMobWarConfigScreen(Screen parent) {
        super(Text.literal("Universal Universal Mob War Config"));
    }

    @Override
    protected void init() {
        int panelWidth = 240;
        int panelHeight = this.height - 40;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 30;

        if (scrollablePanel != null) this.remove(scrollablePanel);

        scrollablePanel = new ScrollableWidget(panelX, panelY, panelWidth, panelHeight, Text.literal("Config Panel")) {

            @Override
            protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
                ModConfig config = ModConfig.getInstance();
                int y = 10;
                int x = 20;
                int s = (int) scrollOffset;

                CheckboxWidget enabled = CheckboxWidget.builder(Text.literal("Enable Universal Mob War"), textRenderer)
                        .pos(x, y - s).checked(config.modEnabled).build();
                enabled.render(context, mouseX, mouseY, delta);
                y += 24;

                CheckboxWidget scaling = CheckboxWidget.builder(Text.literal("Enable Global Mob Scaling System"), textRenderer)
                        .pos(x, y - s).checked(config.scalingEnabled).build();
                scaling.render(context, mouseX, mouseY, delta);
                y += 24;

                CheckboxWidget ignore = CheckboxWidget.builder(Text.literal("Same-species alliances (ignoreSameSpecies)"), textRenderer)
                        .pos(x, y - s).checked(config.ignoreSameSpecies).build();
                ignore.render(context, mouseX, mouseY, delta);
                y += 24;

                CheckboxWidget targetPlayers = CheckboxWidget.builder(Text.literal("Player immunity (targetPlayers)"), textRenderer)
                        .pos(x, y - s).checked(config.targetPlayers).build();
                targetPlayers.render(context, mouseX, mouseY, delta);
                y += 24;

                CheckboxWidget neutral = CheckboxWidget.builder(Text.literal("Neutral mobs always hostile"), textRenderer)
                        .pos(x, y - s).checked(config.neutralMobsAlwaysAggressive).build();
                neutral.render(context, mouseX, mouseY, delta);
                y += 24;

                CheckboxWidget alliance = CheckboxWidget.builder(Text.literal("Enable alliance system"), textRenderer)
                        .pos(x, y - s).checked(config.allianceSystemEnabled).build();
                alliance.render(context, mouseX, mouseY, delta);
                y += 24;

                CheckboxWidget evolution = CheckboxWidget.builder(Text.literal("Enable mob leveling"), textRenderer)
                        .pos(x, y - s).checked(config.evolutionSystemEnabled).build();
                evolution.render(context, mouseX, mouseY, delta);
                y += 24;

                RangeSlider rangeSlider = new RangeSlider(x, y - s, 200, 20, config.rangeMultiplier);
                rangeSlider.render(context, mouseX, mouseY, delta);
                y += 28;

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
            public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
                double max = Math.max(0, totalContentHeight - this.height);
                scrollOffset = Math.max(0, Math.min(scrollOffset - vertical * getDeltaYPerScroll(), max));
                return true;
            }

            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
        };

        this.addDrawableChild(scrollablePanel);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static class RangeSlider extends SliderWidget {
        public RangeSlider(int x, int y, int w, int h, double initialValue) {
            super(x, y, w, h, Text.literal(""), (initialValue - 0.01) / 99.99);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Detection range multiplier: " + String.format("%.2f", getValue())));
        }

        @Override
        protected void applyValue() {}

        public double getValue() {
            return 0.01 + this.value * 99.99;
        }
    }
}
