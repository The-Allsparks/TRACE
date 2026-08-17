package org.allsparks.trace.examples;

/**
 * Simulated subsystem used to teach the hardware vs decision-logic boundary.
 * Physical outputs are not energized; this is a desktop model only.
 */
public final class SimulatedDrive {
    private double commandedPower;
    private double poseX;

    public double readEncoder() {
        return poseX;
    }

    public void setPower(double power) {
        commandedPower = power;
        poseX += power * 0.02;
    }

    public double commandedPower() {
        return commandedPower;
    }
}
