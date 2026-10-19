package io.github.qupath.extension.spectrum;

import qupath.lib.gui.viewer.overlays.PathOverlay;
import qupath.lib.images.ImageData;
import qupath.lib.regions.ImageRegion;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** Temporary numbered markers in image coordinates; never added to the object hierarchy. */
final class PixelMarkerOverlay implements PathOverlay {

    record Markers(String serverPath, List<SpectrumSelection.Point> points, long selectedId) {
        Markers {
            points = List.copyOf(points);
        }
    }

    private volatile Markers markers = new Markers("", List.of(), -1);

    void update(String serverPath, List<SpectrumSelection.Point> points, long selectedId) {
        markers = new Markers(serverPath, points, selectedId);
    }

    @Override
    public void paintOverlay(Graphics2D graphics, ImageRegion region, double downsample,
                             ImageData<BufferedImage> imageData, boolean paintCompletely) {
        var snapshot = markers;
        if (imageData == null || !snapshot.serverPath().equals(imageData.getServer().getPath()))
            return;
        var g = (Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (var point : snapshot.points()) {
                if (point.z() != region.getZ() || point.t() != region.getT())
                    continue;
                Color color = point.id() == snapshot.selectedId() ? new Color(255, 190, 40) : Color.CYAN;
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
                g.fill(new Rectangle2D.Double(point.x(), point.y(), 1, 1));
                g.setColor(color);
                g.setStroke(new BasicStroke((float)(1.5 * downsample)));
                g.draw(new Rectangle2D.Double(point.x(), point.y(), 1, 1));
                drawMarker(g, point, color, downsample);
            }
        } finally {
            g.dispose();
        }
    }

    private static void drawMarker(Graphics2D graphics, SpectrumSelection.Point point, Color color, double scale) {
        var g = (Graphics2D)graphics.create();
        try {
            g.translate(point.x() + 0.5, point.y() + 0.5);
            g.scale(scale, scale);
            var ring = new Ellipse2D.Double(-7, -7, 14, 14);
            var horizontal = new Line2D.Double(-11, 0, 11, 0);
            var vertical = new Line2D.Double(0, -11, 0, 11);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(4));
            g.draw(ring);
            g.draw(horizontal);
            g.draw(vertical);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(ring);
            g.draw(horizontal);
            g.draw(vertical);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            String label = "#" + point.id();
            var outline = g.getFont().createGlyphVector(g.getFontRenderContext(), label).getOutline(12, -10);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(3));
            g.draw(outline);
            g.setColor(color);
            g.fill(outline);
        } finally {
            g.dispose();
        }
    }
}
