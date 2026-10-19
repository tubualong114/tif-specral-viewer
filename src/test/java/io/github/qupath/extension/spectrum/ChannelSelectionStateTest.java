package io.github.qupath.extension.spectrum;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelSelectionStateTest {

    @Test
    void createsAllState() {
        var state = ChannelSelectionState.all(28);
        assertEquals(ChannelSelectionState.Kind.ALL, state.kind());
        assertEquals(-1, state.channelIndex());
        assertEquals(28, state.channelCount());
    }

    @Test
    void createsSingleStateWithZeroBasedIndex() {
        var state = ChannelSelectionState.single(12, 23);
        assertEquals(ChannelSelectionState.Kind.SINGLE, state.kind());
        assertEquals(12, state.channelIndex());
        assertEquals(23, state.channelCount());
    }

    @Test
    void createsCustomAndUnsupportedStates() {
        assertEquals(ChannelSelectionState.Kind.CUSTOM, ChannelSelectionState.custom(20).kind());
        assertEquals(ChannelSelectionState.Kind.UNSUPPORTED, ChannelSelectionState.unsupported().kind());
    }
}
