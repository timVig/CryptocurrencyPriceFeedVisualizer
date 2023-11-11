package com.company.util;

/**
 * Class defined for mapping a maximum/minimum double value to its index in a list/array
 */
public class DoubleIndexPair{
    private double value;
    private int index;

    public DoubleIndexPair(double val, int index ){
        this.value = val;
        this.index = index;
    }

    public double getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }
}
