package io.iridium.overvaults.config.vault.entry;

import com.google.gson.annotations.Expose;

public class SigilEntry {

    @Expose private final String type;

    public SigilEntry(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
