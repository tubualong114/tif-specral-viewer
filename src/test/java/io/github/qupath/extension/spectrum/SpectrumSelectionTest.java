package io.github.qupath.extension.spectrum;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpectrumSelectionTest {

    @Test
    void averagesAllCompletedClicksAndRecomputesAfterDeletion() {
        var selection = new SpectrumSelection();
        var first = selection.add(10, 20, 0, 0, true);
        var second = selection.add(30, 40, 0, 0, true);
        assertTrue(selection.complete(second.id(), sample(30, 40, 2, 7)));
        assertTrue(selection.complete(first.id(), sample(10, 20, 1, 2)));
        assertEquals(2, selection.spectrum().pointCount());
        assertArrayEquals(new double[]{1.5, 4.5}, selection.spectrum().intensities());
        selection.remove(first.id());
        assertArrayEquals(new double[]{2, 7}, selection.spectrum().intensities());
        selection.remove(second.id());
        assertNull(selection.spectrum());
    }

    @Test
    void repeatedPixelsDoNotChangeWeightAndPendingOrFailedPointsAreExcluded() {
        var selection = new SpectrumSelection();
        var first = selection.add(10, 20, 0, 0, true);
        assertEquals(first.id(), selection.add(10, 20, 0, 0, true).id());
        var second = selection.add(30, 40, 0, 0, true);
        selection.add(50, 60, 0, 0, true);
        selection.fail(second.id(), "Read failed");
        assertNull(selection.spectrum());
        selection.complete(first.id(), sample(10, 20, 3, 8));
        assertEquals(3, selection.points().size());
        assertEquals(1, selection.spectrum().pointCount());
        assertArrayEquals(new double[]{3, 8}, selection.spectrum().intensities());
    }

    @Test
    void deletedClearedAndReplacedPointsCannotBeResurrectedByLateReads() {
        var selection = new SpectrumSelection();
        var first = selection.add(10, 20, 0, 0, true);
        selection.remove(first.id());
        assertFalse(selection.complete(first.id(), sample(10, 20, 1, 2)));
        var second = selection.add(10, 20, 0, 0, true);
        selection.clear();
        var third = selection.add(10, 20, 0, 0, false);
        assertNotEquals(second.id(), third.id());
        assertFalse(selection.complete(second.id(), sample(10, 20, 1, 2)));
        var fourth = selection.add(30, 40, 0, 0, false);
        assertFalse(selection.complete(third.id(), sample(10, 20, 1, 2)));
        selection.complete(fourth.id(), sample(30, 40, 6, 8));
        assertEquals(1, selection.points().size());
        assertArrayEquals(new double[]{6, 8}, selection.spectrum().intensities());
    }

    @Test
    void switchingBackToSingleCanRetainTheSelectedPixel() {
        var selection = new SpectrumSelection();
        var first = selection.add(10, 20, 0, 0, true);
        var second = selection.add(30, 40, 0, 0, true);
        selection.complete(first.id(), sample(10, 20, 1, 2));
        selection.retainOnly(first.id());
        assertFalse(selection.fail(second.id(), "Late failure"));
        assertEquals(List.of(first.id()), selection.points().stream().map(SpectrumSelection.Point::id).toList());
        assertArrayEquals(new double[]{1, 2}, selection.spectrum().intensities());
    }

    @Test
    void rejectsCoordinatesOrImageAndChannelMismatches() {
        var selection = new SpectrumSelection();
        var first = selection.add(10, 20, 0, 0, true);
        assertThrows(IllegalArgumentException.class,
                () -> selection.complete(first.id(), sample(11, 20, 1, 2)));
        selection.complete(first.id(), sample(10, 20, 1, 2));
        var second = selection.add(30, 40, 0, 0, true);
        assertThrows(IllegalArgumentException.class, () -> selection.complete(second.id(),
                new SpectrumSample("other", "image", 30, 40, 0, 0, "UINT8", List.of("A", "B"), new double[]{1, 2})));
        assertThrows(IllegalArgumentException.class, () -> selection.complete(second.id(),
                new SpectrumSample("server", "image", 30, 40, 0, 0, "UINT8", List.of("A"), new double[]{1})));
    }

    private static SpectrumSample sample(int x, int y, double a, double b) {
        return new SpectrumSample("server", "image", x, y, 0, 0, "UINT8", List.of("A", "B"), new double[]{a, b});
    }
}
