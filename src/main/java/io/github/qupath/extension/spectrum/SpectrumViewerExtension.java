package io.github.qupath.extension.spectrum;

import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.viewer.tools.PathTool;
import qupath.lib.gui.viewer.tools.PathTools;

import java.awt.geom.Point2D;
import java.util.ResourceBundle;

/**
 * QuPath extension entry point.
 */
public final class SpectrumViewerExtension implements QuPathExtension {

    private static final Logger logger = LoggerFactory.getLogger(SpectrumViewerExtension.class);
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(
            "io.github.qupath.extension.spectrum.strings");
    private static final Version QUPATH_VERSION = Version.parse("0.7.0");

    private boolean installed;
    private QuPathGUI qupath;
    private PathTool probeTool;
    private PathTool toolBeforeProbe;
    private SpectrumInspector inspector;

    @Override
    public void installExtension(QuPathGUI qupath) {
        if (installed) {
            logger.debug("{} is already installed", getName());
            return;
        }
        installed = true;
        this.qupath = qupath;

        var icon = new Label("⊹");
        icon.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        probeTool = PathTools.createTool(
                MouseEvent.MOUSE_CLICKED,
                this::handleMouseClicked,
                RESOURCES.getString("tool.name"),
                icon);
        qupath.getToolManager().installTool(probeTool, null);

        inspector = new SpectrumInspector(
                qupath, RESOURCES, probeTool, this::activateProbe, this::restorePreviousTool);

        MenuItem menuItem = new MenuItem(RESOURCES.getString("menu.open"));
        menuItem.setOnAction(event -> inspector.showAndActivate());
        qupath.getMenu("Extensions>" + getName(), true).getItems().add(menuItem);
        logger.info("Installed {} for QuPath {}", getName(), QUPATH_VERSION);
    }

    private void activateProbe() {
        if (qupath == null || probeTool == null)
            return;
        var toolManager = qupath.getToolManager();
        if (toolManager.getSelectedTool() != probeTool) {
            toolBeforeProbe = toolManager.getSelectedTool();
            toolManager.setSelectedTool(probeTool);
        }
    }

    private void restorePreviousTool() {
        if (qupath == null || probeTool == null)
            return;
        var toolManager = qupath.getToolManager();
        if (toolManager.getSelectedTool() == probeTool && toolBeforeProbe != null && toolBeforeProbe != probeTool)
            toolManager.setSelectedTool(toolBeforeProbe);
        toolBeforeProbe = null;
    }

    private void handleMouseClicked(MouseEvent event) {
        if (event.isConsumed() || event.getButton() != MouseButton.PRIMARY
                || event.getClickCount() != 1 || !event.isStillSincePress())
            return;
        event.consume();

        var viewer = qupath == null ? null : qupath.getViewer();
        if (viewer == null || viewer.getServer() == null) {
            if (inspector != null)
                inspector.requestSample(null, -1, -1);
            return;
        }
        Point2D point = viewer.componentPointToImagePoint(event.getX(), event.getY(), null, false);
        int x = (int)Math.floor(point.getX());
        int y = (int)Math.floor(point.getY());
        inspector.requestSample(viewer, x, y);
    }

    @Override
    public String getName() {
        return RESOURCES.getString("extension.name");
    }

    @Override
    public String getDescription() {
        return RESOURCES.getString("extension.description");
    }

    @Override
    public Version getQuPathVersion() {
        return QUPATH_VERSION;
    }
}
