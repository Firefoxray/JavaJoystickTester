package com.fire.javajoysticktester.render;

import com.fire.javajoysticktester.input.JoystickSnapshot;
import com.fire.javajoysticktester.input.PreferredInputDevice;
import com.fire.javajoysticktester.model.ShipState;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Rendering surface.
 */
public class ShipPanel extends JPanel {
    private static final double[][] SHIP_VERTICES = {
            {0.00, 0.00, 2.55},
            {-0.35, 0.12, 0.90},
            {0.35, 0.12, 0.90},
            {-0.35, -0.12, 0.90},
            {0.35, -0.12, 0.90},
            {-1.35, -0.05, -0.40},
            {1.35, -0.05, -0.40},
            {0.00, 0.36, -0.55},
            {0.00, -0.36, -0.55},
            {0.00, 0.00, -1.25},
            {0.00, 0.24, 1.55}
    };

    private static final int[][] SHIP_EDGES = {
            {0, 1}, {0, 2}, {0, 3}, {0, 4},
            {1, 2}, {3, 4}, {1, 3}, {2, 4},
            {3, 5}, {4, 6}, {5, 6},
            {1, 7}, {2, 7}, {3, 8}, {4, 8},
            {5, 9}, {6, 9}, {7, 9}, {8, 9},
            {0, 10}, {1, 10}, {2, 10}
    };

    private static final int STAR_COUNT = 220;
    private static final double[][] STAR_FIELD = buildStars();

    private final ShipState shipState;

    private PreferredInputDevice preferredInputDevice = PreferredInputDevice.AUTO;
    private boolean keyboardActive;
    private JoystickSnapshot joystickSnapshot = JoystickSnapshot.disconnected(java.util.List.of());
    private String activeInputDescription = "Keyboard";

    private long lastStarNanos = System.nanoTime();

    public ShipPanel(ShipState shipState) {
        this.shipState = shipState;
        setBackground(new Color(8, 12, 24));
        setFocusable(true);
    }

    public void updateInputDebug(PreferredInputDevice preferredInputDevice, boolean keyboardActive, JoystickSnapshot joystickSnapshot, String activeInputDescription) {
        this.preferredInputDevice = preferredInputDevice;
        this.keyboardActive = keyboardActive;
        this.joystickSnapshot = joystickSnapshot;
        this.activeInputDescription = activeInputDescription;
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
            drawButtonPanel(g2d);
        } finally {
            g2d.dispose();
        }
    }

    private void drawBackgroundReferences(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        updateStarField();
        drawStars(g2d, centerX, centerY, width, height);
        drawCenterReticle(g2d, centerX, centerY);
    }

    private void updateStarField() {
        long now = System.nanoTime();
        double deltaSec = Math.max(0.0, (now - lastStarNanos) / 1_000_000_000.0);
        lastStarNanos = now;

        double speed = 0.5 + shipState.getThrottle() * 2.8;
        for (double[] star : STAR_FIELD) {
            star[2] -= deltaSec * speed;
            if (star[2] <= 0.08) {
                resetStar(star, (int) (star[3] * 1000));
            }
        }
    }

    private void drawStars(Graphics2D g2d, int centerX, int centerY, int width, int height) {
        for (double[] star : STAR_FIELD) {
            double depth = Math.max(0.1, star[2]);
            double perspective = 1.0 / depth;

            int x = centerX + (int) Math.round(star[0] * perspective);
            int y = centerY + (int) Math.round(star[1] * perspective);

            if (x < 0 || x >= width || y < 0 || y >= height) {
                continue;
            }

            int shade = (int) Math.round(140 + (1.0 - depth) * 110);
            g2d.setColor(new Color(shade, shade, 255, 180));
            int size = depth < 0.35 ? 3 : (depth < 0.7 ? 2 : 1);
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

        g2d.setColor(new Color(200, 250, 255));
        Point nose = projectedPoints[0];
        Point centerBody = projectedPoints[9];
        g2d.setStroke(new BasicStroke(2.4f));
        g2d.drawLine(centerBody.x, centerBody.y, nose.x, nose.y);
        g2d.fillOval(nose.x - 4, nose.y - 4, 8, 8);
    }

    private void drawHudText(Graphics2D g2d) {
        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        int x = 16;
        int y = 24;
        int lineHeight = 18;

        g2d.setColor(new Color(220, 240, 255));
        g2d.drawString("Java Joystick Tester (0.2 Alpha)", x, y);

        g2d.setColor(new Color(170, 220, 255));
        g2d.drawString(String.format("Pitch: %7.2f°  (target %7.2f°)", shipState.getPitchDegrees(), shipState.getTargetPitchDegrees()), x, y + lineHeight);
        g2d.drawString(String.format("Yaw:   %7.2f°  (target %7.2f°)", shipState.getYawDegrees(), shipState.getTargetYawDegrees()), x, y + lineHeight * 2);
        g2d.drawString(String.format("Roll:  %7.2f°  (target %7.2f°)", shipState.getRollDegrees(), shipState.getTargetRollDegrees()), x, y + lineHeight * 3);
        g2d.drawString(String.format("Throttle: %6.1f%% (target %6.1f%%)", shipState.getThrottle() * 100.0, shipState.getTargetThrottle() * 100.0), x, y + lineHeight * 4);

        g2d.setColor(new Color(130, 190, 235));
        g2d.drawString("Controls: Arrows=Pitch/Yaw, Q/E=Roll, W/S=Throttle", x, y + lineHeight * 6);
        g2d.drawString("Settings menu: choose input, controller, mapping", x, y + lineHeight * 7);

        g2d.drawString("Preferred Input: " + preferredInputDevice + " | Active: " + activeInputDescription, x, y + lineHeight * 8);
        g2d.drawString("Joystick access: " + joystickSnapshot.accessStatus(), x, y + lineHeight * 9);
        g2d.drawString("T.16000M detected: " + (joystickSnapshot.thrustmasterT16000MDetected() ? "YES" : "NO") + " | Keyboard Active: " + (keyboardActive ? "YES" : "NO"), x, y + lineHeight * 10);
        g2d.drawString("Raw axes: " + formatAxes(joystickSnapshot.axes()), x, y + lineHeight * 11);
    }

    private void drawButtonPanel(Graphics2D g2d) {
        int panelWidth = 260;
        int panelHeight = Math.max(180, getHeight() - 32);
        int panelX = getWidth() - panelWidth - 16;
        int panelY = 16;

        g2d.setColor(new Color(12, 22, 40, 190));
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 14, 14);
        g2d.setColor(new Color(90, 160, 220, 180));
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 14, 14);

        int x = panelX + 12;
        int y = panelY + 22;

        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g2d.setColor(new Color(220, 240, 255));
        g2d.drawString("Controller Buttons", x, y);

        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String controllerName = joystickSnapshot.connected() ? joystickSnapshot.controllerName() : "No controller";
        g2d.setColor(new Color(160, 210, 245));
        g2d.drawString(controllerName, x, y + 17);

        List<Integer> buttonIndices = extractButtonIndices(joystickSnapshot.buttons());
        if (buttonIndices.isEmpty()) {
            g2d.setColor(new Color(170, 190, 210));
            g2d.drawString("No digital buttons reported.", x, y + 42);
            return;
        }

        int top = y + 30;
        int cols = 4;
        int cellWidth = (panelWidth - 24) / cols;
        int cellHeight = 24;

        for (int i = 0; i < buttonIndices.size(); i++) {
            int index = buttonIndices.get(i);
            int col = i % cols;
            int row = i / cols;
            int cellX = x + col * cellWidth;
            int cellY = top + row * (cellHeight + 6);

            if (cellY + cellHeight > panelY + panelHeight - 8) {
                g2d.setColor(new Color(170, 190, 210));
                g2d.drawString("...", x, panelY + panelHeight - 10);
                break;
            }

            boolean pressed = isButtonIndexPressed(joystickSnapshot.buttons(), index);
            Color fill = pressed ? new Color(70, 200, 120, 210) : new Color(35, 55, 85, 180);
            Color border = pressed ? new Color(150, 255, 180, 230) : new Color(120, 170, 210, 140);

            g2d.setColor(fill);
            g2d.fillRoundRect(cellX, cellY, cellWidth - 8, cellHeight, 8, 8);
            g2d.setColor(border);
            g2d.drawRoundRect(cellX, cellY, cellWidth - 8, cellHeight, 8, 8);

            g2d.setColor(new Color(235, 245, 255));
            g2d.drawString("B" + index, cellX + 8, cellY + 16);
        }
    }

    private static List<Integer> extractButtonIndices(Map<String, Boolean> buttons) {
        List<Integer> out = new ArrayList<>();
        for (String key : buttons.keySet()) {
            Integer parsed = parseButtonIndex(key);
            if (parsed != null && !out.contains(parsed)) {
                out.add(parsed);
            }
        }
        out.sort(Integer::compareTo);
        return out;
    }

    private static boolean isButtonIndexPressed(Map<String, Boolean> buttons, int targetButtonIndex) {
        for (Map.Entry<String, Boolean> entry : buttons.entrySet()) {
            if (!entry.getValue()) {
                continue;
            }
            Integer parsed = parseButtonIndex(entry.getKey());
            if (parsed != null && parsed == targetButtonIndex) {
                return true;
            }
        }
        return false;
    }

    private static Integer parseButtonIndex(String raw) {
        if (raw == null) {
            return null;
        }

        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }

        if (digits.length() == 0) {
            return null;
        }

        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String formatAxes(Map<String, Float> axes) {
        if (axes.isEmpty()) {
            return "none";
        }
        StringJoiner joiner = new StringJoiner(", ");
        int count = 0;
        for (Map.Entry<String, Float> entry : axes.entrySet()) {
            joiner.add(entry.getKey() + "=" + String.format("%.2f", entry.getValue()));
            count++;
            if (count >= 6) {
                break;
            }
        }
        return joiner.toString();
    }

    private static double[][] buildStars() {
        double[][] stars = new double[STAR_COUNT][4];
        for (int i = 0; i < STAR_COUNT; i++) {
            resetStar(stars[i], i);
        }
        return stars;
    }

    private static void resetStar(double[] star, int seed) {
        double nx = (((seed * 73) % 1000) / 999.0) * 2.0 - 1.0;
        double ny = (((seed * 151 + 37) % 1000) / 999.0) * 2.0 - 1.0;
        double depth = 0.35 + (((seed * 199 + 11) % 1000) / 999.0) * 1.2;
        star[0] = nx * 460.0;
        star[1] = ny * 320.0;
        star[2] = depth;
        star[3] = seed;
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
