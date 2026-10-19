# QuPath Spectrum Viewer

An open-source **QuPath 0.7.0** extension for navigating multichannel images and inspecting the raw per-channel spectrum of an individual pixel.

## Features

- Channel slider: `0` shows all direct channels; `1…N` shows one channel.
- Spectrum probe: click a pixel to read all raw channel values at the viewer's current Z/T plane.
- Reusable non-modal window with coordinates, pixel type, spectrum chart, and channel table.
- English and Simplified Chinese UI selected from the runtime locale.
- Single-threaded background sampling with stale-request suppression.

Displayed values are original pixel values and are independent of QuPath LUT, brightness, and contrast settings.

## Install

1. Obtain `qupath-extension-spectrum-viewer-*.jar` from a release or `build/libs/`.
2. Start QuPath 0.7.0 and drag the JAR onto the main window.
3. Restart QuPath.
4. Open `Extensions > Spectrum Viewer > Open spectrum viewer`.

Opening the window activates the spectrum probe. Select QuPath's Move tool to pan again, or use **Activate probe** in the spectrum window to resume sampling.

## Build

QuPath 0.7.0 recommends OpenJDK 25. A Gradle Wrapper is included, so no global Gradle installation is required.

```bash
./gradlew clean test build
```

On Windows PowerShell, run `./gradlew.bat clean test build`. The extension JAR is created in `build/libs/`.

## Image notes

The bundled samples are ImageJ TIFF files with 20–28 uint8 channels and no wavelength metadata. The extension therefore uses QuPath channel names, falling back to `Channel 1…N`. If QuPath opens a sample with the wrong number of channels, explicitly select the **Bio-Formats** reader.

## Current scope

- Exact single-pixel sampling only; no neighborhood or ROI averaging.
- No wavelength inference from filenames and no spectral unmixing.
- Decoding is delegated to QuPath's active `ImageServer`.

## License

Licensed under the [Apache License 2.0](LICENSE). QuPath itself is GPL-3.0-or-later; QuPath binaries are not bundled in this extension.
