#!/usr/bin/env bash
set -euo pipefail

# Usage: ./tools/profiling/record-inference-trace.sh [serial]
# Starts a two-minute system trace, then pulls it when recording completes.
if [[ $# -gt 0 ]]; then
  adb_command=(adb -s "$1")
else
  adb_command=(adb)
fi
config_path=/data/misc/perfetto-configs/local-ai-inference.pbtxt
"${adb_command[@]}" push tools/profiling/inference-trace.pbtxt "$config_path"
"${adb_command[@]}" shell perfetto --txt -c "$config_path" -o /data/misc/perfetto-traces/local-ai-inference.perfetto-trace &
trace_pid=$!
echo "Tracing. Start one Profile session from Chat, STT, or TTS now; recording ends after 120 seconds."
wait "$trace_pid"
mkdir -p build/profiling
"${adb_command[@]}" pull /data/misc/perfetto-traces/local-ai-inference.perfetto-trace build/profiling/local-ai-inference.perfetto-trace
echo "Saved build/profiling/local-ai-inference.perfetto-trace"
