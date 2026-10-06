# Windows rendering investigation — 0.2.4

The OXP3 owner reports stuttering camera motion and menus. No OXP3 timing report
has been collected yet, so neither an Intel driver fault nor a hardware limitation
has been established.

## Findings and changes

The software canvas repeated affine scaling of large camera/office images on the
Swing event thread. The same thread handles menus and gameplay. A bounded 128 MiB
cache now reuses bilinearly resized artwork, moving only the crop/translation on
successive frames. The default composition is 1920×1080, with Windows DPI scaling
accounted for; output above 1080p scales the completed image. Native remains an
alternative. This does not reduce camera scan cadence or gameplay simulation rate.

Native JavaSound stop/close calls also ran on the event thread, including every
menu selection. They now execute on an ordered audio worker. A deliberately blocked
fake audio driver test proves these calls no longer block the event thread. This
is a proven isolation fix, not evidence that the user's audio driver was stalling.

Analog batches previously replayed every stale sample after a delayed paint. Only
the newest analog sample is now applied; button transitions are preserved in order.
Menu navigation uses a 0.55 engagement / 0.30 release threshold, a 350 ms initial
repeat delay, and 180 ms repeats. Office sensitivity is unchanged.

## Local measurements

Owned imported Steam data, isolated saves, Java 21, 768 MiB heap. The paired
1080p offscreen benchmark uses the same renderer/profile/input scene and changes
only Canvas back to the previous implementation for the baseline:

| Scene at 1920×1080 | Old Canvas median | Cached Canvas median |
| --- | ---: | ---: |
| Office pan | 16.49 ms | 3.24 ms |
| Camera pan | 32.63 ms | 7.96 ms |

A separate five-second real-window check (1904×1041 client area, animated camera
static, normal frame loop) measured 59.3 presented FPS and 5.00 seconds of simulated
time. Paint p95 was 9.18 ms; max was 57.25 ms. This short local check does not prove
long-session behavior or OXP3 performance. First-use texture preparation can still
produce spikes. No original-game AI timing claims are inferred from this test.

## Next device check

Run 0.2.4 on the OXP3 with **Rendering: 1080p**, reproduce the camera issue, then
copy **Options → Performance / controller report**. Compare scene time, output
scaling time, update intervals, XInput query time, and graphics configuration.
A D3D configuration alone does not prove all draw operations are GPU accelerated.
If artifacts persist with low timings, compare the Native option to the composed
1080p path before changing system graphics drivers.

[Oracle's Java 2D troubleshooting guidance](https://docs.oracle.com/en/java/javase/22/troubleshoot/java-2d.html)
describes software image rendering and mixed accelerated/unaccelerated costs.
[Pipeline troubleshooting](https://docs.oracle.com/en/java/javase/22/troubleshoot/java-2d-pipeline-rendering-and-properties.html)
describes driver/pipeline diagnostics. Neither establishes an OXP3-specific defect.
