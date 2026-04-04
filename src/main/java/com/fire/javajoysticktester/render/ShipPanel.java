package com.fire.javajoysticktester.render;

import com.fire.javajoysticktester.model.ShipState;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

/**
 * Rendering surface.
 *
 * Draws a lightweight low-poly ship by rotating 3D model points, then projecting
 * them into screen space. Also renders simple flight references (stars + horizon)
 * so pitch/yaw/roll are easier to read.
 */
public class ShipPanel extends JPanel {
    private static final double[][] SHIP_VERTICES = {
            {0.00, 0.00, 2.10},   // 0 nose
            {-0.35, 0.12, 0.90},  // 1 upper-left front
            {0.35, 0.12, 0.90},   // 2 upper-right front
            {-0.35, -0.12, 0.90}, // 3 lower-left front
            {0.35, -0.12, 0.90},  // 4 lower-right front
            {-1.35, -0.05, -0.40},// 5 left wing tip
            {1.35, -0.05, -0.40}, // 6 right wing tip
            {0.00, 0.36, -0.55},  // 7 dorsal fin
            {0.00, -0.36, -0.55}, // 8 ventral fin
            {0.00, 0.00, -1.25}   // 9 engine
    };

    private static final int[][] SHIP_EDGES = {
            {0, 1}, {0, 2}, {0, 3}, {0, 4},
            {1, 2}, {3, 4}, {1, 3}, {2, 4},
            {3, 5}, {4, 6}, {5, 6},
            {1, 7}, {2, 7}, {3, 8}, {4, 8},
            {5, 9}, {6, 9}, {7, 9}, {8, 9}
    };

    private static final double[][] STAR_FIELD = buildStars();

    private final ShipState shipState;

    public ShipPanel(ShipState shipState) {
        this.shipState = shipState;
        setBackground(new Color(8, 12, 24));
        setFocusable(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawBackgroundReferences(g2d);
            drawShipWireframe(g2d);
            drawHudText(g2d);
        } finally {
            g2d.dispose();
        }
    }

    private void drawBackgroundReferences(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        drawStars(g2d, centerX, centerY, width, height);

        double pitchOffset = shipState.getPitchDegrees() * 3.0;
        double rollRad = Math.toRadians(shipState.getRollDegrees());
        double cos = Math.cos(rollRad);
        double sin = Math.sin(rollRad);

        g2d.setColor(new Color(52, 90, 130, 170));
        g2d.setStroke(new BasicStroke(2f));

        int halfSpan = Math.max(width, height);
        int x1 = -halfSpan;
        int y1 = (int) Math.round(pitchOffset);
        int x2 = halfSpan;
        int y2 = (int) Math.round(pitchOffset);

        Point p1 = rotate2D(x1, y1, cos, sin, centerX, centerY);
        Point p2 = rotate2D(x2, y2, cos, sin, centerX, centerY);
        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

        g2d.setStroke(new BasicStroke(1f));
        g2d.setColor(new Color(44, 70, 104, 110));
        for (int i = -3; i <= 3; i++) {
            if (i == 0) {
                continue;
            }
            int gy = (int) Math.round(pitchOffset + i * 44);
            Point ga = rotate2D(-halfSpan, gy, cos, sin, centerX, centerY);
            Point gb = rotate2D(halfSpan, gy, cos, sin, centerX, centerY);
            g2d.drawLine(ga.x, ga.y, gb.x, gb.y);
        }

        drawCenterReticle(g2d, centerX, centerY);
    }

    private void drawStars(Graphics2D g2d, int centerX, int centerY, int width, int height) {
        double yawShift = shipState.getYawDegrees() * 6.0;
        double pitchShift = shipState.getPitchDegrees() * 4.0;
        double speedParallax = shipState.getThrottle() * 20.0;

        for (double[] star : STAR_FIELD) {
            double sx = wrapToRange(star[0] + yawShift * star[2], -620.0, 620.0);
            double sy = wrapToRange(star[1] + pitchShift * star[2] + speedParallax * (star[2] - 0.6), -420.0, 420.0);

            int x = centerX + (int) Math.round(sx);
            int y = centerY + (int) Math.round(sy);

            if (x < 0 || x >= width || y < 0 || y >= height) {
                continue;
            }

            int shade = (int) Math.round(120 + star[2] * 115);
            g2d.setColor(new Color(shade, shade, 255, 160));
            int size = star[2] > 0.82 ? 2 : 1;
            g2d.fillRect(x, y, size, size);
        }
    }

    private void drawCenterReticle(Graphics2D g2d, int centerX, int centerY) {
        g2d.setColor(new Color(140, 190, 240, 150));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawLine(centerX - 12, centerY, centerX - 3, centerY);
        g2d.drawLine(centerX + 3, centerY, centerX + 12, centerY);
        g2d.drawLine(centerX, centerY - 12, centerX, centerY - 3);
        g2d.drawLine(centerX, centerY + 3, centerX, centerY + 12);
    }

    private void drawShipWireframe(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        Point[] projectedPoints = new Point[SHIP_VERTICES.length];
        for (int i = 0; i < SHIP_VERTICES.length; i++) {
            double[] p = SHIP_VERTICES[i];
            double[] rotated = rotateXYZ(p[0], p[1], p[2],
                    shipState.getPitchDegrees(),
                    shipState.getYawDegrees(),
                    shipState.getRollDegrees());

            double cameraDistance = 7.4;
            double depth = rotated[2] + cameraDistance;
            double perspective = 260.0 / Math.max(1.0, depth);

            int sx = centerX + (int) Math.round(rotated[0] * perspective);
            int sy = centerY - (int) Math.round(rotated[1] * perspective);
            projectedPoints[i] = new Point(sx, sy);
        }

        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(new Color(110, 225, 255));
        for (int[] edge : SHIP_EDGES) {
            Point a = projectedPoints[edge[0]];
            Point b = projectedPoints[edge[1]];
            g2d.drawLine(a.x, a.y, b.x, b.y);
        }

        g2d.setColor(new Color(180, 250, 255));
        Point nose = projectedPoints[0];
        g2d.fillOval(nose.x - 3, nose.y - 3, 6, 6);
    }

    private void drawHudText(Graphics2D g2d) {
        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        int x = 16;
        int y = 24;
        int lineHeight = 18;

        g2d.setColor(new Color(220, 240, 255));
        g2d.drawString("Ship Debug", x, y);

        g2d.setColor(new Color(170, 220, 255));
        g2d.drawString(String.format("Pitch: %7.2f°  (target %7.2f°)", shipState.getPitchDegrees(), shipState.getTargetPitchDegrees()), x, y + lineHeight);
        g2d.drawString(String.format("Yaw:   %7.2f°  (target %7.2f°)", shipState.getYawDegrees(), shipState.getTargetYawDegrees()), x, y + lineHeight * 2);
        g2d.drawString(String.format("Roll:  %7.2f°  (target %7.2f°)", shipState.getRollDegrees(), shipState.getTargetRollDegrees()), x, y + lineHeight * 3);
        g2d.drawString(String.format("Throttle: %6.1f%% (target %6.1f%%)", shipState.getThrottle() * 100.0, shipState.getTargetThrottle() * 100.0), x, y + lineHeight * 4);

        g2d.setColor(new Color(130, 190, 235));
        g2d.drawString("Controls: Arrows=Pitch/Yaw, Q/E=Roll, W/S=Throttle", x, y + lineHeight * 6);
        g2d.drawString("Auto-center: Pitch/Yaw/Roll enabled", x, y + lineHeight * 7);
    }

    private static Point rotate2D(int x, int y, double cos, double sin, int cx, int cy) {
        int rx = (int) Math.round(x * cos - y * sin) + cx;
        int ry = (int) Math.round(x * sin + y * cos) + cy;
        return new Point(rx, ry);
    }

    private static double wrapToRange(double value, double min, double max) {
        double range = max - min;
        double wrapped = (value - min) % range;
        if (wrapped < 0) {
            wrapped += range;
        }
        return wrapped + min;
    }

    private static double[][] buildStars() {
        int starCount = 90;
        double[][] stars = new double[starCount][3];
        for (int i = 0; i < starCount; i++) {
            double x = -600.0 + ((i * 137) % 1200);
            double y = -380.0 + ((i * 89) % 760);
            double brightness = 0.45 + (((i * 53) % 100) / 100.0) * 0.55;
            stars[i][0] = x;
            stars[i][1] = y;
            stars[i][2] = brightness;
        }
        return stars;
    }

    private static double[] rotateXYZ(double x, double y, double z,
                                      double pitchDeg, double yawDeg, double rollDeg) {
        double pitch = Math.toRadians(pitchDeg);
        double yaw = Math.toRadians(yawDeg);
        double roll = Math.toRadians(rollDeg);

        double py = y * Math.cos(pitch) - z * Math.sin(pitch);
        double pz = y * Math.sin(pitch) + z * Math.cos(pitch);
        double px = x;

        double yx = px * Math.cos(yaw) + pz * Math.sin(yaw);
        double yz = -px * Math.sin(yaw) + pz * Math.cos(yaw);
        double yy = py;

        double rx = yx * Math.cos(roll) - yy * Math.sin(roll);
        double ry = yx * Math.sin(roll) + yy * Math.cos(roll);
        double rz = yz;

        return new double[]{rx, ry, rz};
    }
}
