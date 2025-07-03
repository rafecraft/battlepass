package me.kev.battlepass.model;

import java.util.List;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a battlepass tier, its XP requirement, and rewards.
 */
public class Tier {
    private int tierNumber;
    private int xpRequired;
    private List<ItemStack> rewards;

    public Tier(int tierNumber, int xpRequired, List<ItemStack> rewards) {
        this.tierNumber = tierNumber;
        this.xpRequired = xpRequired;
        this.rewards = rewards;
    }

    public int getTierNumber() {
        return tierNumber;
    }

    public int getXpRequired() {
        return xpRequired;
    }

    public List<ItemStack> getRewards() {
        return rewards;
    }
}


