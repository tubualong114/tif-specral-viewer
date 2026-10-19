package io.github.qupath.extension.spectrum;

import org.junit.jupiter.api.Test;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.WrappedBufferedImageServer;
import qupath.lib.regions.ImageRegion;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PixelMarkerOverlayTest {

    @Test
    void rendersSelectedAndOtherMarkersOnlyForTheMatchingImageAndPlane() throws Exception {
        try (var imageData = new ImageData<>(new WrappedBufferedImageServer("markers",
                new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY)))) {
            var overlay = new PixelMarkerOverlay();
            overlay.update(imageData.getServer().getPath(), List.of(
                    new SpectrumSelection.Point(1, 30, 30, 0, 0, null, null),
                    new SpectrumSelection.Point(2, 70, 70, 0, 0, null, null)), 1);
            var canvas = render(overlay, imageData, 0, 0);
            assertTrue(containsColor(canvas, 0xffbe28), "Selected marker should be orange");
            assertTrue(containsColor(canvas, 0x00ffff), "Other markers should be cyan");
            assertFalse(hasVisiblePixels(render(overlay, imageData, 1, 0)));
            assertFalse(hasVisiblePixels(render(overlay, imageData, 0, 1)));
            overlay.update("another image", List.of(new SpectrumSelection.Point(1, 30, 30, 0, 0, null, null)), 1);
            assertFalse(hasVisiblePixels(render(overlay, imageData, 0, 0)));
        }
    }

    private static BufferedImage render(PixelMarkerOverlay overlay, ImageData<BufferedImage> data, int z, int t) {
        var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        var transform = g.getTransform();
        var stroke = g.getStroke();
        overlay.paintOverlay(g, ImageRegion.createInstance(0, 0, 100, 100, z, t), 1, data, true);
        assertEquals(transform, g.getTransform());
        assertEquals(stroke, g.getStroke());
        g.dispose();
        return image;
    }

    private static boolean containsColor(BufferedImage image, int rgb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == (0xff000000 | rgb))
                    return true;
            }
        }
        return false;
    }

    private static boolean hasVisiblePixels(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0)
                    return true;
            }
        }
        return false;
    }
}
