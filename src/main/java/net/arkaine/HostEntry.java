package net.arkaine;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class HostEntry {
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }


    private String content;
    private BooleanProperty enabled = new SimpleBooleanProperty();

    public HostEntry(String line) {
        if (line.startsWith("#")) {
            this.content = line.substring(1).trim();
            enabled.set(false);
        } else {
            this.content = line.trim();
            enabled.set(true);
        }

        enabled.addListener((obs, oldVal, newVal) -> {
            // Ne rien faire ici car la mise à jour du fichier se fait via la cellule
        });
    }

    public String getContent() {
        return content;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    @Override
    public String toString() {
        return enabled.get() ? content : "#" + content;
    }
}