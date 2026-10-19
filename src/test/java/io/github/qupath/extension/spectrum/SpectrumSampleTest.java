package io.github.qupath.extension.spectrum;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpectrumSampleTest {

    @Test
    void protectsIntensityArrayFromMutation() {
        double[] input = {1, 2, 3};
        var sample = new SpectrumSample("server", "image", 1, 2, 0, 0,
                "UINT8", List.of("A", "B", "C"), input);
        input[0] = 99;
        double[] result = sample.intensities();
        result[1] = 99;
        assertArrayEquals(new double[]{1, 2, 3}, sample.intensities());
    }

    @Test
    void rejectsMismatchedChannelMetadata() {
        assertThrows(IllegalArgumentException.class, () ->
                new SpectrumSample("server", "image", 1, 2, 0, 0,
                        "UINT8", List.of("A"), new double[]{1, 2}));
    }
}
