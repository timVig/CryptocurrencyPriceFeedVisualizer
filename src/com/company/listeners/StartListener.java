package com.company.listeners;

import com.company.ui.ChartUIDisplay;
import org.json.simple.parser.ParseException;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * This class defines a listener which is linked to the search button, which tells it to display the chart with the
 * currently entered search parameters.
 */
public class StartListener implements ActionListener {

    private JLabel searchCoin;
    private JLabel searchTime;
    private XYChart chart;
    private SwingWrapper wrapper;

    ChartUIDisplay display;
    public StartListener( JLabel searchCoin, JLabel searchTime, XYChart chart, SwingWrapper wrapper, ChartUIDisplay display ) {
        this.searchCoin = searchCoin;
        this.searchTime = searchTime;
        this.chart = chart;
        this.wrapper = wrapper;
        this.display = display;
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        String coin = searchCoin.getText();
        String period = searchTime.getText();
        try { display.displayAnyChart( coin, wrapper, chart , period); }
        catch (IOException | ParseException exception ) { exception.printStackTrace(); }
    }
}
