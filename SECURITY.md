# Security Policy

## Supported versions

| Version | Supported |
| ------- | --------- |
| 0.1.x   | Yes       |

## Reporting a vulnerability

Please do **not** open a public issue for security problems that could put robots, students, or machines at risk.

Prefer:

1. GitHub Security Advisories for this repository (when available), or
2. A private email contact published by the maintainers

Include:

- A description of the issue
- Steps to reproduce
- Impact assessment (for example: unexpected motor motion, credential exposure, unrestricted deserialization, log files containing secrets)

## Safety expectations for this project

TRACE intentionally:

- Remains **observational** in Phases 0–5
- **Never energizes physical outputs** as part of recording
- Treats `TraceMode.REPLAY` as unimplemented and **fails closed** if selected
- Avoids storing raw camera video, student-identifying information, Wi-Fi passwords, tokens, or secrets
- Parses TRACE logs with a bounded, purpose-built reader rather than Java deserialization

If you discover a path that commands hardware, enables replay without output isolation, or writes credentials into a log, treat it as a safety defect.

## Secrets

Never store passwords, Wi-Fi credentials, API keys, or tokens in the repository, issues, or exported logs. Sanitize file names and paths.
