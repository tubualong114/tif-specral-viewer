package io.github.qupath.extension.spectrum;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/** Isolates the opt-in GUI test from the user's installed QuPath preferences. */
public final class MemoryPreferencesFactory implements PreferencesFactory {
    private final Preferences root = new Node(null, "");

    @Override
    public Preferences systemRoot() { return root; }

    @Override
    public Preferences userRoot() { return root; }

    private static final class Node extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, Node> children = new HashMap<>();

        Node(AbstractPreferences parent, String name) { super(parent, name); }

        @Override
        protected void putSpi(String key, String value) { values.put(key, value); }
        @Override
        protected String getSpi(String key) { return values.get(key); }
        @Override
        protected void removeSpi(String key) { values.remove(key); }
        @Override
        protected void removeNodeSpi() { values.clear(); children.clear(); }
        @Override
        protected String[] keysSpi() { return values.keySet().toArray(String[]::new); }
        @Override
        protected String[] childrenNamesSpi() { return children.keySet().toArray(String[]::new); }
        @Override
        protected AbstractPreferences childSpi(String name) { return children.computeIfAbsent(name, n -> new Node(this, n)); }
        @Override
        protected void syncSpi() { }
        @Override
        protected void flushSpi() { }
    }
}
