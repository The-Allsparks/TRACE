# Contributing to TRACE

TRACE is maintained by [The Allsparks](https://github.com/The-Allsparks) (FTC Team 36117) for our team and the wider FTC community.

## Setup

```powershell
git clone https://github.com/The-Allsparks/TRACE.git
cd TRACE
.\gradlew.bat test
```

## Rules of engagement

1. **Do not command motors, servos, or mechanism states.** Phases 0–5 are observational.
2. Do not implement or enable `TraceMode.REPLAY` without passing the replay approval gate.
3. Do not add ViDAR, Pedro, AMPER, MIMIC, or BEACON compile dependencies to this repository.
4. Distinguish **verified fact**, **engineering inference**, and **untested hypothesis** in documentation.
5. Never describe an FRC capability as a current FTC capability without a primary source.
6. Do not commit secrets, Wi-Fi passwords, tokens, or student PII.
7. If records are dropped, the drop must be counted with a reason. Silent discard is a bug.

## Pull requests

- Prefer small, reviewable PRs.
- Include motivation, phase impact, test evidence, and safety notes.
- Update docs when behavior or maturity labels change.
- Run `.\gradlew.bat check` (or `./gradlew check`) before requesting review.
- Use `Closes #<issue>` only when the PR fully resolves that issue.
- Keep architecture/validation work in **draft** PRs until the relevant gate passes.

## Line endings

The repository stores LF line endings (see [.gitattributes](.gitattributes)).

## License

Contributions are accepted under the MIT License ([LICENSE](LICENSE)). No CLA is required.
