package io.github.ikiuox.pirouette.gui;

import io.github.ikiuox.pirouette.PirouetteClient;
import io.github.ikiuox.pirouette.config.PirouetteConfig;
import io.github.ikiuox.pirouette.config.SpinProfile;
import io.github.ikiuox.pirouette.core.Direction;
import io.github.ikiuox.pirouette.core.Easing;
import io.github.ikiuox.pirouette.core.RepeatMode;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Settings screen: three tabs, plain buttons and sliders only (no popups, no
 * scrolling), and a live preview that plays the spin you are configuring.
 */
public final class PirouetteConfigScreen extends Screen {
    private enum Tab { SPIN, TURN, GENERAL }

    private static final int COLUMN_WIDTH = 200;
    private static final int GAP = 4;
    private static final int TABS_Y = 24;
    private static final int CONTENT_Y = 52;
    private static final int ROW_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 28;
    private static final double PREVIEW_HOLD_MS = 650.0;
    private static final double FADE_IN_NANOS = 160_000_000.0;

    // Same palette as the mod icon.
    private static final int AMBER = 0xE2A13B;
    private static final int BONE = 0xF1ECE2;
    private static final int RING = 0x5C7488;
    private static final int PANEL = 0x0E141B;
    private static final int BORDER = 0x34465A;
    private static final int GRID = 0x2A3947;

    private final Screen parent;
    private final Tab tab;
    private final long openedAtNanos = System.nanoTime();
    private int contentBottom;

    public PirouetteConfigScreen(Screen parent) {
        this(parent, Tab.SPIN);
    }

    private PirouetteConfigScreen(Screen parent, Tab tab) {
        super(Text.translatable("pirouette.config.title"));
        this.parent = parent;
        this.tab = tab;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        PirouetteConfig config = PirouetteConfig.get();
        int left = this.width / 2 - COLUMN_WIDTH / 2;

        // Tabs
        int tabWidth = (COLUMN_WIDTH - 2 * GAP) / 3;
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            Tab target = tabs[i];
            ButtonWidget tabButton = ButtonWidget.builder(
                    Text.translatable("pirouette.config.tab." + target.name().toLowerCase(Locale.ROOT)),
                    button -> this.client.setScreen(new PirouetteConfigScreen(this.parent, target)))
                .dimensions(left + i * (tabWidth + GAP), TABS_Y, tabWidth, 20)
                .build();
            tabButton.active = target != this.tab;
            this.addDrawableChild(tabButton);
        }

        // Rows for the current tab
        int y = CONTENT_Y;
        switch (this.tab) {
            case SPIN -> y = this.addProfileRows(left, y, config.spin);
            case TURN -> y = this.addProfileRows(left, y, config.turn);
            case GENERAL -> y = this.addGeneralRows(left, y, config);
        }
        this.contentBottom = y;

        // Footer
        int footerY = this.height - FOOTER_HEIGHT;
        int third = (COLUMN_WIDTH - 2 * GAP) / 3;

        ButtonWidget test = ButtonWidget.builder(
                Text.translatable("pirouette.config.test"), button -> this.testSpin())
            .dimensions(left, footerY, third, 20)
            .tooltip(Tooltip.of(Text.translatable("pirouette.tooltip.test")))
            .build();
        test.active = this.client != null && this.client.player != null;
        this.addDrawableChild(test);

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("pirouette.config.reset"), button -> this.resetTab())
            .dimensions(left + third + GAP, footerY, third, 20)
            .tooltip(Tooltip.of(Text.translatable("pirouette.tooltip.reset")))
            .build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("pirouette.config.done"), button -> this.close())
            .dimensions(left + 2 * (third + GAP), footerY, third, 20)
            .build());
    }

    private int addProfileRows(int left, int y, SpinProfile profile) {
        this.addSlider(left, y, 15.0, 3600.0, 15.0, profile.angle,
            v -> Text.translatable("pirouette.config.angle", (int) v),
            v -> profile.angle = (float) v,
            "pirouette.tooltip.angle");
        y += ROW_HEIGHT;

        this.addSlider(left, y, 50.0, 2000.0, 10.0, profile.durationMs,
            v -> Text.translatable("pirouette.config.duration", (int) v),
            v -> profile.durationMs = (int) v,
            "pirouette.tooltip.duration");
        y += ROW_HEIGHT;

        this.addDrawableChild(ButtonWidget.builder(curveLabel(profile.easing), button -> {
                profile.easing = profile.easing.next();
                button.setMessage(curveLabel(profile.easing));
            })
            .dimensions(left, y, COLUMN_WIDTH, 20)
            .tooltip(Tooltip.of(Text.translatable("pirouette.tooltip.curve")))
            .build());
        y += ROW_HEIGHT;

        this.addDrawableChild(ButtonWidget.builder(directionLabel(profile.direction), button -> {
                profile.direction = profile.direction.next();
                button.setMessage(directionLabel(profile.direction));
            })
            .dimensions(left, y, COLUMN_WIDTH, 20)
            .tooltip(Tooltip.of(Text.translatable("pirouette.tooltip.direction")))
            .build());
        y += ROW_HEIGHT;
        return y;
    }

    private int addGeneralRows(int left, int y, PirouetteConfig config) {
        this.addDrawableChild(ButtonWidget.builder(toggleLabel("pirouette.config.enabled", config.enabled), button -> {
                config.enabled = !config.enabled;
                button.setMessage(toggleLabel("pirouette.config.enabled", config.enabled));
            })
            .dimensions(left, y, COLUMN_WIDTH, 20)
            .tooltip(Tooltip.of(Text.translatable("pirouette.tooltip.enabled")))
            .build());
        y += ROW_HEIGHT;

        this.addDrawableChild(ButtonWidget.builder(repeatLabel(config.repeatMode), button -> {
                config.repeatMode = config.repeatMode.next();
                button.setMessage(repeatLabel(config.repeatMode));
            })
            .dimensions(left, y, COLUMN_WIDTH, 20)
            .tooltip(Tooltip.of(Text.translatable("pirouette.tooltip.repeat")))
            .build());
        y += ROW_HEIGHT;

        this.addDrawableChild(ButtonWidget.builder(
                toggleLabel("pirouette.config.cancel_on_move", config.cancelOnMouseMove), button -> {
                config.cancelOnMouseMove = !config.cancelOnMouseMove;
                button.setMessage(toggleLabel("pirouette.config.cancel_on_move", config.cancelOnMouseMove));
            })
            .dimensions(left, y, COLUMN_WIDTH, 20)
            .tooltip(Tooltip.of(Text.translatable("pirouette.tooltip.cancel_on_move")))
            .build());
        y += ROW_HEIGHT;

        this.addSlider(left, y, 0.0, 2000.0, 50.0, config.cooldownMs,
            v -> Text.translatable("pirouette.config.cooldown", (int) v),
            v -> config.cooldownMs = (int) v,
            "pirouette.tooltip.cooldown");
        y += ROW_HEIGHT;
        return y;
    }

    private void addSlider(int x, int y, double min, double max, double step, double current,
                           DoubleFunction<Text> label, DoubleConsumer onChange, String tooltipKey) {
        ValueSlider slider = new ValueSlider(x, y, COLUMN_WIDTH, 20, min, max, step, current, label, onChange);
        slider.setTooltip(Tooltip.of(Text.translatable(tooltipKey)));
        this.addDrawableChild(slider);
    }

    // ------------------------------------------------------------------ labels

    private static Text curveLabel(Easing easing) {
        return Text.translatable("pirouette.config.curve",
            Text.translatable("pirouette.curve." + easing.name().toLowerCase(Locale.ROOT)));
    }

    private static Text directionLabel(Direction direction) {
        return Text.translatable("pirouette.config.direction",
            Text.translatable("pirouette.direction." + direction.name().toLowerCase(Locale.ROOT)));
    }

    private static Text repeatLabel(RepeatMode mode) {
        return Text.translatable("pirouette.config.repeat",
            Text.translatable("pirouette.repeat." + mode.name().toLowerCase(Locale.ROOT)));
    }

    private static Text toggleLabel(String key, boolean on) {
        Text state = Text.translatable(on ? "options.on" : "options.off")
            .formatted(on ? Formatting.GREEN : Formatting.RED);
        return Text.translatable(key, state);
    }

    // ----------------------------------------------------------------- actions

    private void testSpin() {
        MinecraftClient mc = this.client;
        if (mc == null || mc.player == null) {
            return;
        }
        PirouetteConfig config = PirouetteConfig.get();
        SpinProfile profile = this.tab == Tab.TURN ? config.turn : config.spin;
        mc.setScreen(null);
        PirouetteClient.driver().trigger(mc, profile);
    }

    private void resetTab() {
        PirouetteConfig config = PirouetteConfig.get();
        PirouetteConfig defaults = PirouetteConfig.defaults();
        switch (this.tab) {
            case SPIN -> config.spin = defaults.spin;
            case TURN -> config.turn = defaults.turn;
            case GENERAL -> {
                config.enabled = defaults.enabled;
                config.repeatMode = defaults.repeatMode;
                config.cancelOnMouseMove = defaults.cancelOnMouseMove;
                config.cooldownMs = defaults.cooldownMs;
            }
        }
        this.client.setScreen(new PirouetteConfigScreen(this.parent, this.tab));
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    /** Runs whenever the screen goes away (Done, Esc, tab switch, Test). */
    @Override
    public void removed() {
        PirouetteConfig.save();
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);
        if (this.tab != Tab.GENERAL) {
            this.renderPreview(context);
        }
    }

    /** Left: the speed curve with a moving dot. Right: a dial showing the actual turn. */
    private void renderPreview(DrawContext context) {
        int left = this.width / 2 - COLUMN_WIDTH / 2;
        int top = this.contentBottom + 2;
        int available = this.height - FOOTER_HEIGHT - 6 - top;
        if (available < 40) {
            return; // window too small, skip the preview instead of overlapping buttons
        }
        int panelHeight = Math.min(available, 72);
        double fade = Math.min(1.0, (System.nanoTime() - this.openedAtNanos) / FADE_IN_NANOS);

        context.fill(left, top, left + COLUMN_WIDTH, top + panelHeight, argb(0xC0, PANEL, fade));
        drawFrame(context, left, top, COLUMN_WIDTH, panelHeight, argb(0xFF, BORDER, fade));

        PirouetteConfig config = PirouetteConfig.get();
        SpinProfile profile = this.tab == Tab.TURN ? config.turn : config.spin;
        double spinMs = Math.max(1, profile.durationMs);
        double clockMs = (System.nanoTime() / 1_000_000.0) % (spinMs + PREVIEW_HOLD_MS);
        double t = Math.min(1.0, clockMs / spinMs);
        double progress = profile.easing.apply(t);

        // Curve plot
        int plotLeft = left + 10;
        int plotTop = top + 9;
        int plotWidth = 100;
        int plotHeight = panelHeight - 18;
        double vMin = -0.05;
        double vMax = 1.15;
        int zeroY = plotY(0.0, plotTop, plotHeight, vMin, vMax);
        int oneY = plotY(1.0, plotTop, plotHeight, vMin, vMax);
        context.fill(plotLeft, zeroY, plotLeft + plotWidth + 1, zeroY + 1, argb(0xFF, GRID, fade));
        context.fill(plotLeft, oneY, plotLeft + plotWidth + 1, oneY + 1, argb(0xFF, GRID, fade));

        int previousY = zeroY;
        for (int i = 0; i <= plotWidth; i++) {
            double value = profile.easing.apply(i / (double) plotWidth);
            int y = plotY(value, plotTop, plotHeight, vMin, vMax);
            int x = plotLeft + i;
            context.fill(x, Math.min(y, previousY), x + 1, Math.max(y, previousY) + 1, argb(0xFF, AMBER, fade));
            previousY = y;
        }
        int markerX = plotLeft + (int) Math.round(t * plotWidth);
        int markerY = plotY(progress, plotTop, plotHeight, vMin, vMax);
        context.fill(markerX - 2, markerY - 2, markerX + 3, markerY + 3, argb(0xFF, BONE, fade));

        // Dial
        int radius = Math.min(panelHeight / 2 - 8, 30);
        int cx = left + 155;
        int cy = top + panelHeight / 2;
        for (int i = 0; i < 64; i++) {
            double a = i * (Math.PI * 2.0 / 64.0);
            int x = cx + (int) Math.round(Math.sin(a) * radius);
            int y = cy - (int) Math.round(Math.cos(a) * radius);
            context.fill(x, y, x + 1, y + 1, argb(0xFF, RING, fade));
        }
        context.fill(cx, cy - radius - 3, cx + 1, cy - radius - 1, argb(0xFF, BONE, fade));

        double sign = profile.direction == Direction.LEFT ? -1.0 : 1.0;
        double theta = Math.toRadians(sign * profile.angle * progress);
        double dx = Math.sin(theta);
        double dy = -Math.cos(theta);
        int length = radius - 2;
        for (int s = 0; s <= length; s++) {
            int x = cx + (int) Math.round(dx * s);
            int y = cy + (int) Math.round(dy * s);
            context.fill(x, y, x + 1, y + 1, argb(0xFF, BONE, fade));
        }
        int tipX = cx + (int) Math.round(dx * length);
        int tipY = cy + (int) Math.round(dy * length);
        context.fill(tipX - 1, tipY - 1, tipX + 2, tipY + 2, argb(0xFF, AMBER, fade));
        context.fill(cx - 1, cy - 1, cx + 2, cy + 2, argb(0xFF, BONE, fade));
    }

    private static int plotY(double value, int top, int height, double vMin, double vMax) {
        double ratio = (value - vMin) / (vMax - vMin);
        return top + height - (int) Math.round(ratio * height);
    }

    private static double bound(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int argb(int alpha, int rgb, double fade) {
        int a = (int) Math.round(alpha * bound(fade, 0.0, 1.0));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static void drawFrame(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    // ----------------------------------------------------------------- slider

    /** A slider with real units: shows the value, snaps to a step, reports changes live. */
    private static final class ValueSlider extends SliderWidget {
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<Text> label;
        private final DoubleConsumer onChange;

        ValueSlider(int x, int y, int width, int height, double min, double max, double step,
                    double current, DoubleFunction<Text> label, DoubleConsumer onChange) {
            super(x, y, width, height, Text.empty(), normalize(current, min, max));
            this.min = min;
            this.max = max;
            this.step = step;
            this.label = label;
            this.onChange = onChange;
            this.updateMessage();
        }

        private static double normalize(double current, double min, double max) {
            return (PirouetteConfigScreen.bound(current, min, max) - min) / (max - min);
        }

        private double currentValue() {
            double raw = this.min + (this.max - this.min) * this.value;
            double snapped = Math.round(raw / this.step) * this.step;
            return PirouetteConfigScreen.bound(snapped, this.min, this.max);
        }

        @Override
        protected void updateMessage() {
            if (this.label != null) {
                this.setMessage(this.label.apply(this.currentValue()));
            }
        }

        @Override
        protected void applyValue() {
            double v = this.currentValue();
            this.value = (v - this.min) / (this.max - this.min); // snap the handle too
            this.onChange.accept(v);
        }
    }
}
