package com.dayssky.mma.features.cz.data;

public enum CharmType {
    ABILITY(1.0),
    TREE(1.35),
    WILDCARD(1.8);

    private final double factor;

    private CharmType(double factor) {
        this.factor = factor;
    }

    public static CharmType byId(int typeId) {
        if (typeId < 4) {
            return ABILITY;
        } else {
            return typeId < 9 ? TREE : WILDCARD;
        }
    }

    public double factor() {
        return this.factor;
    }
}
