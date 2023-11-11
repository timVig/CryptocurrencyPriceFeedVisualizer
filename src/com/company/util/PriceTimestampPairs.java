package com.company.util;

import java.util.Date;
import java.util.LinkedList;

public class PriceTimestampPairs {
    private LinkedList<Date> timestamps;
    private LinkedList<Double> prices;

    public PriceTimestampPairs(LinkedList<Date> timestamps, LinkedList<Double> prices){
        this.timestamps = timestamps;
        this.prices = prices;
    }

    public LinkedList<Date> getTimestamps() {
        return timestamps;
    }

    public LinkedList<Double> getPrices() {
        return prices;
    }
}
