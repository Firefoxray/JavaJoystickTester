package com.fire.javajoysticktester.model;

/**
 * Mutable model for ship orientation and throttle.
 *
 * Stores both target values (what input requests) and smoothed values
 * (what the renderer reads). This keeps input and rendering concerns separate.
 */
public class ShipState {
    private static final double MAX_PITCH_DEGREES = 70.0;
    private static final double MAX_ROLL_DEGREES = 85.0;

    private static final double ORIENTATION_SMOOTHING = 9.0;
    private static final double THROTTLE_SMOOTHING = 6.0;

    private double pitchDegrees;
    private double yawDegrees;
    private double rollDegrees;
    private double throttle;

    private double targetPitchDegrees;
    private double targetYawDegrees;
    private double targetRollDegrees;
    private double targetThrottle;

    public double getPitchDegrees() {
        return pitchDegrees;
    }

    public double getYawDegrees() {
        return yawDegrees;
    }

    public double getRollDegrees() {
        return rollDegrees;
    }

    public double getThrottle() {
        return throttle;
    }

    public double getTargetPitchDegrees() {
        return targetPitchDegrees;
    }

    public double getTargetYawDegrees() {
        return targetYawDegrees;
    }

    public double getTargetRollDegrees() {
        return targetRollDegrees;
    }

    public double getTargetThrottle() {
        return targetThrottle;
    }

    public void setPitchTargetDegrees(double pitchDegrees) {
        targetPitchDegrees = clamp(pitchDegrees, -MAX_PITCH_DEGREES, MAX_PITCH_DEGREES);
    }

    public void setYawTargetDegrees(double yawDegrees) {
        targetYawDegrees = wrapAngle(yawDegrees);
    }

    public void setRollTargetDegrees(double rollDegrees) {
        targetRollDegrees = clamp(rollDegrees, -MAX_ROLL_DEGREES, MAX_ROLL_DEGREES);
    }

    public void setThrottleTarget(double throttle) {
        targetThrottle = clamp(throttle, 0.0, 1.0);
    }

    public void addPitchTarget(double deltaDegrees) {
        targetPitchDegrees = clamp(targetPitchDegrees + deltaDegrees, -MAX_PITCH_DEGREES, MAX_PITCH_DEGREES);
    }

    public void addYawTarget(double deltaDegrees) {
        targetYawDegrees = wrapAngle(targetYawDegrees + deltaDegrees);
    }

    public void addRollTarget(double deltaDegrees) {
        targetRollDegrees = clamp(targetRollDegrees + deltaDegrees, -MAX_ROLL_DEGREES, MAX_ROLL_DEGREES);
    }

    public void addThrottleTarget(double delta) {
        targetThrottle = clamp(targetThrottle + delta, 0.0, 1.0);
    }

    public void nudgePitchTargetTowardCenter(double amount) {
        targetPitchDegrees = moveToward(targetPitchDegrees, 0.0, amount);
    }

    public void nudgeYawTargetTowardCenter(double amount) {
        targetYawDegrees = moveTowardAngle(targetYawDegrees, 0.0, amount);
    }

    public void nudgeRollTargetTowardCenter(double amount) {
        targetRollDegrees = moveToward(targetRollDegrees, 0.0, amount);
    }

    /**
     * Advance smoothed values toward targets.
     */
    public void update(double deltaTimeSec) {
        double orientationAlpha = smoothingAlpha(ORIENTATION_SMOOTHING, deltaTimeSec);
        double throttleAlpha = smoothingAlpha(THROTTLE_SMOOTHING, deltaTimeSec);

        pitchDegrees = lerp(pitchDegrees, targetPitchDegrees, orientationAlpha);
        yawDegrees = lerpAngle(yawDegrees, targetYawDegrees, orientationAlpha);
        rollDegrees = lerp(rollDegrees, targetRollDegrees, orientationAlpha);
        throttle = lerp(throttle, targetThrottle, throttleAlpha);
    }

    private static double smoothingAlpha(double rate, double deltaTimeSec) {
        return 1.0 - Math.exp(-rate * Math.max(0.0, deltaTimeSec));
    }

    private static double lerp(double current, double target, double alpha) {
        return current + (target - current) * alpha;
    }

    private static double lerpAngle(double currentDeg, double targetDeg, double alpha) {
        double delta = shortestAngleDelta(currentDeg, targetDeg);
        return wrapAngle(currentDeg + delta * alpha);
    }

    private static double moveToward(double value, double target, double maxStep) {
        double delta = target - value;
        if (Math.abs(delta) <= maxStep) {
            return target;
        }
        return value + Math.signum(delta) * maxStep;
    }

    private static double moveTowardAngle(double current, double target, double maxStep) {
        double delta = shortestAngleDelta(current, target);
        if (Math.abs(delta) <= maxStep) {
            return wrapAngle(target);
        }
        return wrapAngle(current + Math.signum(delta) * maxStep);
    }

    private static double shortestAngleDelta(double from, double to) {
        return wrapAngle(to - from);
    }

    private static double wrapAngle(double angle) {
        double wrapped = angle % 360.0;
        return wrapped < -180.0 ? wrapped + 360.0 : (wrapped > 180.0 ? wrapped - 360.0 : wrapped);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
