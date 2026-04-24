package io.iridium.overvaults.config.vault.entry;

import com.google.gson.annotations.Expose;
import iskallia.vault.config.Config;
import iskallia.vault.item.crystal.CrystalData;
import iskallia.vault.item.crystal.layout.CrystalLayout;
import iskallia.vault.item.crystal.objective.CrystalObjective;
import iskallia.vault.item.crystal.theme.CrystalTheme;
import iskallia.vault.item.crystal.time.CrystalTime;

import java.util.List;

public class CrystalDataEntry {

    @Expose private final CrystalObjective objective;
    @Expose private final List<WeightedObjectiveEntry> objectives;
    @Expose private final CrystalLayout layout;
    @Expose private final CrystalTheme theme;
    @Expose private final SigilEntry sigil;
    @Expose private final List<ModifierStackEntry> modifiers;
    @Expose private final CrystalTime time;
    @Expose private final Integer vaultLevel;
    @Expose private final Boolean rollRandomModifiers;

    public CrystalDataEntry(CrystalObjective objective, CrystalLayout layout, CrystalTheme theme, SigilEntry sigil, List<ModifierStackEntry> modifiers, CrystalTime time, int vaultLevel, boolean rollRandomModifiers) {
        this(objective, null, layout, theme, sigil, modifiers, time, vaultLevel, rollRandomModifiers);
    }

    public CrystalDataEntry(List<WeightedObjectiveEntry> objectives, CrystalLayout layout, CrystalTheme theme, SigilEntry sigil, List<ModifierStackEntry> modifiers, CrystalTime time, int vaultLevel, boolean rollRandomModifiers) {
        this(null, objectives, layout, theme, sigil, modifiers, time, vaultLevel, rollRandomModifiers);
    }

    private CrystalDataEntry(CrystalObjective objective, List<WeightedObjectiveEntry> objectives, CrystalLayout layout, CrystalTheme theme, SigilEntry sigil, List<ModifierStackEntry> modifiers, CrystalTime time, int vaultLevel, boolean rollRandomModifiers) {
        this.objective = objective;
        this.objectives = objectives;
        this.layout = layout;
        this.theme = theme;
        this.sigil = sigil;
        this.modifiers = modifiers;
        this.time = time;
        this.vaultLevel = vaultLevel;
        this.rollRandomModifiers = rollRandomModifiers;
    }

    public CrystalObjective getObjective() {
        return objective;
    }

    public List<WeightedObjectiveEntry> getObjectives() {
        return objectives;
    }

    public CrystalLayout getLayout() {
        return layout;
    }

    public CrystalTheme getTheme() {
        return theme;
    }

    public SigilEntry getSigil() {
        return sigil;
    }

    public List<ModifierStackEntry> getModifiers() {
        return modifiers;
    }

    public CrystalTime getTime() {
        return time;
    }

    public Integer getVaultLevel() {
        return vaultLevel;
    }

    public Boolean getRollRandomModifiers() {
        return rollRandomModifiers;
    }

    public CrystalObjective getRandomObjective() {
        if (objectives == null || objectives.isEmpty()) {
            return objective;
        }

        int totalWeight = 0;
        for (WeightedObjectiveEntry weightedObjective : objectives) {
            if (weightedObjective != null && weightedObjective.isValid()) {
                totalWeight += weightedObjective.getWeight();
            }
        }

        if (totalWeight <= 0) {
            return objective;
        }

        int target = Config.rand.nextInt(totalWeight);
        for (WeightedObjectiveEntry weightedObjective : objectives) {
            if (weightedObjective == null || !weightedObjective.isValid()) {
                continue;
            }

            target -= weightedObjective.getWeight();
            if (target < 0) {
                return weightedObjective.getObjective();
            }
        }

        return objective;
    }

    public static void applyData(CrystalDataEntry entry, CrystalData crystal) {
        CrystalObjective objective = entry.getRandomObjective();

        if(objective != null) crystal.setObjective(objective);
        if(entry.layout != null) crystal.setLayout(entry.layout);
        if(entry.theme != null) crystal.setTheme(entry.theme);
        if(entry.sigil != null) crystal.setSigil(entry.sigil.getType());
        if(entry.time != null) crystal.setTime(entry.time);
        if(entry.modifiers != null) crystal.getModifiers().getList().addAll(ModifierStackEntry.getModifiers(entry.modifiers));
        if(entry.vaultLevel != null) crystal.getProperties().setLevel(entry.vaultLevel);
        if(entry.rollRandomModifiers != null) crystal.getModifiers().setRandomModifiers(entry.rollRandomModifiers);
    }

    public static CrystalData applyData(CrystalDataEntry entry) {
        CrystalData crystal = CrystalData.empty();
        CrystalObjective objective = entry.getRandomObjective();

        if(objective != null) crystal.setObjective(objective);
        if(entry.layout != null) crystal.setLayout(entry.layout);
        if(entry.theme != null) crystal.setTheme(entry.theme);
        if(entry.sigil != null) crystal.setSigil(entry.sigil.getType());
        if(entry.time != null) crystal.setTime(entry.time);
        if(entry.modifiers != null) crystal.getModifiers().getList().addAll(ModifierStackEntry.getModifiers(entry.modifiers));
        if(entry.vaultLevel != null) crystal.getProperties().setLevel(entry.vaultLevel);
        if(entry.rollRandomModifiers != null) crystal.getModifiers().setRandomModifiers(entry.rollRandomModifiers);

        return crystal;
    }

}
