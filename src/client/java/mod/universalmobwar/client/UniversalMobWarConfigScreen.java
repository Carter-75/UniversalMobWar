package mod.universalmobwar.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import mod.universalmobwar.config.ModConfig;

public class UniversalMobWarConfigScreen extends Screen {
    private final Screen parent;
    private CheckboxWidget enabledCheckbox;
    private CheckboxWidget scalingCheckbox;

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

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> {
            config.modEnabled = enabledCheckbox.isChecked();
            config.scalingEnabled = scalingCheckbox.isChecked();
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
}
