package io.github.qupath.extension.spectrum;

/**
 * A UI-friendly summary of QuPath's current channel selection.
 */
record ChannelSelectionState(Kind kind, int channelIndex, int channelCount) {

    enum Kind {
        ALL,
        SINGLE,
        CUSTOM,
        UNSUPPORTED
    }

    static ChannelSelectionState all(int count) {
        return new ChannelSelectionState(Kind.ALL, -1, count);
    }

    static ChannelSelectionState single(int index, int count) {
        return new ChannelSelectionState(Kind.SINGLE, index, count);
    }

    static ChannelSelectionState custom(int count) {
        return new ChannelSelectionState(Kind.CUSTOM, -1, count);
    }

    static ChannelSelectionState unsupported() {
        return new ChannelSelectionState(Kind.UNSUPPORTED, -1, 0);
    }
}
