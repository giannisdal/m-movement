package io.github.giannisdal.mmove;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.function.IntConsumer;
import javax.swing.JComponent;

import static io.github.giannisdal.mmove.UiStyle.ACCENT;
import static io.github.giannisdal.mmove.UiStyle.createGraphics;

/** Flat custom slider. */
final class Slider extends JComponent {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final int TRACK_PADDING = 10;
    private static final Color TRACK = new Color(0x333844);

    private final int min;
    private final int max;
    private final transient IntConsumer onChange;
    private int value;

    Slider(int min, int max, int value, IntConsumer onChange) {
        this.min = min;
        this.max = max;
        this.value = value;
        this.onChange = onChange;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        super.processMouseEvent(event);
        if (event.getID() == MouseEvent.MOUSE_PRESSED) updateValue(event.getX());
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent event) {
        super.processMouseMotionEvent(event);
        if (event.getID() == MouseEvent.MOUSE_DRAGGED) updateValue(event.getX());
    }

    private void updateValue(int mouseX) {
        double fraction = (mouseX - TRACK_PADDING) / (double) trackWidth();
        value = Math.clamp(Math.round(min + fraction * (max - min)), min, max);
        onChange.accept(value);
        repaint();
    }

    private int trackWidth() {
        return getWidth() - 2 * TRACK_PADDING;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D graphics = createGraphics(g);
        int centerY = getHeight() / 2;
        int width = trackWidth();
        int thumbX = TRACK_PADDING + (int) ((value - min) / (double) (max - min) * width);

        graphics.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(TRACK);
        graphics.drawLine(TRACK_PADDING, centerY, TRACK_PADDING + width, centerY);
        graphics.setColor(ACCENT);
        graphics.drawLine(TRACK_PADDING, centerY, thumbX, centerY);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(thumbX - 9, centerY - 9, 18, 18);
        graphics.dispose();
    }
}
