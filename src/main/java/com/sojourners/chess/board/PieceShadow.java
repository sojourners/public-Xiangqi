package com.sojourners.chess.board;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shared soft shadow drawing for the board and the theme preview. */
public final class PieceShadow {

    private static final int CACHE_SIZE = 12;
    private static final Map<Shape, Sprite> CACHE = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Shape, Sprite> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private PieceShadow() {
    }

    /** Angle is clockwise from the right; length is a percentage of the piece diameter. */
    public static void draw(GraphicsContext gc, double centerX, double centerY, double radius,
                            double angle, double lengthPercent) {
        draw(gc, centerX, centerY, radius, angle, lengthPercent, 100);
    }

    /** Size scales the circular footprint; cast length stays relative to the piece diameter. */
    public static void draw(GraphicsContext gc, double centerX, double centerY, double radius,
                            double angle, double lengthPercent, double sizePercent) {
        if (!Double.isFinite(radius) || radius <= 0 || !Double.isFinite(angle)
                || !Double.isFinite(lengthPercent) || !Double.isFinite(sizePercent)) {
            return;
        }
        angle = ((angle % 360) + 360) % 360;
        lengthPercent = Math.max(0, Math.min(50, lengthPercent));
        sizePercent = Math.max(0, Math.min(100, sizePercent));
        if (sizePercent == 0) {
            return;
        }
        Shape shape = new Shape(radius, angle, lengthPercent, sizePercent);
        Sprite sprite = CACHE.computeIfAbsent(shape, PieceShadow::createSprite);
        gc.drawImage(sprite.image(), centerX + sprite.left(), centerY + sprite.top());
    }

    private static Sprite createSprite(Shape shape) {
        double size = shape.sizePercent() / 100;
        double radius = shape.radius() * size;
        double length = shape.radius() * 2 * shape.lengthPercent() / 100;
        double angle = Math.toRadians(shape.angle());
        double dx = Math.cos(angle) * length;
        double dy = Math.sin(angle) * length;
        double softness = Math.max(1.5, shape.radius() * 0.1) * size;
        int left = (int) Math.floor(Math.min(0, dx) - radius - softness - 1);
        int top = (int) Math.floor(Math.min(0, dy) - radius - softness - 1);
        int width = (int) Math.ceil(Math.max(0, dx) + radius + softness + 1) - left;
        int height = (int) Math.ceil(Math.max(0, dy) + radius + softness + 1) - top;
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixels = image.getPixelWriter();

        // Sweep the circular footprint along the cast direction, keeping the shadow
        // connected to the piece even at longer settings, then soften its boundary.
        double squaredLength = dx * dx + dy * dy;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double px = left + x + 0.5;
                double py = top + y + 0.5;
                double t = squaredLength == 0 ? 0
                        : Math.max(0, Math.min(1, (px * dx + py * dy) / squaredLength));
                double distance = Math.hypot(px - t * dx, py - t * dy);
                double edge = Math.max(0, Math.min(1, (radius + softness - distance) / (2 * softness)));
                int alpha = (int) Math.round(0.32 * 255 * edge * edge * (3 - 2 * edge));
                pixels.setArgb(x, y, alpha << 24);
            }
        }
        return new Sprite(image, left, top);
    }

    private record Shape(double radius, double angle, double lengthPercent, double sizePercent) {
    }

    private record Sprite(WritableImage image, int left, int top) {
    }
}
