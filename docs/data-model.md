# Data model

TRACE uses three categories. Mixing them makes replay and teaching harder.

## Inputs

Facts entering the program from nondeterministic sources:

* Gamepad state
* Encoders, IMU, digital/analog sensors
* Battery voltage and motor-controller observations
* Camera observations and detection timestamps
* Communication health
* Dashboard or operator configuration
* Filesystem or network values
* Randomness that affects behavior
* External timestamps

**Replay rule (Phase 6+):** capture inputs *before* decision logic consumes them.

## Outputs

Values calculated or commanded by the program:

* Motor power, velocity, current, or voltage commands
* Servo positions and mechanism goals
* Estimated pose and fused targets
* Controller setpoints and power allocations
* Safe-state decisions and path-following outputs
* Internal state-machine states

Outputs are how students see *what the software believed and did*. They are not sufficient for deterministic replay by themselves.

## Events

Sparse semantic occurrences:

* OpMode initialized / autonomous started / path started
* Target acquired, mechanism calibrated, limit reached
* Interlock or brownout protection activated
* Communications degraded or recovered
* Safe state entered
* Exception thrown, loop overrun, data dropped
* Session stopped

Events reconstruct the *story*. Signals reconstruct the *plot*.

## Why the split matters

| Question | Category |
|----------|----------|
| What did the world tell the robot? | Input |
| What did the robot decide or command? | Output |
| When did a meaningful transition happen? | Event |

The simple API `Trace.record(name, value)` records an **output** so beginners can graph values without learning IO boundaries yet. Hardware facts should move to `Trace.recordInput` when students are ready. That is intentional scaffolding, not hidden reclassification.

## Quality

`OK`, `STALE`, `ESTIMATED`, `INVALID`, `MISSING`, `ASYNC` describe trust. Asynchronous vision results must be timestamped and transferred into a cycle snapshot before decision logic uses them (Phase 6). Until then, worker-thread records should be marked `ASYNC` if they are accepted at all.
