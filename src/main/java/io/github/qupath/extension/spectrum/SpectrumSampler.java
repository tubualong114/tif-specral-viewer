package io.github.qupath.extension.spectrum;

import qupath.lib.images.servers.ImageServer;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reads exact, full-resolution 1x1 regions away from the JavaFX thread.
 */
final class SpectrumSampler implements AutoCloseable {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "qupath-spectrum-sampler");
        thread.setDaemon(true);
        return thread;
    });

    CompletableFuture<SpectrumSample> sample(
            ImageServer<BufferedImage> server,
            String imageName,
            int x,
            int y,
            int z,
            int t) {
        Objects.requireNonNull(server, "server");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sampleNow(server, imageName, x, y, z, t);
            } catch (IOException e) {
                throw new SpectrumReadException(e);
            }
        }, executor);
    }

    static SpectrumSample sampleNow(
            ImageServer<BufferedImage> server,
            String imageName,
            int x,
            int y,
            int z,
            int t) throws IOException {
        validateCoordinates(server, x, y, z, t);
        BufferedImage image = server.readRegion(1.0, x, y, 1, 1, z, t);
        if (image == null || image.getWidth() < 1 || image.getHeight() < 1)
            throw new IOException("The image server returned no pixel data");

        int expectedChannels = server.nChannels();
        double[] values = extractValues(image.getRaster(), expectedChannels);
        List<String> names = new ArrayList<>(expectedChannels);
        for (int channel = 0; channel < expectedChannels; channel++) {
            String name = server.getChannel(channel) == null ? null : server.getChannel(channel).getName();
            if (name == null || name.isBlank())
                name = "Channel " + (channel + 1);
            names.add(name);
        }
        String resolvedImageName = imageName;
        if (resolvedImageName == null || resolvedImageName.isBlank()) {
            resolvedImageName = server.getMetadata() == null ? null : server.getMetadata().getName();
        }
        if (resolvedImageName == null || resolvedImageName.isBlank())
            resolvedImageName = server.getPath();

        return new SpectrumSample(
                server.getPath(), resolvedImageName, x, y, z, t,
                String.valueOf(server.getPixelType()), names, values);
    }

    static double[] extractValues(Raster raster, int expectedChannels) throws IOException {
        if (raster == null)
            throw new IOException("The image server returned a null raster");
        if (raster.getNumBands() != expectedChannels) {
            throw new IOException("Expected " + expectedChannels + " channels but received "
                    + raster.getNumBands() + " raster bands");
        }
        double[] values = new double[expectedChannels];
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        for (int channel = 0; channel < expectedChannels; channel++)
            values[channel] = raster.getSampleDouble(minX, minY, channel);
        return values;
    }

    private static void validateCoordinates(
            ImageServer<BufferedImage> server, int x, int y, int z, int t) {
        if (x < 0 || x >= server.getWidth() || y < 0 || y >= server.getHeight())
            throw new IllegalArgumentException("Pixel coordinate is outside the image bounds");
        if (z < 0 || z >= server.nZSlices() || t < 0 || t >= server.nTimepoints())
            throw new IllegalArgumentException("Z/T coordinate is outside the image bounds");
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    static final class SpectrumReadException extends RuntimeException {
        SpectrumReadException(IOException cause) {
            super(cause);
        }
    }
}
