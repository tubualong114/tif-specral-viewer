package io.github.qupath.extension.spectrum;

import javafx.collections.ListChangeListener;
import qupath.lib.display.ChannelDisplayInfo;
import qupath.lib.display.DirectServerChannelInfo;
import qupath.lib.display.ImageDisplay;
import qupath.lib.gui.viewer.QuPathViewer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts between a compact slider model and QuPath's display-channel model.
 */
final class ChannelDisplayController implements AutoCloseable {

    private final QuPathViewer viewer;
    private final ImageDisplay display;
    private final List<Runnable> stateListeners = new ArrayList<>();
    private final ListChangeListener<ChannelDisplayInfo> selectedListener = change -> stateChanged();
    private final ListChangeListener<ChannelDisplayInfo> availableListener = change -> stateChanged();
    private boolean changing;

    ChannelDisplayController(QuPathViewer viewer) {
        this.viewer = viewer;
        this.display = viewer == null ? null : viewer.getImageDisplay();
        if (display != null) {
            display.selectedChannels().addListener(selectedListener);
            display.availableChannels().addListener(availableListener);
        }
    }

    void addStateListener(Runnable listener) {
        stateListeners.add(listener);
    }

    int getChannelCount() {
        return directChannels().size();
    }

    List<String> getChannelNames() {
        return directChannels().stream().map(ChannelDisplayInfo::getName).toList();
    }

    boolean isSupported() {
        if (viewer == null || viewer.getServer() == null || display == null)
            return false;
        var channels = directChannels();
        if (channels.size() != viewer.getServer().nChannels() || channels.size() < 2)
            return false;
        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i).getChannel() != i)
                return false;
        }
        return true;
    }

    ChannelSelectionState getState() {
        if (!isSupported())
            return ChannelSelectionState.unsupported();

        var direct = directChannels();
        var selected = new ArrayList<>(display.selectedChannels());
        Set<ChannelDisplayInfo> directSet = new HashSet<>(direct);

        if (selected.size() == direct.size() && directSet.containsAll(selected))
            return ChannelSelectionState.all(direct.size());

        if (selected.size() == 1 && selected.getFirst() instanceof DirectServerChannelInfo info) {
            int index = direct.indexOf(info);
            if (index >= 0)
                return ChannelSelectionState.single(index, direct.size());
        }
        return ChannelSelectionState.custom(direct.size());
    }

    void selectSliderValue(int value) {
        if (!isSupported())
            return;
        int channelCount = getChannelCount();
        int normalized = Math.max(0, Math.min(channelCount, value));
        changing = true;
        try {
            if (normalized == 0)
                selectAll();
            else
                selectSingle(normalized - 1);
            viewer.repaintEntireImage();
        } finally {
            changing = false;
            fireStateListeners();
        }
    }

    private void selectAll() {
        // Grayscale mode can only show one channel. Returning to "All" therefore
        // deliberately returns to QuPath's additive color-LUT mode.
        display.setUseGrayscaleLuts(false);
        var direct = directChannels();
        Set<ChannelDisplayInfo> directSet = new HashSet<>(direct);
        for (var selected : new ArrayList<>(display.selectedChannels())) {
            if (!directSet.contains(selected))
                display.setChannelSelected(selected, false);
        }
        for (var channel : direct)
            display.setChannelSelected(channel, true);
    }

    private void selectSingle(int index) {
        var direct = directChannels();
        var target = direct.get(index);
        display.setChannelSelected(target, true);
        for (var selected : new ArrayList<>(display.selectedChannels())) {
            if (selected != target)
                display.setChannelSelected(selected, false);
        }
    }

    private List<DirectServerChannelInfo> directChannels() {
        if (display == null)
            return List.of();
        return display.availableChannels().stream()
                .filter(DirectServerChannelInfo.class::isInstance)
                .map(DirectServerChannelInfo.class::cast)
                .sorted(Comparator.comparingInt(DirectServerChannelInfo::getChannel))
                .toList();
    }

    private void stateChanged() {
        if (!changing)
            fireStateListeners();
    }

    private void fireStateListeners() {
        for (var listener : List.copyOf(stateListeners))
            listener.run();
    }

    @Override
    public void close() {
        if (display != null) {
            display.selectedChannels().removeListener(selectedListener);
            display.availableChannels().removeListener(availableListener);
        }
        stateListeners.clear();
    }
}
