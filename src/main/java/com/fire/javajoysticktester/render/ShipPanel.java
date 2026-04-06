package com.fire.javajoysticktester.render;

import com.fire.javajoysticktester.AppInfo;
import com.fire.javajoysticktester.input.InputSystem;
import com.fire.javajoysticktester.input.JoystickSnapshot;
import com.fire.javajoysticktester.input.PreferredInputDevice;
import com.fire.javajoysticktester.model.ShipState;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
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
    private static final double RENDER_YAW_OFFSET_DEG = 180.0;
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
    private static final int[][] SHIP_FACES = {
            {0, 1, 10}, {0, 10, 2}, {1, 3, 5}, {2, 6, 4},
            {3, 4, 8}, {1, 7, 2}, {5, 6, 9}, {7, 9, 8},
            {1, 3, 8, 7}, {2, 7, 8, 4}, {3, 5, 9, 8}, {4, 8, 9, 6}
    };
    private static final Color[] SHIP_FACE_COLORS = {
            new Color(95, 190, 235, 220), new Color(82, 165, 220, 220), new Color(70, 120, 180, 220),
            new Color(70, 120, 180, 220), new Color(60, 90, 145, 220), new Color(76, 140, 188, 220),
            new Color(52, 78, 132, 220), new Color(66, 108, 162, 220), new Color(52, 90, 145, 220),
            new Color(52, 90, 145, 220), new Color(45, 72, 120, 220), new Color(45, 72, 120, 220)
    };

    private static final int STAR_COUNT = 520;
    private static final double[][] STAR_FIELD = buildStars();

    private final ShipState shipState;

    private PreferredInputDevice preferredInputDevice = PreferredInputDevice.AUTO;
    private boolean keyboardActive;
    private JoystickSnapshot joystickSnapshot = JoystickSnapshot.disconnected(java.util.List.of());
    private String activeInputDescription = "Keyboard";
    private InputSystem inputSystem;
    private boolean solidPlaneEnabled;
    private boolean firePrimaryActive;
    private boolean boostActive;
    private boolean debugModeEnabled;
    private double lastPitchDegrees;
    private double lastYawDegrees;
    private double lastRollDegrees;

    private long lastStarNanos = System.nanoTime();
    private long starRespawnCounter = 1009;

    public ShipPanel(ShipState shipState) {
        this.shipState = shipState;
        setBackground(new Color(8, 12, 24));
        setFocusable(true);
    }

    public void updateInputDebug(PreferredInputDevice preferredInputDevice,
                                 boolean keyboardActive,
                                 JoystickSnapshot joystickSnapshot,
                                 String activeInputDescription,
                                 InputSystem inputSystem) {
        this.preferredInputDevice = preferredInputDevice;
        this.keyboardActive = keyboardActive;
        this.joystickSnapshot = joystickSnapshot;
        this.activeInputDescription = activeInputDescription;
        this.inputSystem = inputSystem;
        this.solidPlaneEnabled = inputSystem != null && inputSystem.isSolidPlaneEnabled();
        this.firePrimaryActive = inputSystem != null && inputSystem.isFirePrimaryActive();
        this.boostActive = inputSystem != null && inputSystem.isBoostActive();
        this.debugModeEnabled = inputSystem != null && inputSystem.isDebugModeEnabled();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawBackgroundReferences(g2d);
            drawShip(g2d);
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

        double yawDelta = clamp(shortestAngleDelta(lastYawDegrees, shipState.getYawDegrees()), -3.0, 3.0);
        double pitchDelta = clamp(shortestAngleDelta(lastPitchDegrees, shipState.getPitchDegrees()), -3.0, 3.0);
        double rollDelta = clamp(shortestAngleDelta(lastRollDegrees, shipState.getRollDegrees()), -3.0, 3.0);
        lastYawDegrees = shipState.getYawDegrees();
        lastPitchDegrees = shipState.getPitchDegrees();
        lastRollDegrees = shipState.getRollDegrees();

        double boostMultiplier = boostActive ? 2.2 : 1.0;
        double speed = (0.82 + shipState.getThrottle() * 3.25) * boostMultiplier;
        double yawShift = yawDelta * 52.0;
        double pitchShift = pitchDelta * 46.0;
        double rollShift = rollDelta * 24.0;
        for (double[] star : STAR_FIELD) {
            star[2] -= deltaSec * speed;
            star[0] -= yawShift + rollShift;
            star[1] += pitchShift - rollShift * 0.35;
            if (star[2] <= 0.08) {
                starRespawnCounter += 7919;
                resetStar(star, (int) (starRespawnCounter & 0x7FFFFFFF));
            } else if (Math.abs(star[0]) > 980 || Math.abs(star[1]) > 760) {
                starRespawnCounter += 3089;
                resetStar(star, (int) (starRespawnCounter & 0x7FFFFFFF));
            }
        }
    }

    private void drawStars(Graphics2D g2d, int centerX, int centerY, int width, int height) {
        for (double[] star : STAR_FIELD) {
            double depth = Math.max(0.1, star[2]);
            double perspective = 1.0 / depth;

            int x = centerX + (int) Math.round(star[0] * perspective);
            int y = centerY + (int) Math.round(star[1] * perspective);
            double trailOffset = boostActive ? 0.18 : 0.085;
            int trailX = centerX + (int) Math.round(star[0] * (1.0 / (depth + trailOffset)));
            int trailY = centerY + (int) Math.round(star[1] * (1.0 / (depth + trailOffset)));

            if (x < 0 || x >= width || y < 0 || y >= height) {
                continue;
            }

            int shade = (int) Math.round(145 + (1.0 - depth) * 105);
            int alpha = (int) Math.round(120 + (1.0 - depth) * 110);
            Color starColor = boostActive
                    ? new Color(Math.min(255, shade + 20), Math.min(255, shade + 35), 255, Math.max(70, Math.min(alpha + 30, 255)))
                    : new Color(shade, shade, 255, Math.max(70, Math.min(alpha, 240)));
            g2d.setColor(starColor);

            if (depth < (boostActive ? 0.9 : 0.58)) {
                g2d.setStroke(new BasicStroke(depth < 0.28 ? 2.6f : (boostActive ? 1.8f : 1.2f)));
                g2d.drawLine(trailX, trailY, x, y);
            }

            int size = depth < 0.28 ? 3 : (depth < 0.65 ? 2 : 1);
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

    private void drawShip(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        Point[] projectedPoints = new Point[SHIP_VERTICES.length];
        double[] depthValues = new double[SHIP_VERTICES.length];
        for (int i = 0; i < SHIP_VERTICES.length; i++) {
            double[] p = SHIP_VERTICES[i];
            double[] rotated = rotateXYZ(p[0], p[1], p[2],
                    shipState.getPitchDegrees(),
                    shipState.getYawDegrees() + RENDER_YAW_OFFSET_DEG,
                    shipState.getRollDegrees());

            double cameraDistance = 7.4;
            double depth = rotated[2] + cameraDistance;
            double perspective = 260.0 / Math.max(1.0, depth);
            depthValues[i] = depth;

            int sx = centerX + (int) Math.round(rotated[0] * perspective);
            int sy = centerY - (int) Math.round(rotated[1] * perspective);
            projectedPoints[i] = new Point(sx, sy);
        }

        if (solidPlaneEnabled) {
            drawSolidShip(g2d, projectedPoints, depthValues);
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

        if (debugModeEnabled) {
            drawDebugShipLabels(g2d, projectedPoints);
        }

        if (firePrimaryActive) {
            double[] forward = rotateXYZ(0.0, 0.0, 1.0,
                    shipState.getPitchDegrees(),
                    shipState.getYawDegrees() + RENDER_YAW_OFFSET_DEG,
                    shipState.getRollDegrees());
            int frontX = centerX + (int) Math.round((forward[0] * 3.8) * 260.0 / Math.max(1.0, forward[2] + 7.4));
            int frontY = centerY - (int) Math.round((forward[1] * 3.8) * 260.0 / Math.max(1.0, forward[2] + 7.4));
            drawFirePrimaryEffect(g2d, nose, new Point(frontX, frontY));
        }
    }

    private void drawSolidShip(Graphics2D g2d, Point[] points, double[] depthValues) {
        List<Integer> faceOrder = new ArrayList<>();
        for (int i = 0; i < SHIP_FACES.length; i++) {
            faceOrder.add(i);
        }

        faceOrder.sort((a, b) -> Double.compare(avgDepth(SHIP_FACES[b], depthValues), avgDepth(SHIP_FACES[a], depthValues)));
        for (int faceIdx : faceOrder) {
            int[] face = SHIP_FACES[faceIdx];
            Polygon polygon = new Polygon();
            for (int vertex : face) {
                polygon.addPoint(points[vertex].x, points[vertex].y);
            }
            g2d.setColor(SHIP_FACE_COLORS[faceIdx % SHIP_FACE_COLORS.length]);
            g2d.fillPolygon(polygon);
        }
    }

    private static double avgDepth(int[] face, double[] depthValues) {
        double total = 0.0;
        for (int index : face) {
            total += depthValues[index];
        }
        return total / Math.max(1, face.length);
    }

    private void drawDebugShipLabels(Graphics2D g2d, Point[] projectedPoints) {
        Point nose = projectedPoints[0];
        Point tail = projectedPoints[9];
        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        g2d.setColor(new Color(255, 245, 120, 220));
        g2d.drawString("FRONT", nose.x + 8, nose.y - 8);
        g2d.setColor(new Color(255, 165, 120, 220));
        g2d.drawString("BACK", tail.x + 8, tail.y + 14);
    }

    private void drawFirePrimaryEffect(Graphics2D g2d, Point nose, Point projectileTarget) {
        int dx = projectileTarget.x - nose.x;
        int dy = projectileTarget.y - nose.y;
        double len = Math.max(1.0, Math.hypot(dx, dy));
        int fx = nose.x + (int) Math.round(dx / len * 28.0);
        int fy = nose.y + (int) Math.round(dy / len * 28.0);

        g2d.setColor(new Color(255, 90, 90, 130));
        g2d.fillOval(fx - 12, fy - 12, 24, 24);
        g2d.setColor(new Color(255, 45, 45, 220));
        g2d.fillOval(fx - 5, fy - 5, 10, 10);
        g2d.setColor(new Color(255, 170, 170, 210));
        g2d.drawLine(nose.x, nose.y, fx, fy);
    }

    private void drawHudText(Graphics2D g2d) {
        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        int x = 16;
        int y = 24;
        int lineHeight = 18;

        g2d.setColor(new Color(220, 240, 255));
        g2d.drawString(AppInfo.fullTitle(), x, y);

        g2d.setColor(new Color(170, 220, 255));
        g2d.drawString(String.format("Pitch: %7.2f°  (target %7.2f°)", shipState.getPitchDegrees(), shipState.getTargetPitchDegrees()), x, y + lineHeight);
        g2d.drawString(String.format("Yaw:   %7.2f°  (target %7.2f°)", shipState.getYawDegrees(), shipState.getTargetYawDegrees()), x, y + lineHeight * 2);
        g2d.drawString(String.format("Roll:  %7.2f°  (target %7.2f°)", shipState.getRollDegrees(), shipState.getTargetRollDegrees()), x, y + lineHeight * 3);
        g2d.drawString(String.format("Throttle: %6.1f%% (target %6.1f%%)", shipState.getThrottle() * 100.0, shipState.getTargetThrottle() * 100.0), x, y + lineHeight * 4);
        double mph = 100.0 + shipState.getThrottle() * 900.0;
        double displayMph = mph + (boostActive ? 500.0 : 0.0);
        g2d.drawString(String.format("Speed: %6.0f MPH", displayMph), x, y + lineHeight * 5);
        if (boostActive) {
            g2d.setColor(new Color(255, 220, 120));
            g2d.drawString("BOOST ACTIVE (+500 MPH)", x + 220, y + lineHeight * 5);
        }

        g2d.setColor(new Color(130, 190, 235));
        g2d.drawString("Flight Controls: Pitch=nose up/down | Yaw=nose left/right | Roll=bank left/right", x, y + lineHeight * 7);
        g2d.drawString("Keyboard: Arrows=Pitch/Yaw, Q/E=Roll, W/S=Throttle", x, y + lineHeight * 8);

        g2d.drawString("Preferred Input: " + preferredInputDevice + " | Active: " + activeInputDescription, x, y + lineHeight * 9);
        g2d.drawString("Joystick access: " + joystickSnapshot.accessStatus(), x, y + lineHeight * 10);
        g2d.drawString("T.16000M detected: " + (joystickSnapshot.thrustmasterT16000MDetected() ? "YES" : "NO") + " | Keyboard Active: " + (keyboardActive ? "YES" : "NO"), x, y + lineHeight * 11);
        g2d.drawString("Plane Render: " + (solidPlaneEnabled ? "Solid Retro Fill + Wireframe" : "Wireframe"), x, y + lineHeight * 12);
        g2d.drawString("Raw axes: " + formatAxes(joystickSnapshot.axes()), x, y + lineHeight * 13);
        if (debugModeEnabled) {
            g2d.setColor(new Color(255, 170, 140));
            g2d.drawString("DEBUG MODE", x, y + lineHeight * 14);
        }
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

        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        String controllerName = joystickSnapshot.connected() ? joystickSnapshot.controllerName() : "No controller";
        g2d.setColor(new Color(160, 210, 245));
        g2d.drawString(clipText(controllerName, 30), x, y + 16);

        List<Integer> buttonIndices = inputSystem != null
                ? inputSystem.getLogicalButtonIndices(joystickSnapshot)
                : extractButtonIndices(joystickSnapshot.buttons());

        if (buttonIndices.isEmpty()) {
            g2d.setColor(new Color(170, 190, 210));
            g2d.drawString("No digital buttons reported.", x, y + 42);
            return;
        }

        int top = y + 30;
        int cols = 5;
        int gap = 4;
        int cellWidth = (panelWidth - 24 - (cols - 1) * gap) / cols;
        int cellHeight = 22;

        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        for (int i = 0; i < buttonIndices.size(); i++) {
            int index = buttonIndices.get(i);
            int col = i % cols;
            int row = i / cols;
            int cellX = x + col * (cellWidth + gap);
            int cellY = top + row * (cellHeight + 6);

            if (cellY + cellHeight > panelY + panelHeight - 8) {
                g2d.setColor(new Color(170, 190, 210));
                g2d.drawString("...", x, panelY + panelHeight - 10);
                break;
            }

            boolean pressed = inputSystem != null
                    ? inputSystem.isLogicalButtonPressed(joystickSnapshot, index)
                    : isButtonIndexPressed(joystickSnapshot.buttons(), index);
            Color fill = pressed ? new Color(70, 200, 120, 210) : new Color(35, 55, 85, 180);
            Color border = pressed ? new Color(150, 255, 180, 230) : new Color(120, 170, 210, 140);

            g2d.setColor(fill);
            g2d.fillRoundRect(cellX, cellY, cellWidth, cellHeight, 8, 8);
            g2d.setColor(border);
            g2d.drawRoundRect(cellX, cellY, cellWidth, cellHeight, 8, 8);

            g2d.setColor(new Color(235, 245, 255));
            String label = inputSystem != null
                    ? inputSystem.getLogicalButtonLabel(joystickSnapshot, index)
                    : "B" + index;
            g2d.drawString(clipText(label, 8), cellX + 4, cellY + 15);
        }
    }

    private static String clipText(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, Math.max(0, max - 1)) + "…";
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
            resetStar(stars[i], i * 4099 + 17);
        }
        return stars;
    }

    private static void resetStar(double[] star, int seed) {
        long s1 = Integer.toUnsignedLong(seed) * 1664525L + 1013904223L;
        long s2 = s1 * 1664525L + 1013904223L;
        long s3 = s2 * 1664525L + 1013904223L;

        double nx = ((s1 & 0xFFFF) / 65535.0) * 2.0 - 1.0;
        double ny = ((s2 & 0xFFFF) / 65535.0) * 2.0 - 1.0;
        double depth = 0.22 + ((s3 & 0xFFFF) / 65535.0) * 1.28;

        star[0] = nx * 560.0;
        star[1] = ny * 400.0;
        star[2] = depth;
        star[3] = seed;
    }

    private static double[] rotateXYZ(double x, double y, double z, double pitchDeg, double yawDeg, double rollDeg) {
        double pitch = Math.toRadians(pitchDeg);
        double yaw = Math.toRadians(yawDeg);
        double roll = Math.toRadians(rollDeg);

        double cosy = Math.cos(yaw);
        double siny = Math.sin(yaw);
        double x1 = x * cosy + z * siny;
        double y1 = y;
        double z1 = -x * siny + z * cosy;

        double cosp = Math.cos(pitch);
        double sinp = Math.sin(pitch);
        double x2 = x1;
        double y2 = y1 * cosp - z1 * sinp;
        double z2 = y1 * sinp + z1 * cosp;

        double cosr = Math.cos(roll);
        double sinr = Math.sin(roll);
        double x3 = x2 * cosr - y2 * sinr;
        double y3 = x2 * sinr + y2 * cosr;
        double z3 = z2;

        return new double[]{x3, y3, z3};
    }

    private static double shortestAngleDelta(double from, double to) {
        double delta = (to - from) % 360.0;
        if (delta > 180.0) {
            delta -= 360.0;
        } else if (delta < -180.0) {
            delta += 360.0;
        }
        return delta;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
