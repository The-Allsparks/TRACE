package org.allsparks.trace.core;

/**
 * TRACE distinguishes three evidence categories.
 *
 * <p><b>INPUT</b> — facts entering the program from nondeterministic sources
 * (sensors, gamepads, files, network, cameras, randomness). Replay-essential
 * inputs must be captured before decision logic consumes them.
 *
 * <p><b>OUTPUT</b> — values calculated or commanded by the program (motor
 * commands, estimated pose, setpoints, state-machine states).
 *
 * <p><b>EVENT</b> — sparse semantic occurrences (autonomous started, interlock
 * activated, loop overrun, exception).
 */
public enum RecordCategory {
    INPUT((byte) 1),
    OUTPUT((byte) 2),
    EVENT((byte) 3),
    /** Accounting record describing dropped data; never silently omitted. */
    DROP((byte) 4);

    private final byte wireId;

    RecordCategory(byte wireId) {
        this.wireId = wireId;
    }

    public byte wireId() {
        return wireId;
    }

    public static RecordCategory fromWire(byte id) {
        switch (id) {
            case 1:
                return INPUT;
            case 2:
                return OUTPUT;
            case 3:
                return EVENT;
            case 4:
                return DROP;
            default:
                throw new IllegalArgumentException("Unknown category id: " + id);
        }
    }
}
