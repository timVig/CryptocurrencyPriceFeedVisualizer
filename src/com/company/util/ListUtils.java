package com.company.util;
import org.springframework.stereotype.Component;

import java.util.LinkedList;

@Component
public class ListUtils {

    public ListUtils() {}
    /**
     * This method return the maximum from a linked list
     * @return -> A Pair Containing the max double value and its index.
     */
    public DoubleIndexPair getMaxFromList(LinkedList<Double> list ){
        double max = 0; int index = 0; int count = 0;
        for( double i: list ){
            if( max < i ) {
                max = i;
                index = count;
            }
            count++;
        }

        DoubleIndexPair pair = new DoubleIndexPair( max, index );
        return pair;
    }

    /**
     * This method return the minimum from a linked list
     * @return -> A Pair Containing the min double value and its index.
     */
    public DoubleIndexPair getMinFromList(LinkedList<Double> list ){
        double min = Double.MAX_VALUE; int index = 0; int count = 0;
        for( double i: list ){
            if( min > i ) {
                min = i;
                index = count;
            }
            count++;
        }

        DoubleIndexPair pair = new DoubleIndexPair( min, index );
        return pair;
    }
}
