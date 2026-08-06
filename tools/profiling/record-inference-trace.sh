#!/usr/bin/env bash
set -euo pipefail

# Usage: ./tools/profiling/record-inference-trace.sh [serial]
# Starts a two-minute system trace, then pulls it when recording completes.
serial_args=()
if [[ $# -gt 0 ]]; then serial_args=(-s "$1"); fi
adb "${serial_args[@]}" push tools/profiling/inference-trace.pbtxt /data/local/tmp/inference-trace.pbtxt
adb "${serial_args[@]}" shell perfetto -c /data/local/tmp/inference-trace.pbtxt -o /data/misc/perfetto-traces/local-ai-inference.perfetto-trace &
trace_pid=$!
echo "Tracing. Start one Profile session from Chat, STT, or TTS now; recording ends after 120 seconds."
wait "$trace_pid"
mkdir -p build/profiling
adb "${serial_args[@]}" pull /data/misc/perfetto-traces/local-ai-inference.perfetto-trace build/profiling/local-ai-inference.perfetto-trace
echo "Saved build/profiling/local-ai-inference.perfetto-trace"
