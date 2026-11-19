package io.github.giannisdal.mmove;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.io.Serial;
import javax.swing.JComponent;

import static io.github.giannisdal.mmove.UiStyle.ACCENT;
import static io.github.giannisdal.mmove.UiStyle.FONT;
import static io.github.giannisdal.mmove.UiStyle.createGraphics;
import static io.github.giannisdal.mmove.UiStyle.drawCenteredText;

final class MovementButton extends JComponent {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Color RED = new Color(0xFF5C6C);

    MovementButton() {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        super.processMouseEvent(event);
        if (event.getID() == MouseEvent.MOUSE_CLICKED) toggle();
    }

    private void toggle() {
        if (MMove.running()) {
            MMove.stopMoving();
        } else {
            MMove.scheduleMovement();
        }
        getParent().repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D graphics = createGraphics(g);
        boolean active = MMove.running();
        graphics.setColor(active ? RED : ACCENT);
        graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(FONT, Font.BOLD, 14));
        drawCenteredText(graphics, active ? "Stop" : "Start", getWidth(), 31);
        graphics.dispose();
    }
}
