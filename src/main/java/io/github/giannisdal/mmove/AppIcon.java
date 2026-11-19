package io.github.giannisdal.mmove;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

final class AppIcon {
    private AppIcon() {
    }

    public static void main(String[] args) throws IOException {
        BufferedImage logo = load();
        if (logo == null) throw new IOException("Could not load assets/logo.png");

        Path iconset = Files.createDirectories(Paths.get(args[0]));
        for (int size : new int[] {16, 32, 128, 256, 512}) {
            String name = "icon_" + size + "x" + size;
            writeIcon(logo, size, iconset.resolve(name + ".png"));
            writeIcon(logo, size * 2, iconset.resolve(name + "@2x.png"));
        }
    }

    private static void writeIcon(BufferedImage logo, int size, Path path) throws IOException {
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int badgeWidth = Math.round((float) size * 0.8f);
        int badgeHeight = Math.round((float) badgeWidth * (float) logo.getHeight() / (float) logo.getWidth());
        Graphics2D graphics = icon.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(logo, (size - badgeWidth) / 2, (size - badgeHeight) / 2,
                badgeWidth, badgeHeight, null);
        graphics.dispose();
        ImageIO.write(icon, "png", path.toFile());
    }

    static BufferedImage load() {
        URL resource = AppIcon.class.getResource("/assets/logo.png");
        if (resource == null) return null;

        try {
            BufferedImage source = ImageIO.read(resource);
            return source;
        } catch (IOException ignored) {
            return null;
        }
    }

    /** Downscales in halving steps so large artwork stays sharp at small sizes. */
    static BufferedImage scaled(BufferedImage source, int size) {
        if (source == null) return null;
        BufferedImage current = source;
        while (current.getWidth() > size) {
            int next = Math.max(size, current.getWidth() / 2);
            BufferedImage step = new BufferedImage(next, next, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = step.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(current, 0, 0, next, next, null);
            graphics.dispose();
            current = step;
        }
        return current;
    }
}