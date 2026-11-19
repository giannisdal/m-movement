import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
    static final Color BG = new Color(0x14161B);
    static final Color CARD = new Color(0x1E2128);
    static final Color TRACK = new Color(0x333844);
    static final Color ACCENT = new Color(0x6C8CFF);
    static final Color GREEN = new Color(0x3DDC97);
    static final Color RED = new Color(0xFF5C6C);
    static final Color TEXT = new Color(0xE8EAF0);
    static final Color MUTED = new Color(0x8A91A3);
    static final String FONT = "Segoe UI";

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mover");
        t.setDaemon(true);
        return t;
    });
    private static ScheduledFuture<?> job;
    private static int pixels = 200;
    private static int interval = 20;
    private static String lastMove = null;
    private static JComponent root;
    private static JFrame frame;

    public static void main(String[] args) {
        if (!claimSingleInstance()) return;
        if (args.length > 0) pixels = clamp(Integer.parseInt(args[0]), 1, 400);
        if (args.length > 1) interval = clamp(Integer.parseInt(args[1]), 1, 120);
        SwingUtilities.invokeLater(MMove::buildUi);
    }

    /** Binds a loopback port as an instance lock; a second launch just focuses the first window. */
    static boolean claimSingleInstance() {
        try {
            ServerSocket server = new ServerSocket(47653, 1, InetAddress.getLoopbackAddress());
            Thread t = new Thread(() -> {
                while (true) {
                    try (Socket s = server.accept()) {
                        SwingUtilities.invokeLater(() -> {
                            if (frame != null) {
                                frame.setExtendedState(JFrame.NORMAL);
                                frame.toFront();
                                frame.requestFocus();
                            }
                        });
                    } catch (Exception e) {
                        return;
                    }
                }
            }, "single-instance");
            t.setDaemon(true);
            t.start();
            return true;
        } catch (Exception e) {
            try (Socket s = new Socket(InetAddress.getLoopbackAddress(), 47653)) {
                // connecting is the signal
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
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

    static void buildUi() {
        frame = new JFrame("mMove");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Image logo = null;
        try {
            logo = cropLogo(ImageIO.read(MMove.class.getResource("/logo.png")));
            frame.setIconImage(logo);
        } catch (Exception ignored) {
        }
        final Image logoImg = logo;

        Slider pxSlider = new Slider(1, 400, pixels, v -> {
            pixels = v;
            reschedule();
        });
        Slider intSlider = new Slider(1, 120, interval, v -> {
            interval = v;
            reschedule();
        });

        // Absolute layout inside a fixed-size panel so nothing shifts.
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = hq(g);
                g2.setColor(BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                if (logoImg != null) g2.drawImage(logoImg, 132, 24, 96, 96, null);
                g2.setColor(TEXT);
                g2.setFont(new Font(FONT, Font.BOLD, 22));
                center(g2, "mMove", 150);

                card(g2, 24, 176, "Distance", pixels + " px");
                card(g2, 24, 262, "Interval", interval + " s");

                boolean on = running();
                g2.setColor(on ? GREEN : MUTED);
                g2.fillOval(36, 454, 10, 10);
                g2.setColor(MUTED);
                g2.setFont(new Font(FONT, Font.PLAIN, 12));
                g2.drawString(on ? (lastMove == null ? "Running" : "Running - last move " + lastMove) : "Stopped", 54, 464);
            }

            void card(Graphics2D g2, int x, int y, String title, String value) {
                g2.setColor(CARD);
                g2.fillRoundRect(x, y, 312, 76, 18, 18);
                g2.setColor(MUTED);
                g2.setFont(new Font(FONT, Font.PLAIN, 12));
                g2.drawString(title, x + 16, y + 24);
                g2.setColor(TEXT);
                g2.setFont(new Font(FONT, Font.BOLD, 13));
                int w = g2.getFontMetrics().stringWidth(value);
                g2.drawString(value, x + 312 - 16 - w, y + 24);
            }

            void center(Graphics2D g2, String s, int y) {
                g2.drawString(s, (getWidth() - g2.getFontMetrics().stringWidth(s)) / 2, y);
            }
        };
        root = panel;
        panel.setPreferredSize(new Dimension(360, 490));

        pxSlider.setBounds(40, 216, 280, 26);
        intSlider.setBounds(40, 302, 280, 26);
        panel.add(pxSlider);
        panel.add(intSlider);

        JComponent button = new JComponent() {
            {
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
                Graphics2D g2 = hq(g);
                g2.setColor(running() ? RED : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(FONT, Font.BOLD, 14));
                String s = running() ? "Stop" : "Start";
                g2.drawString(s, (getWidth() - g2.getFontMetrics().stringWidth(s)) / 2, 31);
            }
        };
        button.setBounds(24, 372, 312, 48);
        panel.add(button);

        frame.setContentPane(panel);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static Graphics2D hq(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    static boolean running() {
        return job != null && !job.isCancelled();
    }

    static void toggle() {
        if (running()) {
            job.cancel(false);
            job = null;
            lastMove = null;
        } else {
            reschedule(true);
        }
        root.repaint();
    }

    static void reschedule() {
        reschedule(false);
    }

    static void reschedule(boolean force) {
        if (!force && !running()) {
            root.repaint();
            return;
        }
        if (job != null) job.cancel(false);
        job = scheduler.scheduleWithFixedDelay(() -> {
            try {
                move(new Robot(), pixels);
                lastMove = LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
                SwingUtilities.invokeLater(() -> root.repaint());
            } catch (Exception ignored) {
            }
        }, 0, interval, TimeUnit.SECONDS);
        root.repaint();
    }

    static void move(Robot robot, int px) {
        Point p = MouseInfo.getPointerInfo().getLocation();
        smoothMove(robot, p.x, p.y, p.x + px, p.y);
        smoothMove(robot, p.x + px, p.y, p.x, p.y);
    }

    static void smoothMove(Robot r, int x1, int y1, int x2, int y2) {
        int steps = Math.min(Math.max(1, Math.abs(x2 - x1)), 50);
        for (int i = 1; i <= steps; i++) {
            r.mouseMove(x1 + (x2 - x1) * i / steps, y1 + (y2 - y1) * i / steps);
            r.delay(10);
        }
    }

    /** Flat custom slider. */
    static class Slider extends JComponent {
        final int min, max;
        int value;
        final IntConsumer onChange;

        Slider(int min, int max, int value, IntConsumer onChange) {
            this.min = min;
            this.max = max;
            this.value = value;
            this.onChange = onChange;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    set(e.getX());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    set(e.getX());
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        void set(int x) {
            int pad = 10;
            double f = (x - pad) / (double) (getWidth() - 2 * pad);
            value = clamp((int) Math.round(min + f * (max - min)), min, max);
            onChange.accept(value);
            repaint();
            if (getParent() != null) getParent().repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = hq(g);
            int pad = 10, cy = getHeight() / 2;
            int w = getWidth() - 2 * pad;
            int x = pad + (int) ((value - min) / (double) (max - min) * w);
            g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(TRACK);
            g2.drawLine(pad, cy, pad + w, cy);
            g2.setColor(ACCENT);
            g2.drawLine(pad, cy, x, cy);
            g2.setColor(Color.WHITE);
            g2.fillOval(x - 9, cy - 9, 18, 18);
        }
    }
}
