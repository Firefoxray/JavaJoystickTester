package com.fire.javajoysticktester.model;

/**
 * Mutable model for ship orientation and throttle.
 *
 * This class is intentionally independent from input/render layers so we can
 * later drive it from joystick input without changing drawing code.
 */
public class ShipState {
    private double pitchDegrees;
    private double yawDegrees;
    private double rollDegrees;
    private double throttle;

    public double getPitchDegrees() {
        return pitchDegrees;
    }

    public void setPitchDegrees(double pitchDegrees) {
        this.pitchDegrees = pitchDegrees;
    }

    public double getYawDegrees() {
        return yawDegrees;
    }

    public void setYawDegrees(double yawDegrees) {
        this.yawDegrees = yawDegrees;
    }

    public double getRollDegrees() {
        return rollDegrees;
    }

    public void setRollDegrees(double rollDegrees) {
        this.rollDegrees = rollDegrees;
    }

    public double getThrottle() {
        return throttle;
    }

    public void setThrottle(double throttle) {
        this.throttle = clamp(throttle, 0.0, 1.0);
    }

    public void addPitch(double deltaDegrees) {
        pitchDegrees = wrapAngle(pitchDegrees + deltaDegrees);
    }

    public void addYaw(double deltaDegrees) {
        yawDegrees = wrapAngle(yawDegrees + deltaDegrees);
    }

    public void addRoll(double deltaDegrees) {
        rollDegrees = wrapAngle(rollDegrees + deltaDegrees);
    }

    public void addThrottle(double delta) {
        throttle = clamp(throttle + delta, 0.0, 1.0);
    }

    private static double wrapAngle(double angle) {
        double wrapped = angle % 360.0;
        return wrapped < -180.0 ? wrapped + 360.0 : (wrapped > 180.0 ? wrapped - 360.0 : wrapped);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
