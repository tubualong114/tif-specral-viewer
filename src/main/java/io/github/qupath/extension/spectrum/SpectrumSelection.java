package io.github.qupath.extension.spectrum;

import java.util.ArrayList;
import java.util.List;

/** Selection state, including pending reads. Accessed only on the JavaFX thread. */
final class SpectrumSelection {

    record Point(long id, int x, int y, int z, int t, SpectrumSample sample, String error) {
        boolean isPending() {
            return sample == null && error == null;
        }
    }

    record Spectrum(List<String> channelNames, double[] intensities, int pointCount) {
        Spectrum {
            channelNames = List.copyOf(channelNames);
            intensities = intensities.clone();
        }

        @Override
        public double[] intensities() {
            return intensities.clone();
        }
    }

    private final List<Point> points = new ArrayList<>();
    private long nextId = 1;

    List<Point> points() {
        return List.copyOf(points);
    }

    Point add(int x, int y, int z, int t, boolean multiple) {
        if (!multiple)
            clear();
        for (var point : points) {
            if (point.x() == x && point.y() == y && point.z() == z && point.t() == t)
                return point;
        }
        var point = new Point(nextId++, x, y, z, t, null, null);
        points.add(point);
        return point;
    }

    boolean complete(long id, SpectrumSample sample) {
        int index = indexOf(id);
        if (index < 0)
            return false;
        var point = points.get(index);
        if (point.x() != sample.x() || point.y() != sample.y()
                || point.z() != sample.z() || point.t() != sample.t())
            throw new IllegalArgumentException("Sample coordinates differ from the selected point");
        var reference = points.stream().map(Point::sample).filter(java.util.Objects::nonNull).findFirst();
        if (reference.isPresent()) {
            var first = reference.get();
            if (!first.serverPath().equals(sample.serverPath()) || first.z() != sample.z()
                    || first.t() != sample.t() || !first.channelNames().equals(sample.channelNames()))
                throw new IllegalArgumentException("Samples must belong to the same image, plane and channels");
        }
        points.set(index, new Point(id, point.x(), point.y(), point.z(), point.t(), sample, null));
        return true;
    }

    boolean fail(long id, String error) {
        int index = indexOf(id);
        if (index < 0)
            return false;
        var point = points.get(index);
        points.set(index, new Point(id, point.x(), point.y(), point.z(), point.t(), null,
                error == null ? "Read failed" : error));
        return true;
    }

    boolean remove(long id) {
        return points.removeIf(point -> point.id() == id);
    }

    void retainOnly(long id) {
        points.removeIf(point -> point.id() != id);
    }

    void clear() {
        // Keep IDs increasing so a removed point's late callback can never match a new point.
        points.clear();
    }

    Spectrum spectrum() {
        var samples = points.stream().map(Point::sample).filter(java.util.Objects::nonNull).toList();
        if (samples.isEmpty())
            return null;
        var names = samples.getFirst().channelNames();
        double[] mean = new double[names.size()];
        for (var sample : samples) {
            double[] values = sample.intensities();
            for (int channel = 0; channel < mean.length; channel++)
                mean[channel] += values[channel] / samples.size();
        }
        return new Spectrum(names, mean, samples.size());
    }

    private int indexOf(long id) {
        for (int index = 0; index < points.size(); index++) {
            if (points.get(index).id() == id)
                return index;
        }
        return -1;
    }
}
