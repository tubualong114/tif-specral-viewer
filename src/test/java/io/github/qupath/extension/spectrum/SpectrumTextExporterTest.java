package io.github.qupath.extension.spectrum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class SpectrumTextExporterTest {

    @TempDir
    Path directory;

    @Test
    void exportsTheDisplayedMeanAsOneBasedIndicesAndUnroundedValues() throws IOException {
        var selection = new SpectrumSelection();
        var first = selection.add(0, 0, 0, 0, true);
        var second = selection.add(1, 0, 0, 0, true);
        selection.complete(first.id(), new SpectrumSample("s", "i", 0, 0, 0, 0, "FLOAT64",
                List.of("A", "B"), new double[]{1, Math.PI}));
        selection.complete(second.id(), new SpectrumSample("s", "i", 1, 0, 0, 0, "FLOAT64",
                List.of("A", "B"), new double[]{2, Math.PI}));
        var spectrum = selection.spectrum();
        var originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            SpectrumTextExporter.write(directory.resolve("mean.txt"), spectrum);
        } finally {
            Locale.setDefault(originalLocale);
        }
        var lines = Files.readAllLines(directory.resolve("mean.txt"));
        assertEquals("channel_index\tintensity", lines.getFirst());
        assertEquals("1\t1.5", lines.get(1));
        assertEquals(Math.PI, Double.parseDouble(lines.get(2).split("\t")[1]));
        assertEquals(3, lines.size());
    }

    @Test
    void propagatesWriteErrors() {
        var spectrum = new SpectrumSelection.Spectrum(List.of("A"), new double[]{1}, 1);
        assertThrows(IOException.class, () -> SpectrumTextExporter.write(directory.resolve("missing/out.txt"), spectrum));
    }
}
