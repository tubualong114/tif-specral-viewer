package io.github.qupath.extension.spectrum;

import org.junit.jupiter.api.Test;
import qupath.lib.gui.extensions.QuPathExtension;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionPackagingTest {

    private static final String BUNDLE = "io.github.qupath.extension.spectrum.strings";

    @Test
    void extensionIsDiscoverableThroughServiceLoader() {
        boolean found = ServiceLoader.load(QuPathExtension.class).stream()
                .map(ServiceLoader.Provider::type)
                .anyMatch(SpectrumViewerExtension.class::equals);
        assertTrue(found, "SpectrumViewerExtension must be registered as a QuPathExtension service");
    }

    @Test
    void englishAndChineseBundlesHaveTheSameKeys() {
        var english = ResourceBundle.getBundle(BUNDLE, Locale.ENGLISH);
        var chinese = ResourceBundle.getBundle(BUNDLE, Locale.SIMPLIFIED_CHINESE);
        assertEquals(english.keySet(), chinese.keySet());
        assertEquals("Spectrum Viewer", english.getString("extension.name"));
        assertEquals("像素光谱查看器", chinese.getString("extension.name"));
    }
}
