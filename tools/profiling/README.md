# Perfetto profiling workflow

1. Build and install `profile` (`./gradlew :app:installProfile`). It uses the normal application ID and debug signing, so it can replace a debug install without losing app-private models or history.
2. Open Chat, Speech to Text, or Text to Speech; select and configure the model and workload on that screen, then tap its **Profile** button.
3. Connect the Galaxy S24 with USB debugging enabled and run `./tools/profiling/record-inference-trace.sh`.
4. Start exactly one profile session while the trace is recording. The trace contains `LAP/<capability>/<run-id>` and phase markers. The same IDs are stored in run metrics and profile export.
5. Open the trace in Perfetto or Android Studio's Android Performance Analyzer. Inspect scheduler, frequency, process memory, battery/power, thermal and the device-exposed GPU tracks/counters.

GPU/NPU utilization is deliberately not inferred by the app. Galaxy S24 Snapdragon and Exynos variants expose different GPU/accelerator tracks, so treat missing counters as unavailable. Match the external track to the LiteRT marker and the persisted requested/effective backend plus fallback reason.

The trace is useful for both ordinary inference and Profile runs. Trace slices stay cheap unless a system trace is actively recording; extended CPU/PSS/battery/thermal sampling is enabled only for workloads launched through a screen's Profile button.
