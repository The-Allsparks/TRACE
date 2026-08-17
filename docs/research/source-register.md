# Source register

All rows accessed **2026-08-17**.

| Source | Maintainer or organization | URL | Relevant claim | How TRACE uses the information | Stability or maturity concern |
|--------|----------------------------|-----|----------------|--------------------------------|-------------------------------|
| AdvantageKit docs: What is AdvantageKit? | FRC 6328 / Littleton Robotics | https://docs.advantagekit.org/getting-started/what-is-advantagekit/ | Records inputs for deterministic sim replay; IO layer | Input/output split, replay teaching model | FRC/WPILib; not FTC runtime |
| AdvantageKit GitHub | FRC 6328 | https://github.com/Mechanical-Advantage/AdvantageKit | Open-source logging & replay for FRC | Build-vs-adopt baseline | Vendor-agnostic in FRC, not portable as-is |
| AdvantageScope home | FRC 6328 | https://docs.advantagescope.org/ | Visualizer; AdvantageKit not required; FTC Dashboard live | Preferred visualization; no competing dashboard | Desktop app; not a recorder |
| AdvantageScope log files | FRC 6328 | https://docs.advantagescope.org/overview/log-files/ | Formats: WPILOG, DS, Hoot, REVLOG, Road Runner, CSV, RLOG | Phase 5 export targets; CSV list format | CSV is lossy |
| WPILib DataLog | WPILib | https://docs.wpilib.org/en/stable/docs/software/telemetry/datalog.html | Separate I/O thread; binary logs; DataLogManager quotas | Async writer, quotas, no per-record flush | roboRIO paths; FPGA timestamps |
| WPILib Epilogue | WPILib | https://docs.wpilib.org/en/stable/docs/software/telemetry/robot-telemetry-with-annotations.html | `@Logged` codegen; importance levels; nulls illegal | Priority/importance analogy; avoid “log everything” | Java annotation processor; FRC-only |
| DogLog docs / FAQ | Team 581 / Jonah Snider | https://doglog.dev/ and https://doglog.dev/getting-started/faq | Simple `DogLog.log`; wraps DataLogManager | Beginner API shape | WPILib-only |
| DogLog GitHub | jonahsnider | https://github.com/jonahsnider/doglog | Unopinionated FRC logger | API comparison | FRC vendordep |
| NetworkTables intro | WPILib | https://docs.wpilib.org/en/stable/docs/software/networktables/networktables-intro.html | Pub/sub live telemetry | Live vs recorded distinction | Not on FTC DS |
| Driver Station Log Viewer | WPILib / FIRST | https://docs.wpilib.org/en/stable/docs/software/driverstation/driver-station-log-viewer.html | `.dslog`/`.dsevents` diagnostics | Event category motivation | FRC DS only |
| CTRE Signal Logging | CTR Electronics | https://v6.docs.ctr-electronics.com/en/latest/docs/api-reference/api-usage/signal-logging.html | Hoot logs CAN signals off the main loop | Do not duplicate vendor logs | EULA; FRC Phoenix 6 |
| AdvantageScope Hoot/REVLOG note | FRC 6328 | https://docs.advantagescope.org/overview/log-files/ | Hoot and REVLOG supported | Correlation, not adoption | Vendor formats |
| FTC Datalogging wiki | FIRST Tech Challenge / Westside Robotics | https://github.com/FIRST-Tech-Challenge/FtcRobotController/wiki/Datalogging | Official CSV teaching sample | Phase 2 CSV; avoid OnBot source dir | Wiki last edited 2022 |
| Datalogger.java | FIRST-Tech-Challenge/WikiSupport | https://github.com/FIRST-Tech-Challenge/WikiSupport/blob/master/SampleOpModes/Datalogging/Datalogger.java | Buffered CSV writer; `/sdcard/FIRST/java/src/Datalogs` | Negative example for storage location | Sample, not SDK API |
| FTC Control Hub manage | FIRST | https://ftc-docs.firstinspires.org/en/latest/programming_resources/shared/managing_control_hub/Managing-a-Control-Hub.html | Download `robotControllerLog.txt` | Unstructured logs are insufficient | Android RC |
| FTC Android Studio OpMode tutorial | FIRST | https://ftc-docs.firstinspires.org/en/latest/programming_resources/tutorial_specific/android_studio/creating_op_modes/Creating-and-Running-an-Op-Mode-(Android-Studio).html | `telemetry.addData` live DS telemetry | Adapter interface, not a dependency | Live only |
| FTC Dashboard features | ACME Robotics | https://acmerobotics.github.io/ftc-dashboard/features.html | Packets, field overlay, MultipleTelemetry | Optional live adapter | Extra robot CPU/network |
| Road Runner log files | Ryan Brott / Road Runner | https://rr.brott.dev/docs/v1-0/log-files/ | Binary channel/schema logs; hub web `/logs` | Phase 5 candidate export | RR-specific schemas |
| road-runner-quickstart MecanumDrive | ACME Robotics | https://github.com/acmerobotics/road-runner-quickstart/blob/5f35f4c22c1ae7c0be5b35da0961c8f3a181ad31/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/MecanumDrive.java | `FlightRecorder.write` usage | Evidence RR already records drive inputs | TeamCode sample |
| Pedro Pathing dashboard | Pedro Pathing maintainers | https://pedropathing.com/docs/pathing/dashboard | Live Panels or FTC Dashboard | TRACE records Pedro; does not replace it | Live tuning differs by dashboard |
| Pedro Pathing install | Pedro Pathing | https://pedropathing.com/docs/pathing/installation | `com.pedropathing:telemetry` dependency | Adapter version pin later | Android Studio required |
| PsiKit GitHub | PsiLynx | https://github.com/PsiLynx/PsiKit | AdvantageKit port for FTC | Primary adopt candidate | Beta; BSD AdvantageKit license |
| PsiKit homepage | PsiLynx | https://psilynx.github.io/PsiKit/#/ | Lists file logging/replay as coming soon | **Do not trust over source** | Stale relative to repo |
| PsiKit replay.md | PsiLynx | https://github.com/PsiLynx/PsiKit/blob/main/docs/replay.md | RLOGWriter, `.rlog` path, replay workflow | Accurate capability check | Docs split from homepage |
| PsiKit changelog | PsiLynx | https://github.com/PsiLynx/PsiKit/blob/main/docs/changelog.md | Autolog opt-in; bulk-only defaults | Student-safety lesson: no auto-activation | Rapid beta churn |
| AdvantageScope Lite FTC | j5155 | https://github.com/j5155/AdvantageScope-Lite-FTC | On-robot AdvantageScope; RR/PsiKit logs | Visualization deployment option | Small, unofficial |
| FateWeaver | HermesFTC | https://github.com/HermesFTC/FateWeaver | RR-log FTC logger + download UI | Alternative file format family | Community project |
| AMPER repository | The Allsparks | https://github.com/The-Allsparks/AMPER | Org Java library conventions | TRACE scaffold source | Sibling project |
| MIMIC repository | The Allsparks | https://github.com/The-Allsparks/MIMIC | Mechanism framework; Java 11; MIT | Boundary: TRACE does not command mechanisms | Early scaffold |
| ViDAR repository | The Allsparks | https://github.com/The-Allsparks/ViDAR | Perception project | Boundary: no raw video in TRACE by default | Mixed language repo |
| FtcRobotController | FIRST | https://github.com/FIRST-Tech-Challenge/FtcRobotController | Current FTC SDK host | Compatibility target without compile coupling | Season cadence |
