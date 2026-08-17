# Ecosystem review

Accessed **2026-08-17**. Claims below are tied to primary pages or repositories. Interpretations are labeled **inference**.

## FRC systems

### AdvantageKit (Team 6328)

AdvantageKit records data flowing *into* robot code so the full program can be replayed in simulation from a log ([What is AdvantageKit?](https://docs.advantagekit.org/getting-started/what-is-advantagekit/), [GitHub](https://github.com/Mechanical-Advantage/AdvantageKit)). Hardware interaction is separated into an IO layer. AdvantageScope does **not** require AdvantageKit ([AdvantageScope README](https://github.com/Mechanical-Advantage/AdvantageScope)).

**Inference:** this is the strongest published teaching model for input/output split and deterministic replay. It is WPILib/FRC-shaped and not a drop-in Control Hub library.

### AdvantageScope

Reads WPILOG, DS logs, Hoot, REVLOG, Road Runner `.log`, CSV, and RLOG; live NT4, Phoenix, RLOG, or FTC Dashboard streaming ([log files](https://docs.advantagescope.org/overview/log-files/), [home](https://docs.advantagescope.org/)). Includes graphs, 2D/3D field, mechanism views, and synchronized external match video.

**Inference:** TRACE should export into this ecosystem rather than invent a dashboard.

### WPILib DataLog

On-robot binary logs; file I/O on a separate thread; mutex + copy on the robot thread ([datalog.html](https://docs.wpilib.org/en/stable/docs/software/telemetry/datalog.html)). `DataLogManager` rotates/cleans files and can record NetworkTables. Joystick logging is opt-in via `DriverStation.startDataLog`.

**Inference:** TRACE’s async writer and quotas follow this *pattern*, not the WPILib classes (which target roboRIO paths and USB FAT32 sticks).

### WPILib Epilogue (2025 annotation logging)

`@Logged` generates logging code; optional `DataLogManager.start()` mirrors NT to disk ([robot-telemetry-with-annotations](https://docs.wpilib.org/en/stable/docs/software/telemetry/robot-telemetry-with-annotations.html)). Importance levels can drop DEBUG fields. Nulls cannot be logged. CAN queries can cause loop overruns.

**Inference:** compile-time annotation logging is powerful but too FRC/WPILib-specific and too “log everything” for TRACE’s beginner onramp.

### DogLog (Team 581)

Thin API over WPILib `DataLogManager` and NT4 ([doglog.dev](https://doglog.dev/), [FAQ](https://doglog.dev/getting-started/faq), [GitHub](https://github.com/jonahsnider/doglog)). FAQ contrasts DogLog (simplest logging), AdvantageKit (replay), and Epilogue (annotate all fields).

**Inference:** DogLog’s `DogLog.log("Arm/Position", value)` inspired TRACE’s beginner `Trace.record` naming, but DogLog cannot run without WPILib.

### NetworkTables and Driver Station logs

NT is a pub/sub telemetry bus ([NT intro](https://docs.wpilib.org/en/stable/docs/software/networktables/networktables-intro.html)). The FRC DS writes `.dslog` / `.dsevents` under `C:\Users\Public\Documents\FRC\Log Files` ([DS log viewer](https://docs.wpilib.org/en/stable/docs/software/driverstation/driver-station-log-viewer.html)).

FTC has no equivalent first-party DS binary flight recorder of this richness. **Inference:** TRACE events should cover the “what went wrong” role DS event logs play in FRC.

### CTRE Phoenix 6 Signal Logger and REV StatusLogger

Phoenix 6 writes `.hoot` with CAN timestamps independent of the main loop ([Signal Logging](https://v6.docs.ctr-electronics.com/en/latest/docs/api-reference/api-usage/signal-logging.html)). AdvantageScope opens Hoot after a CTRE EULA prompt and REVLOG from REV StatusLogger ([log files](https://docs.advantagescope.org/overview/log-files/)).

**Inference:** vendor logs are high-fidelity *device* traces. TRACE is a *program* flight recorder. Do not duplicate Hoot/REVLOG; optionally correlate later.

## FTC systems

### Official FTC Datalogger

Wiki sample writes CSV/text under `/sdcard/FIRST/java/src/Datalogs` for OnBot Java download and spreadsheet charting ([Datalogging wiki](https://github.com/FIRST-Tech-Challenge/FtcRobotController/wiki/Datalogging), [Datalogger.java](https://github.com/FIRST-Tech-Challenge/WikiSupport/blob/master/SampleOpModes/Datalogging/Datalogger.java)). Timestamp is taken at `writeLine()`. Fields are text.

**Inference:** excellent teaching CSV; not a bounded binary flight recorder; writing into OnBot source dirs is a defect TRACE should not copy.

### FTC SDK telemetry and Control Hub logs

OpModes send Driver Station telemetry via `telemetry.addData` ([FTC docs OpMode tutorial](https://ftc-docs.firstinspires.org/en/latest/programming_resources/tutorial_specific/android_studio/creating_op_modes/Creating-and-Running-an-Op-Mode-(Android-Studio).html)). Control Hub `robotControllerLog.txt` is downloadable from the Manage page ([Managing a Control Hub](https://ftc-docs.firstinspires.org/en/latest/programming_resources/shared/managing_control_hub/Managing-a-Control-Hub.html)).

**Inference:** DS telemetry is live and lossy; RC logs are unstructured. TRACE must not block the loop the way naive per-line file writes would.

### FTC Dashboard

Telemetry packets with key/value data and field overlays; `MultipleTelemetry` combines DS and dashboard ([features](https://acmerobotics.github.io/ftc-dashboard/features.html)). AdvantageScope can stream FTC Dashboard live ([AdvantageScope home](https://docs.advantagescope.org/)).

**Inference:** live path should reuse Dashboard/AdvantageScope, not a new web UI.

### Road Runner Flight Recorder

Quickstart logs every run in a binary channel/schema format; recent logs at `http://192.168.43.1:8080/logs` ([RR log files](https://rr.brott.dev/docs/v1-0/log-files/)). AdvantageScope reads Road Runner `.log` natively. `FlightRecorder.write(channel, message)` appears throughout the quickstart ([MecanumDrive](https://github.com/acmerobotics/road-runner-quickstart/blob/5f35f4c22c1ae7c0be5b35da0961c8f3a181ad31/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/MecanumDrive.java)).

**Fact:** this is the most AdvantageScope-native *FTC file* format today.

**Inference:** TRACE Phase 5 should consider RR or WPILOG export. Using RR as the *only* core would inherit RR schemas and not TRACE categories/priorities.

### FateWeaver

Road Runner-log based FTC logger with typed channels and a download dashboard ([HermesFTC/FateWeaver](https://github.com/HermesFTC/FateWeaver)). Implements `Telemetry` by logging strings unless typed channels are used.

### Pedro Pathing

Live pose/path visualization via Panels or FTC Dashboard; not a match flight recorder ([dashboard docs](https://pedropathing.com/docs/pathing/dashboard), [install](https://pedropathing.com/docs/pathing/installation)). Telemetry artifact `com.pedropathing:telemetry`.

**Inference:** TRACE should record Pedro outputs through an optional adapter, not replace Pedro telemetry.

### PsiKit

AdvantageKit port for FTC ([GitHub](https://github.com/PsiLynx/PsiKit), [docs](https://psilynx.github.io/PsiKit/#/)). License is the AdvantageKit BSD-style text (Littleton Robotics; no endorsement using AdvantageKit names). Last push 2026-07-07; Java 8 / compileSdk 30 commit. Source includes `RLOGWriter`, `RLOGReplay`, FTC hardware wrappers, `FtcLoggingSession`, and replay docs describing `.rlog` under `/storage/emulated/0/FIRST/PsiKit` ([replay.md](https://raw.githubusercontent.com/PsiLynx/PsiKit/main/docs/replay.md)).

**Stale-docs fact:** the homepage still lists “log files” and “log replay” as *Coming Soon* while `docs/replay.md` and source already describe them. TRACE must not treat the homepage as the capability matrix.

**Inference:** PsiKit is the closest full-stack FTC replay system, but it is AdvantageKit-shaped (wrappers, RLOG, Kotlin FTC module) and still beta-scale (13 stars, 0 forks at access time). See [build-vs-adopt.md](build-vs-adopt.md).

### AdvantageScope Lite for FTC

Installs AdvantageScope on the robot for live FTC Dashboard viewing and Road Runner / PsiKit log replay ([j5155/AdvantageScope-Lite-FTC](https://github.com/j5155/AdvantageScope-Lite-FTC)). Small project; useful as a visualization deployment option, not a recorder.

## Control Hub constraints (documented vs inferred)

**Documented:** RC is Android; files live on device storage; logs can be pulled via Manage page or adb ([Control Hub manage](https://ftc-docs.firstinspires.org/en/latest/programming_resources/shared/managing_control_hub/Managing-a-Control-Hub.html), [Datalogging Part 3](https://github.com/FIRST-Tech-Challenge/FtcRobotController/wiki/Datalogging-Part-3,-RC-File-Transfer)).

**Inference:** eMMC is small relative to FRC USB logging sticks; writers must bound size, avoid fsync-per-record, and survive sudden power loss. WPILib’s “I/O on another thread” guidance still applies ([datalog.html](https://docs.wpilib.org/en/stable/docs/software/telemetry/datalog.html)).
