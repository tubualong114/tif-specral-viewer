package io.github.qupath.extension.spectrum;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.WrappedBufferedImageServer;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Run with `gradle uiTest` on a machine with a graphical desktop. */
class SpectrumInspectorUiTest {

    @Test
    @Timeout(90)
    void realQuPathWindowSupportsSelectionMeanDeletionAndMarkerLifecycle() throws Exception {
        var started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
        QuPathGUI qupath = null;
        try {
            qupath = fx(QuPathGUI::createHiddenInstance);
            var gui = qupath;
            var extension = new SpectrumViewerExtension();
            var image = new BufferedImage(80, 80, BufferedImage.TYPE_3BYTE_BGR);
            image.getRaster().setPixel(10, 10, new int[]{2, 4, 6});
            image.getRaster().setPixel(20, 20, new int[]{10, 20, 30});
            image.getRaster().setPixel(30, 30, new int[]{4, 8, 12});
            var server = new WrappedBufferedImageServer("UI integration image", image) {
                @Override
                public ImageServerMetadata getOriginalMetadata() {
                    return new ImageServerMetadata.Builder(super.getOriginalMetadata()).sizeZ(2).sizeT(2).build();
                }

                @Override
                public ImageServerMetadata getMetadata() {
                    return getOriginalMetadata();
                }
            };
            fx(() -> {
                gui.getViewer().setImageData(new ImageData<>(server, ImageData.ImageType.FLUORESCENCE));
                extension.installExtension(gui);
                return null;
            });
            SpectrumInspector inspector = field(extension, "inspector");
            CheckBox multiple = field(inspector, "multipleMode");
            Stage stage = field(inspector, "stage");
            Button remove = field(inspector, "removePointButton");
            Button export = field(inspector, "exportButton");
            ListView<SpectrumSelection.Point> list = field(inspector, "pointList");
            var previousTool = fx(() -> gui.getToolManager().getSelectedTool());
            fx(() -> { inspector.showAndActivate(); inspector.requestSample(gui.getViewer(), 10, 10); return null; });
            awaitCount(inspector, 1);
            assertEquals(1, fx(() -> gui.getViewer().getCustomOverlayLayers().size()).intValue());
            fx(() -> {
                multiple.fire();
                inspector.requestSample(gui.getViewer(), 20, 20);
                inspector.requestSample(gui.getViewer(), 30, 30);
                list.getSelectionModel().select(1);
                remove.fire();
                return null;
            });
            awaitCount(inspector, 2);
            assertEquals(2, fx(() -> list.getItems().size()).intValue());
            assertArrayEquals(new double[]{3, 6, 9}, fx(() -> spectrum(inspector).intensities()));
            assertFalse(fx(export::isDisabled));
            fx(() -> {
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                var screenshot = Path.of("build/reports/spectrum-ui.png");
                Files.createDirectories(screenshot.getParent());
                ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null), null), "png", screenshot.toFile());
                return null;
            });
            fx(() -> { inspector.showAndActivate(); return null; });
            assertEquals(2, fx(() -> list.getItems().size()).intValue(), "Reopening the menu must preserve points");
            fx(() -> { list.getSelectionModel().selectFirst(); multiple.fire(); return null; });
            assertArrayEquals(new double[]{2, 4, 6}, fx(() -> spectrum(inspector).intensities()));
            fx(() -> { gui.getViewer().setZPosition(1 - gui.getViewer().getZPosition()); return null; });
            assertTrue(fx(() -> list.getItems().isEmpty()));
            assertTrue(fx(export::isDisabled));
            fx(() -> { inspector.requestSample(gui.getViewer(), 10, 10); return null; });
            awaitCount(inspector, 1);
            fx(() -> { gui.getViewer().setTPosition(1 - gui.getViewer().getTPosition()); return null; });
            assertTrue(fx(() -> list.getItems().isEmpty()));
            fx(() -> { stage.hide(); return null; });
            assertEquals(0, fx(() -> gui.getViewer().getCustomOverlayLayers().size()).intValue());
            assertSame(previousTool, fx(() -> gui.getToolManager().getSelectedTool()));
            fx(() -> {
                inspector.showAndActivate();
                gui.getViewer().setImageData(new ImageData<>(new WrappedBufferedImageServer("other", image)));
                return null;
            });
            assertNull(fx(() -> spectrum(inspector)));
            assertEquals(1, fx(() -> gui.getViewer().getCustomOverlayLayers().size()).intValue());
            fx(() -> { stage.hide(); return null; });
        } finally {
            if (qupath != null) {
                var gui = qupath;
                fx(() -> { gui.getStage().hide(); gui.getViewer().closeViewer(); return null; });
            }
            Platform.exit();
        }
    }

    private static SpectrumSelection.Spectrum spectrum(SpectrumInspector inspector) throws Exception {
        return field(inspector, "displayedSpectrum");
    }

    private static void awaitCount(SpectrumInspector inspector, int count) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (fx(() -> spectrum(inspector) != null && spectrum(inspector).pointCount() == count))
                return;
            Thread.sleep(20);
        }
        fail("Pixel reads did not complete");
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(25, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T)field.get(object);
    }
}
