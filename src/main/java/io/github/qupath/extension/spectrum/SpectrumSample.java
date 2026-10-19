package io.github.qupath.extension.spectrum;

import java.util.List;

/**
 * Immutable raw spectrum sampled from one image pixel.
 */
record SpectrumSample(
        String serverPath,
        String imageName,
        int x,
        int y,
        int z,
        int t,
        String pixelType,
        List<String> channelNames,
        double[] intensities) {

    SpectrumSample {
        channelNames = List.copyOf(channelNames);
        intensities = intensities.clone();
        if (channelNames.size() != intensities.length)
            throw new IllegalArgumentException("Channel names and intensities must have the same length");
    }

    @Override
    public double[] intensities() {
        return intensities.clone();
    }
}
