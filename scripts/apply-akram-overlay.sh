#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TG="$ROOT/Telegram"

# Apply tracked AKRAM TELE patch files in lexical order.
PATCH_DIR="$ROOT/patches"
if [[ -d "$PATCH_DIR" ]]; then
  while IFS= read -r -d '' patch_file; do
    echo "Applying patch: $patch_file"
    git -C "$TG" apply --check "$patch_file"
    git -C "$TG" apply "$patch_file"
  done < <(find "$PATCH_DIR" -type f -name '*.patch' -print0 | sort -z)
fi

# Copy additive source overlays (safe: these paths are AKRAM-owned files).
if [[ -d "$ROOT/overlay" ]]; then
  while IFS= read -r -d '' source_file; do
    relative="${source_file#"$ROOT/overlay/"}"
    target="$TG/$relative"
    mkdir -p "$(dirname "$target")"
    cp "$source_file" "$target"
    echo "Overlay installed: $relative"
  done < <(find "$ROOT/overlay" -type f -print0)
fi
