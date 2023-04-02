package net.sf.rails.game;

/** Class Revenue is a wrapper around different revenue values.
 *  Specifically, it is intended for the 'direct' revenue amounts
 *  of 1837 and 18VA that go directly into the company treasury
 *  rather than being paid out as dividends.
 *
 *  Currently, this class is not yet being applied in the whole
 *  algorithms tombola, but only by the other new class Stops
 *  and the 18VA TrainRunModifier.
 *
 *  Created 04/2023 by Erik Vos
 */
public class Revenue {

    private int normalRevenue = 0;
    private int specialRevenue = 0;

    public Revenue(int normalRevenue, int specialRevenue) {
        this.normalRevenue = normalRevenue;
        this.specialRevenue = specialRevenue;
    }

    public Revenue (int normalRevenue) {
        this.normalRevenue = normalRevenue;
    }

    public int getNormalRevenue() {
        return normalRevenue;
    }

    public void setNormalRevenue(int normalRevenue) {
        this.normalRevenue = normalRevenue;
    }

    public int getSpecialRevenue() {
        return specialRevenue;
    }

    public void setSpecialRevenue(int specialRevenue) {
        this.specialRevenue = specialRevenue;
    }

    public Revenue addNormalRevenue (int normalRevenue) {
        this.normalRevenue += normalRevenue;
        return this;
    }

    public Revenue addSpecialRevenue (int specialRevenue) {
        this.specialRevenue += specialRevenue;
        return this;
    }

    public Revenue addRevenue (int normalRevenue, int specialRevenue) {
        this.normalRevenue += normalRevenue;
        this.specialRevenue += specialRevenue;
        return this;
    }

    public Revenue addRevenue (Revenue revenue) {
        this.normalRevenue += revenue.normalRevenue;
        this.specialRevenue += revenue.specialRevenue;
        return this;
    }

    public Revenue multiplyRevenue (int factor) {
        normalRevenue *= factor;
        specialRevenue *= factor;
        return this;
    }

    public String toString() {
        return "{" + normalRevenue + "," + specialRevenue + "}";
    }

}
