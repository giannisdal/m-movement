package io.github.giannisdal.mmove;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

final class UiStyle {
    static final Color ACCENT = new Color(0x6C8CFF);
    static final String FONT = "Segoe UI";

    private UiStyle() {
    }

    static Graphics2D createGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    static void drawCenteredText(Graphics2D graphics, String text, int width, int baseline) {
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString(text, (width - textWidth) / 2, baseline);
    }

}
