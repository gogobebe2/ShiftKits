package com.gmail.gogobebe2.shiftkits.kit;

import org.bukkit.entity.Player;

public class Cost extends Requirement {
    private int price;

    public Cost(int price) {
        super(price + " XP");
        this.price = price;
    }

    @Override
    protected boolean satisfies(Player player) {
        // TODO: hook into xp plugin to find xp.
        return false;
    }

    protected void takeXP(Player player) {
        // TODO: hook into xp plugin to find xp.
    }
}
