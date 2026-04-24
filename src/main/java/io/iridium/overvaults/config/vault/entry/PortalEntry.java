package io.iridium.overvaults.config.vault.entry;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PortalEntry {
    @Expose private final CrystalDataEntry crystalData;
    @Expose private final boolean shouldDecay;
    @Expose @SerializedName(value = "decayTime", alternate = {"secondsUntilDecay"})
    private final Integer decayTime;
    @Expose @SerializedName(value = "portalOpenLang", alternate = {"activationTranslationComponent", "translationComponent"})
    private final String portalOpenLang;
    @Expose @SerializedName(value = "loginMessage", alternate = {"loginTranslationComponent"})
    private final String loginMessage;
    @Expose @SerializedName(value = "portalDecayed", alternate = {"decayTranslationComponent"})
    private final String portalDecayed;

    public PortalEntry(CrystalDataEntry crystalData, String translationComponent, boolean shouldDecay) {
        this(crystalData, translationComponent, "overvaults.portal.login", "overvaults.portal.decay", -1, shouldDecay);
    }

    public PortalEntry(CrystalDataEntry crystalData, String portalOpenLang, String loginMessage, String portalDecayed, int decayTime, boolean shouldDecay) {
        this.crystalData = crystalData;
        this.shouldDecay = shouldDecay;
        this.decayTime = decayTime;
        this.portalOpenLang = portalOpenLang;
        this.loginMessage = loginMessage;
        this.portalDecayed = portalDecayed;
    }

    public CrystalDataEntry getCrystalData() {
        return crystalData;
    }

    public String getPortalOpenLang() {
        return portalOpenLang == null ? "overvaults.portal.login" : portalOpenLang;
    }

    public String getLoginMessage() {
        return loginMessage == null ? "overvaults.portal.login" : loginMessage;
    }

    public String getPortalDecayed() {
        return portalDecayed == null ? "overvaults.portal.decay" : portalDecayed;
    }

    public int getDecayTime() {
        return decayTime == null ? -1 : decayTime;
    }

    public boolean shouldPortalDecay() {
        return shouldDecay;
    }
}
