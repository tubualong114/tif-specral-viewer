package io.github.qupath.extension.spectrum;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Exports exactly the values displayed by the chart, with locale-independent decimals. */
final class SpectrumTextExporter {

    private SpectrumTextExporter() {
    }

    static void write(Path path, SpectrumSelection.Spectrum spectrum) throws IOException {
        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("channel_index\tintensity\n");
            double[] values = spectrum.intensities();
            for (int index = 0; index < values.length; index++) {
                writer.write(Integer.toString(index + 1));
                writer.write('\t');
                writer.write(Double.toString(values[index]));
                writer.write('\n');
            }
        }
    }
}
