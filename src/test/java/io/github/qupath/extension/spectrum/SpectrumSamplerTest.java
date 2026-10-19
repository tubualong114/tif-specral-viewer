package io.github.qupath.extension.spectrum;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpectrumSamplerTest {

    @Test
    void readsUnsignedByteBandsInOrder() throws IOException {
        var raster = raster(DataBuffer.TYPE_BYTE, 3);
        raster.setSample(0, 0, 0, 7);
        raster.setSample(0, 0, 1, 129);
        raster.setSample(0, 0, 2, 255);
        assertArrayEquals(new double[]{7, 129, 255}, SpectrumSampler.extractValues(raster, 3));
    }

    @Test
    void readsUnsignedShortBandsInOrder() throws IOException {
        var raster = raster(DataBuffer.TYPE_USHORT, 3);
        raster.setSample(0, 0, 0, 1);
        raster.setSample(0, 0, 1, 32768);
        raster.setSample(0, 0, 2, 65535);
        assertArrayEquals(new double[]{1, 32768, 65535}, SpectrumSampler.extractValues(raster, 3));
    }

    @Test
    void readsFloatingPointBandsWithoutRounding() throws IOException {
        var floatRaster = raster(DataBuffer.TYPE_FLOAT, 2);
        floatRaster.setSample(0, 0, 0, 1.25);
        floatRaster.setSample(0, 0, 1, -2.5);
        assertArrayEquals(new double[]{1.25, -2.5}, SpectrumSampler.extractValues(floatRaster, 2), 1e-6);

        var doubleRaster = raster(DataBuffer.TYPE_DOUBLE, 2);
        doubleRaster.setSample(0, 0, 0, Math.PI);
        doubleRaster.setSample(0, 0, 1, 1.0 / 3.0);
        assertArrayEquals(new double[]{Math.PI, 1.0 / 3.0},
                SpectrumSampler.extractValues(doubleRaster, 2), 1e-12);
    }

    @Test
    void rejectsUnexpectedBandCount() {
        var raster = raster(DataBuffer.TYPE_BYTE, 2);
        assertThrows(IOException.class, () -> SpectrumSampler.extractValues(raster, 3));
    }

    private static WritableRaster raster(int dataType, int bands) {
        var sampleModel = new BandedSampleModel(dataType, 1, 1, bands);
        DataBuffer buffer = switch (dataType) {
            case DataBuffer.TYPE_BYTE -> new DataBufferByte(1, bands);
            case DataBuffer.TYPE_USHORT -> new DataBufferUShort(1, bands);
            case DataBuffer.TYPE_FLOAT -> new DataBufferFloat(1, bands);
            case DataBuffer.TYPE_DOUBLE -> new DataBufferDouble(1, bands);
            default -> throw new IllegalArgumentException("Unsupported test data type: " + dataType);
        };
        return Raster.createWritableRaster(sampleModel, buffer, new Point(0, 0));
    }
}
