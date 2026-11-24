# AKRAM TELE upstream overlay / patch system

This repository currently stores build configuration rather than the full Telegram Android source.
The build downloads upstream at build time. AKRAM-owned modifications must therefore be stored here
and reapplied on each build, never made only inside the ephemeral cloned directory.

- `patches/*.patch`: version-sensitive upstream modifications. The script checks and applies each patch.
- `overlay/`: additive AKRAM source files copied into the matching upstream relative path.

The workflow invokes `scripts/apply-akram-overlay.sh` after cloning and before building.
Build success is not proof of runtime correctness; each feature must still be tested on-device.
