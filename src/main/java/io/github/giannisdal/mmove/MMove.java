package io.github.giannisdal.mmove;

import java.awt.Frame;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * mMove - keeps the machine "active" (Teams green, no lock) by moving the real cursor.
 * No admin rights needed. Requires Java 22+.
 */
public class MMove {
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
        if (args.length > 0) pixels = Math.clamp(Integer.parseInt(args[0]), 1, 400);
        if (args.length > 1) interval = Math.clamp(Integer.parseInt(args[1]), 1, 120);
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
        } catch (IOException _) {
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
        } catch (IOException _) {
            // The listener has closed.
        }
    }

    private static void requestExistingWindowFocus() {
        try {
            new Socket(InetAddress.getLoopbackAddress(), 47653).close();
        } catch (IOException _) {
            // The existing instance may have already exited.
        }
    }

    private static void focusWindow() {
        if (frame == null) return;
        frame.setExtendedState(Frame.NORMAL);
        frame.toFront();
        frame.requestFocus();
    }

    private static void buildUi() {
        BufferedImage logo = AppIcon.load();
        root = new MainPanel(logo);

        frame = new JFrame("mMove");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        if (logo != null) frame.setIconImage(logo);
        frame.setContentPane(root);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static int distance() {
        return pixels;
    }

    static int intervalSeconds() {
        return interval;
    }

    static String lastMove() {
        return lastMove;
    }

    static boolean running() {
        return job != null && !job.isCancelled();
    }

    static void stopMoving() {
        job.cancel(false);
        job = null;
        lastMove = null;
    }

    static void updateDistance(int distance) {
        pixels = distance;
        refreshSettings();
    }

    static void updateInterval(int seconds) {
        interval = seconds;
        refreshSettings();
    }

    private static void refreshSettings() {
        if (running()) scheduleMovement();
        root.repaint();
    }

    static void scheduleMovement() {
        if (job != null) job.cancel(false);
        long version = ++scheduleVersion;
        int distance = pixels;
        job = scheduler.scheduleWithFixedDelay(() -> moveOnce(distance, version), 0, interval, TimeUnit.SECONDS);
    }

    private static void moveOnce(int distance, long version) {
        try {
            move(new Robot(), distance);
            String time = LocalTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS).toString();
            SwingUtilities.invokeLater(() -> {
                if (running() && version == scheduleVersion) {
                    lastMove = time;
                    root.repaint();
                }
            });
        } catch (Exception _) {
            // Leave the schedule active so the next move can retry if Robot is unavailable.
        }
    }

    private static void move(Robot robot, int distance) {
        Point pointer = MouseInfo.getPointerInfo().getLocation();
        smoothMove(robot, pointer.x, pointer.x + distance, pointer.y);
        smoothMove(robot, pointer.x + distance, pointer.x, pointer.y);
    }

    private static void smoothMove(Robot robot, int startX, int endX, int y) {
        int steps = Math.clamp(Math.abs(endX - startX), 1, 50);
        for (int step = 1; step <= steps; step++) {
            robot.mouseMove(startX + (endX - startX) * step / steps, y);
            robot.delay(10);
        }
    }
}
