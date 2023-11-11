package com.company;

import com.company.http.HttpRequestHandler;
import com.company.ui.ChartUIDisplay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * This program displays the price feeds of popular cryptocurrencies on popular time intervals.
 * Currently contains Bitcoin, Ethereum, Litecoin, and Ripple (XRP).
 * Currently spans times of 1 year, 3 months, 1 month, 1 week, 1 day, and 1 hour.
 * USes the XChart Opensource library in order to visualize the data.
 * Price feeds are gotten from min-api.cryptocompare.com.
 */
@ComponentScan
public class CryptoVisualizer {
    static ChartUIDisplay chartUIDisplay;

    /**
     * The main method which start the cryptovisualizer.
     */
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(HttpRequestHandler.class, ChartUIDisplay.class);
        chartUIDisplay = context.getBean(ChartUIDisplay.class);
        chartUIDisplay.UI_main();
    }
}
