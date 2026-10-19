# QuPath Spectrum Viewer

An open-source **QuPath 0.7.0** extension for navigating multichannel images, inspecting raw pixel spectra, averaging multiple selected pixels, and exporting spectra.

## Features

- Channel slider: `0` shows all direct channels; `1…N` shows one channel.
- Spectrum probe: click a pixel to read all raw channel values at the viewer's current Z/T plane.
- Reusable non-modal window with coordinates, pixel type, spectrum chart, and channel table.
- Numbered crosshair and pixel-outline overlays identify every sampled pixel.
- Editable multi-pixel list with a per-channel arithmetic mean of successful reads.
- TXT export of the displayed single-pixel or mean spectrum.
- English and Simplified Chinese UI selected from the runtime locale.
- Single-threaded background sampling with stale-request suppression.

Displayed values are original pixel values and are independent of QuPath LUT, brightness, and contrast settings.

## Install

1. Obtain `qupath-extension-spectrum-viewer-*.jar` from a release or `build/libs/`.
2. Start QuPath 0.7.0 and drag the JAR onto the main window.
3. Restart QuPath.
4. Open `Extensions > Spectrum Viewer > Open spectrum viewer`.

Opening the window activates the spectrum probe. Select QuPath's Move tool to pan again, or use **Activate probe** in the spectrum window to resume sampling.

## Markers, multiple pixels, and export

In single-pixel mode each click replaces the current point. The orange numbered marker and pixel outline correspond to the point ID and 0-based full-resolution x/y/z/t coordinates shown in the window. Markers remain aligned during zoom and rotation.

Enable **Multiple pixels (mean spectrum)** to retain the current point and add more by clicking the image. Duplicate coordinates are counted once. Select a list entry to highlight its marker in orange; other markers are cyan. **Remove selected** and **Clear pixels** immediately update the spectrum. Turning multiple-pixel mode off keeps the selected entry as the single pixel.

Each channel is the arithmetic mean over successfully read points. Pending and failed reads are excluded, with counts shown in the window. Rapid clicks retain every distinct point; removed points cannot reappear when a read completes. To retry a failed pixel, remove it and click it again. Switching image, active viewer, or Z/T plane clears the selection. Closing the window clears points and removes temporary markers; no QuPath annotations are created.

**Export TXT…** saves a snapshot of the displayed spectrum as UTF-8 tab-separated text with the header `channel_index\tintensity` and two columns: 1-based channel index and raw intensity. Decimal points are locale-independent and values are not rounded for export. In multiple-pixel mode the exported values are means of the currently successful reads; wait for the pending count to reach zero to include the entire selection. Without wavelength metadata, indices are not interpreted as nm.

## Build

QuPath 0.7.0 recommends OpenJDK 25. A Gradle Wrapper is included, so no global Gradle installation is required.

```bash
./gradlew clean test build
```

On Windows PowerShell, run `./gradlew.bat clean test build`. The extension JAR is created in `build/libs/`.

On a machine with a graphical desktop, run `./gradlew uiTest` (`./gradlew.bat uiTest` on Windows) for the real QuPath/JavaFX window integration test. Preferences are isolated in memory. Reports are in `build/reports/tests/uiTest/`, with a window snapshot at `build/reports/spectrum-ui.png`.

## Image notes

The local samples (excluded from Git) are ImageJ TIFF files with 20–28 uint8 channels and no wavelength metadata. The extension therefore uses QuPath channel names, falling back to `Channel 1…N`. If QuPath opens a sample with the wrong number of channels, explicitly select the **Bio-Formats** reader.

## Current scope

- Exact single-pixel sampling and equal-weight averaging of selected pixels; no neighborhood or ROI area averaging.
- No wavelength inference from filenames and no spectral unmixing.
- Decoding is delegated to QuPath's active `ImageServer`.

## License

Licensed under the [Apache License 2.0](LICENSE). QuPath itself is GPL-3.0-or-later; QuPath binaries are not bundled in this extension.
