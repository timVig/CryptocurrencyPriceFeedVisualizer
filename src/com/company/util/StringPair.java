package com.company.util;

/**
 * Class defined for mapping a timeperiod (i.e 1 year) to the units/#OfPointsToPoll (i.e. days/365 for 1 year)
 */
public class StringPair{
    private String unit;
    private String points;

    public StringPair(String u, String p){
        this.unit = u; this.points = p;
    }

    public String getUnit() {
        return unit;
    }

    public String getPoints() {
        return points;
    }
}
