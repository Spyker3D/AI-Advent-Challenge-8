# Smoke environment

- Run ID: `2026-07-26T204039-feature-update`
- Tested revision: `8b3da64`
- Branch: `day_38_new_feature`
- Device ID: `emulator-5554`
- Device reported model: `sdk_gphone64_x86_64` (Pixel 6 AVD supplied by coordinator)
- Android version: `16`
- Orientation: portrait (`1080x2400`)
- Application package: `com.aiassistant`
- Launcher activity: `com.aiassistant.MainActivity`
- Build variant: `debug`
- Installed package path was confirmed with `pm path com.aiassistant`.
- Scenario source: `.codex/smoke/scenarios.md`
- Evidence was created in a new directory and did not overwrite an earlier run.

UI actions and assertions were performed through the configured mobile MCP
against the explicit target `emulator-5554`. Screenshots and Android UI
hierarchy XML were pulled from that same target into this evidence directory.
