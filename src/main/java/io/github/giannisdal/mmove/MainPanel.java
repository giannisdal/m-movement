package io.github.giannisdal.mmove;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import javax.swing.JPanel;

import static io.github.giannisdal.mmove.UiStyle.FONT;
import static io.github.giannisdal.mmove.UiStyle.createGraphics;
import static io.github.giannisdal.mmove.UiStyle.drawCenteredText;

final class MainPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final int CARD_X = 24;
    private static final int CARD_WIDTH = 312;
    private static final int DISTANCE_Y = 176;
    private static final int INTERVAL_Y = 262;

    private static final Color BG = new Color(0x14161B);
    private static final Color CARD = new Color(0x1E2128);
    private static final Color GREEN = new Color(0x3DDC97);
    private static final Color TEXT = new Color(0xE8EAF0);
    private static final Color MUTED = new Color(0x8A91A3);

    private final transient BufferedImage logo;

    MainPanel(BufferedImage logo) {
        super(null);
        this.logo = logo;
        setPreferredSize(new Dimension(360, 490));

        addSlider(new Slider(1, 400, MMove.distance(), MMove::updateDistance), DISTANCE_Y);
        addSlider(new Slider(1, 120, MMove.intervalSeconds(), MMove::updateInterval), INTERVAL_Y);

        MovementButton button = new MovementButton();
        button.setBounds(CARD_X, 372, CARD_WIDTH, 48);
        add(button);
    }

    private void addSlider(Slider slider, int cardY) {
        slider.setBounds(40, cardY + 40, 280, 26);
        add(slider);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D graphics = createGraphics(g);
        graphics.setColor(BG);
        graphics.fillRect(0, 0, getWidth(), getHeight());
        paintHeader(graphics);
        paintCard(graphics, DISTANCE_Y, "Distance", MMove.distance() + " px");
        paintCard(graphics, INTERVAL_Y, "Interval", MMove.intervalSeconds() + " s");
        paintStatus(graphics);
        graphics.dispose();
    }

    private void paintHeader(Graphics2D graphics) {
        if (logo != null) graphics.drawImage(logo, 132, 24, 96, 96, null);
        graphics.setColor(TEXT);
        graphics.setFont(new Font(FONT, Font.BOLD, 22));
        drawCenteredText(graphics, "mMove", getWidth(), 150);
    }

    private void paintCard(Graphics2D graphics, int y, String title, String value) {
        graphics.setColor(CARD);
        graphics.fillRoundRect(CARD_X, y, CARD_WIDTH, 76, 18, 18);
        graphics.setColor(MUTED);
        graphics.setFont(new Font(FONT, Font.PLAIN, 12));
        graphics.drawString(title, CARD_X + 16, y + 24);

        graphics.setColor(TEXT);
        graphics.setFont(new Font(FONT, Font.BOLD, 13));
        int valueWidth = graphics.getFontMetrics().stringWidth(value);
        graphics.drawString(value, CARD_X + CARD_WIDTH - 16 - valueWidth, y + 24);
    }

    private void paintStatus(Graphics2D graphics) {
        boolean active = MMove.running();
        graphics.setColor(active ? GREEN : MUTED);
        graphics.fillOval(36, 454, 10, 10);
        graphics.setColor(MUTED);
        graphics.setFont(new Font(FONT, Font.PLAIN, 12));

        String lastMove = MMove.lastMove();
        String status = active ? "Running" : "Stopped";
        if (active && lastMove != null) status += " - last move " + lastMove;
        graphics.drawString(status, 54, 464);
    }
}
