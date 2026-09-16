---
name: tlog-advantagescope
description: >-
  Pull TRACE .tlog files from the Control Hub, convert to WPILOG, and open
  AdvantageScope. Use when the user asks to pull TRACE, convert logs, open
  AdvantageScope, review loop time, battery, AMPER, or Drive graphs, or after
  a shop run that completed without an RC crash.
---

# TRACE pull to AdvantageScope

After a Drive run, the evidence is `/sdcard/FIRST/trace/*.tlog`. Convert on a laptop. Do not treat `.wpilog` as the Hub record. TRACE will not ship a competing dashboard.

This is **not** crash diagnosis. If the OpMode or RC died, use `ftc-crash-diagnose` first.

## Defaults

| Item | Value |
| --- | --- |
| Hub serial | from MCP `devices`, often `192.168.43.1:5555` |
| Hub folder | `/sdcard/FIRST/trace` |
| Laptop dest | `logs/hub-YYYY-MM-DD-HHmm/trace/` next to the Allsparks workspace (do not commit) |
| Converter | `TRACE/build/libs/trace-0.1.0-SNAPSHOT.jar` via `TraceInspect` |
| AdvantageScope | `%LOCALAPPDATA%\Programs\advantagescope\AdvantageScope.exe` |

Ask the driver to **STOP** first. `TeamTrace.stop()` flushes the writer. Yanking power leaves a truncated `.tlog` that is still a readable prefix.

## Workflow

```
Task Progress:
- [ ] 1. Connect and confirm STOP
- [ ] 2. adb pull /sdcard/FIRST/trace
- [ ] 3. Convert newest complete .tlog with --wpilog
- [ ] 4. Open AdvantageScope on that .wpilog
- [ ] 5. Point at the signals that answer the question
```

### 1. Connect

Same Hub as deploy: MCP `devices` / `hub_status`. If ADB is down, reconnect (`adb connect 192.168.43.1:5555`). Console download does **not** serve `.tlog` files; ADB is required for TRACE.

If the Hub is off, use the newest `logs/hub-*/trace/` pull and say it is not live.

### 2. Pull

```powershell
$stamp = Get-Date -Format "yyyy-MM-dd-HHmm"
$dir = "<workspace>\logs\hub-$stamp"
New-Item -ItemType Directory -Force -Path $dir | Out-Null
adb -s 192.168.43.1:5555 pull /sdcard/FIRST/trace "$dir\trace"
```

If `trace` is missing, INIT never opened a file (`File.toPath()` / `TRACE unavailable`). That is a crash/config problem, not a converter problem.

### 3. Convert

Newest `.tlog` by write time. Build the fat jar if needed:

```powershell
Push-Location <workspace>\TRACE
.\gradlew.bat -q jar
Pop-Location
java -jar <workspace>\TRACE\build\libs\trace-0.1.0-SNAPSHOT.jar <file.tlog> --wpilog
```

Print `complete`, `corrupt`, `truncatedBytes` from TraceInspect stdout. Prefer a **complete** session. `--as-csv` is lossy; do not open CSV in AdvantageScope as the primary view.

### 4. Open

```powershell
Start-Process -FilePath "$env:LOCALAPPDATA\Programs\advantagescope\AdvantageScope.exe" -ArgumentList "`"<file.wpilog>`""
```

Tell the user the session id and which file is open.

### 5. What to look at

Shop questions map to these keys (leading `/` in AdvantageScope):

| Question | Signals |
| --- | --- |
| Loop too slow | `/TRACE/Loop/Duration`, `/TRACE/Loop/Overrun` |
| AMPER cost | `/AMPER/**`, compare a Drive-only session vs AMPER-on |
| Pack sag | `/AMPER/Battery/Voltage`, `/AMPER/Battery/FilteredVoltage` |
| Sticks vs wheels | `/SHIFT/**`, `/Drive/Command/**`, `/Drive/Boost` |
| Recording actually on | `TRACE.bytesWritten` on DS; file size > header |

Live AdvantageScope streaming is **not** this skill. `.tlog` stays canonical.

## Do not

- Convert on the Hub
- Replace `.tlog` with WPILOG on the robot
- Clear `/sdcard/FIRST/trace` unless the user asked
- Treat a missing folder as “no interesting data” without checking INIT/`TeamTrace`
