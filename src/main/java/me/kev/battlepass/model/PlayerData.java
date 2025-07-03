package me.kev.battlepass.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents player progress, current XP, tier, and claimed rewards.
 */
public class PlayerData {
    private int currentXP;
    private int currentTier;
    private Set<Integer> claimedTiers = new HashSet<>();

    public int getCurrentXP() {
        return currentXP;
    }

    public void setCurrentXP(int currentXP) {
        this.currentXP = currentXP;
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(int currentTier) {
        this.currentTier = currentTier;
    }

    public Set<Integer> getClaimedTiers() {
        return claimedTiers;
    }

    public void claimTier(int tier) {
        claimedTiers.add(tier);
    }

    public boolean hasClaimed(int tier) {
        return claimedTiers.contains(tier);
    }
}

