package io.iridium.overvaults.config.vault.entry;

import com.google.gson.annotations.Expose;
import iskallia.vault.item.crystal.objective.CrystalObjective;

public class WeightedObjectiveEntry {

    @Expose private final int weight;
    @Expose private final CrystalObjective objective;

    public WeightedObjectiveEntry(int weight, CrystalObjective objective) {
        this.weight = weight;
        this.objective = objective;
    }

    public int getWeight() {
        return weight;
    }

    public CrystalObjective getObjective() {
        return objective;
    }

    public boolean isValid() {
        return weight > 0 && objective != null;
    }
}
