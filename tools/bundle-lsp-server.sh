#!/usr/bin/env bash
# Bundle the maude-lsp language server into this plugin's resources.
# Usage: [MAUDE_LSP=/path/to/maude-lsp] tools/bundle-lsp-server.sh
set -euo pipefail

PLUGIN_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MAUDE_LSP="${MAUDE_LSP:-$(cd "$PLUGIN_ROOT/../maude-lsp" 2>/dev/null && pwd || true)}"

if [ -z "${MAUDE_LSP:-}" ] || [ ! -f "$MAUDE_LSP/package.json" ]; then
  echo "maude-lsp checkout not found. Set MAUDE_LSP=/path/to/maude-lsp." >&2
  exit 1
fi

echo "Building server from $MAUDE_LSP ..."
( cd "$MAUDE_LSP" && npm install --silent && npm run build:server )

mkdir -p "$PLUGIN_ROOT/src/main/resources/lsp"
cp "$MAUDE_LSP/server/dist/server.js" "$PLUGIN_ROOT/src/main/resources/lsp/server.js"
echo "bundled $(wc -c < "$PLUGIN_ROOT/src/main/resources/lsp/server.js") bytes -> src/main/resources/lsp/server.js"
