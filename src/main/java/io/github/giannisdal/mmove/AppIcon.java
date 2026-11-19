package io.github.giannisdal.mmove;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

final class AppIcon {
    private AppIcon() {
    }

    public static void main(String[] args) throws IOException {
        BufferedImage logo = load();
        if (logo == null) throw new IOException("Could not load assets/logo.png");

        Path iconset = Files.createDirectories(Path.of(args[0]));
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
            return source == null ? null : cropLogo(source);
        } catch (IOException _) {
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
        float badgeWidth = (float) width;
        float badgeHeight = (float) height;
        graphics.fill(new RoundRectangle2D.Float(0, 0, badgeWidth, badgeHeight,
                badgeWidth * 0.56f, badgeHeight * 0.56f));
        graphics.setComposite(AlphaComposite.SrcIn);
        graphics.drawImage(source, -left, -top, null);
        graphics.dispose();
        return cropped;
    }
}
