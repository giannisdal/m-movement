import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * mMove - keeps the machine "active" (Teams green, no lock) by moving the real cursor.
 * No admin rights needed. Requires Java 11+.
 *
 * Usage: java MMove.java [pixels] [intervalSeconds]   (initial slider values)
 */
public class MMove {
    private static final Color BG = new Color(0x14161B);
    private static final Color CARD = new Color(0x1E2128);
    private static final Color TRACK = new Color(0x333844);
    private static final Color ACCENT = new Color(0x6C8CFF);
    private static final Color GREEN = new Color(0x3DDC97);
    private static final Color RED = new Color(0xFF5C6C);
    private static final Color TEXT = new Color(0xE8EAF0);
    private static final Color MUTED = new Color(0x8A91A3);
    private static final String FONT = "Segoe UI";
    private static final String LOGO_RESOURCE = "/assets/logo.png";

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mover");
        t.setDaemon(true);
        return t;
    });
    private static ScheduledFuture<?> job;
    private static long scheduleVersion;
    private static int pixels = 200;
    private static int interval = 20;
    private static String lastMove;
    private static MainPanel root;
    private static JFrame frame;

    public static void main(String[] args) {
        if (!claimSingleInstance()) return;
        if (args.length > 0) pixels = clamp(Integer.parseInt(args[0]), 1, 400);
        if (args.length > 1) interval = clamp(Integer.parseInt(args[1]), 1, 120);
        SwingUtilities.invokeLater(MMove::buildUi);
    }

    /** Binds a loopback port as an instance lock; a second launch just focuses the first window. */
    private static boolean claimSingleInstance() {
        try {
            ServerSocket server = new ServerSocket(47653, 1, InetAddress.getLoopbackAddress());
            Thread listener = new Thread(() -> listenForFocusRequests(server), "single-instance");
            listener.setDaemon(true);
            listener.start();
            return true;
        } catch (IOException ignored) {
            requestExistingWindowFocus();
            return false;
        }
    }

    private static void listenForFocusRequests(ServerSocket server) {
        try (ServerSocket listener = server) {
            while (true) {
                listener.accept().close();
                SwingUtilities.invokeLater(MMove::focusWindow);
            }
        } catch (IOException ignored) {
            // The listener has closed.
        }
    }

    private static void requestExistingWindowFocus() {
        try {
            new Socket(InetAddress.getLoopbackAddress(), 47653).close();
        } catch (IOException ignored) {
            // The existing instance may have already exited.
        }
    }

    private static void focusWindow() {
        if (frame == null) return;
        frame.setExtendedState(JFrame.NORMAL);
        frame.toFront();
        frame.requestFocus();
    }

    private static void buildUi() {
        BufferedImage logo = loadLogo();
        root = new MainPanel(logo);

        frame = new JFrame("mMove");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (logo != null) frame.setIconImage(logo);
        frame.setContentPane(root);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static BufferedImage loadLogo() {
        URL resource = MMove.class.getResource(LOGO_RESOURCE);
        if (resource == null) return null;

        try {
            BufferedImage source = ImageIO.read(resource);
            return source == null ? null : cropLogo(source);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static BufferedImage cropLogo(BufferedImage source) {
        // Bounds of the purple badge within logo.png.
        int left = Math.round(source.getWidth() * 0.168f);
        int top = Math.round(source.getHeight() * 0.177f);
        int width = Math.round(source.getWidth() * 0.656f);
        int height = Math.round(source.getHeight() * 0.652f);
        BufferedImage cropped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = cropped.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.fill(new RoundRectangle2D.Float(0, 0, width, height, width * 0.56f, height * 0.56f));
        graphics.setComposite(AlphaComposite.SrcIn);
        graphics.drawImage(source, -left, -top, null);
        graphics.dispose();
        return cropped;
    }

    private static Graphics2D createGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    private static void drawCenteredText(Graphics2D graphics, String text, int width, int baseline) {
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString(text, (width - textWidth) / 2, baseline);
    }

    private static boolean running() {
        return job != null && !job.isCancelled();
    }

    private static void toggle() {
        if (running()) {
            stopMoving();
        } else {
            scheduleMovement();
        }
        root.repaint();
    }

    private static void stopMoving() {
        job.cancel(false);
        job = null;
        lastMove = null;
    }

    private static void updateDistance(int distance) {
        pixels = distance;
        refreshSettings();
    }

    private static void updateInterval(int seconds) {
        interval = seconds;
        refreshSettings();
    }

    private static void refreshSettings() {
        if (running()) scheduleMovement();
        root.repaint();
    }

    private static void scheduleMovement() {
        if (job != null) job.cancel(false);
        long version = ++scheduleVersion;
        int distance = pixels;
        job = scheduler.scheduleWithFixedDelay(() -> moveOnce(distance, version), 0, interval, TimeUnit.SECONDS);
    }

    private static void moveOnce(int distance, long version) {
        try {
            move(new Robot(), distance);
            String time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
            SwingUtilities.invokeLater(() -> {
                if (running() && version == scheduleVersion) {
                    lastMove = time;
                    root.repaint();
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static void move(Robot robot, int distance) {
        Point pointer = MouseInfo.getPointerInfo().getLocation();
        smoothMove(robot, pointer.x, pointer.x + distance, pointer.y);
        smoothMove(robot, pointer.x + distance, pointer.x, pointer.y);
    }

    private static void smoothMove(Robot robot, int startX, int endX, int y) {
        int steps = Math.min(Math.max(1, Math.abs(endX - startX)), 50);
        for (int step = 1; step <= steps; step++) {
            robot.mouseMove(startX + (endX - startX) * step / steps, y);
            robot.delay(10);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class MainPanel extends JPanel {
        private static final int CARD_X = 24;
        private static final int CARD_WIDTH = 312;
        private static final int DISTANCE_Y = 176;
        private static final int INTERVAL_Y = 262;

        private final BufferedImage logo;

        private MainPanel(BufferedImage logo) {
            super(null);
            this.logo = logo;
            setPreferredSize(new Dimension(360, 490));

            addSlider(new Slider(1, 400, pixels, MMove::updateDistance), DISTANCE_Y);
            addSlider(new Slider(1, 120, interval, MMove::updateInterval), INTERVAL_Y);

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
            paintCard(graphics, DISTANCE_Y, "Distance", pixels + " px");
            paintCard(graphics, INTERVAL_Y, "Interval", interval + " s");
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
            boolean active = running();
            graphics.setColor(active ? GREEN : MUTED);
            graphics.fillOval(36, 454, 10, 10);
            graphics.setColor(MUTED);
            graphics.setFont(new Font(FONT, Font.PLAIN, 12));

            String status = active ? "Running" : "Stopped";
            if (active && lastMove != null) status += " - last move " + lastMove;
            graphics.drawString(status, 54, 464);
        }
    }

    private static final class MovementButton extends JComponent {
        private MovementButton() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggle();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics = createGraphics(g);
            boolean active = running();
            graphics.setColor(active ? RED : ACCENT);
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(FONT, Font.BOLD, 14));
            drawCenteredText(graphics, active ? "Stop" : "Start", getWidth(), 31);
            graphics.dispose();
        }
    }

    /** Flat custom slider. */
    private static final class Slider extends JComponent {
        private static final int TRACK_PADDING = 10;

        private final int min;
        private final int max;
        private final IntConsumer onChange;
        private int value;

        private Slider(int min, int max, int value, IntConsumer onChange) {
            this.min = min;
            this.max = max;
            this.value = value;
            this.onChange = onChange;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter dragHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    updateValue(e.getX());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    updateValue(e.getX());
                }
            };
            addMouseListener(dragHandler);
            addMouseMotionListener(dragHandler);
        }

        private void updateValue(int mouseX) {
            double fraction = (mouseX - TRACK_PADDING) / (double) trackWidth();
            value = clamp((int) Math.round(min + fraction * (max - min)), min, max);
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
}
