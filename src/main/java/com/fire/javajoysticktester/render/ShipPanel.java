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
 * Draws a simple wireframe ship by rotating 3D model points, then projecting
 * them into screen space. This is intentionally lightweight and easy to evolve.
 */
public class ShipPanel extends JPanel {
    private static final double[][] SHIP_VERTICES = {
            {0.0, 0.0, 1.6},   // nose
            {-0.9, 0.0, -1.1}, // left wing
            {0.9, 0.0, -1.1},  // right wing
            {0.0, 0.35, -0.7}, // top fin
            {0.0, -0.35, -0.7} // bottom fin
    };

    private static final int[][] SHIP_EDGES = {
            {0, 1}, {0, 2}, {0, 3}, {0, 4},
            {1, 2}, {1, 3}, {2, 3}, {1, 4}, {2, 4}
    };

    private final ShipState shipState;

    public ShipPanel(ShipState shipState) {
        this.shipState = shipState;
        setBackground(new Color(14, 18, 30));
        setFocusable(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawShipWireframe(g2d);
            drawHudText(g2d);
        } finally {
            g2d.dispose();
        }
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

            // Fake perspective: larger z means closer to camera.
            double cameraDistance = 6.0;
            double depth = rotated[2] + cameraDistance;
            double perspective = 220.0 / Math.max(1.0, depth);

            int sx = centerX + (int) Math.round(rotated[0] * perspective);
            int sy = centerY - (int) Math.round(rotated[1] * perspective);
            projectedPoints[i] = new Point(sx, sy);
        }

        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(new Color(120, 220, 255));
        for (int[] edge : SHIP_EDGES) {
            Point a = projectedPoints[edge[0]];
            Point b = projectedPoints[edge[1]];
            g2d.drawLine(a.x, a.y, b.x, b.y);
        }
    }

    private void drawHudText(Graphics2D g2d) {
        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        g2d.setColor(new Color(220, 240, 255));

        int x = 16;
        int y = 24;
        int lineHeight = 18;

        g2d.drawString(String.format("Pitch:    %7.2f deg", shipState.getPitchDegrees()), x, y);
        g2d.drawString(String.format("Yaw:      %7.2f deg", shipState.getYawDegrees()), x, y + lineHeight);
        g2d.drawString(String.format("Roll:     %7.2f deg", shipState.getRollDegrees()), x, y + lineHeight * 2);
        g2d.drawString(String.format("Throttle: %7.2f %%", shipState.getThrottle() * 100.0), x, y + lineHeight * 3);
        g2d.drawString("Controls: Arrows=Pitch/Yaw, Q/E=Roll, W/S=Throttle", x, y + lineHeight * 5);
    }

    private static double[] rotateXYZ(double x, double y, double z,
                                      double pitchDeg, double yawDeg, double rollDeg) {
        double pitch = Math.toRadians(pitchDeg);
        double yaw = Math.toRadians(yawDeg);
        double roll = Math.toRadians(rollDeg);

        // Pitch around X axis.
        double py = y * Math.cos(pitch) - z * Math.sin(pitch);
        double pz = y * Math.sin(pitch) + z * Math.cos(pitch);
        double px = x;

        // Yaw around Y axis.
        double yx = px * Math.cos(yaw) + pz * Math.sin(yaw);
        double yz = -px * Math.sin(yaw) + pz * Math.cos(yaw);
        double yy = py;

        // Roll around Z axis.
        double rx = yx * Math.cos(roll) - yy * Math.sin(roll);
        double ry = yx * Math.sin(roll) + yy * Math.cos(roll);
        double rz = yz;

        return new double[]{rx, ry, rz};
    }
}
