package com.company.ui;

import com.company.http.HttpRequestHandler;
import com.company.listeners.CustomButtonListener;
import com.company.listeners.StartListener;
import com.company.util.DoubleIndexPair;
import com.company.util.PriceTimestampPairs;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;


public class ChartUIDisplay {
    @Autowired
    HttpRequestHandler httpRequestHandler;

    private  final String[] coins = { "BTC", "ETH", "LTC", "XRP" };
    private  final String[] timeperiods = { "1 Year", "3 Month", "1 Month", "1 Week", "1 Day", "1 Hour" };
    public  XYChart chart;
    public  SwingWrapper<XYChart> wrapped;

    @Autowired
    public ChartUIDisplay( HttpRequestHandler h1 ){
        this.httpRequestHandler = h1;
    }

    public void UI_main() {
        chart = new XYChartBuilder().width( 1000 ).height( 800 ).title("CryptoTracker").
                xAxisTitle("Date").yAxisTitle("Price").build();
        wrapped = new SwingWrapper<>(chart);
        JFrame frame = wrapped.displayChart();
        frame.setLayout( new BorderLayout() );
        frame.setSize( chart.getWidth(), chart.getHeight() + 75 );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        startUI( frame );
        frame.setVisible( true );
    }

    /**
     * This method handles starting the UI, setting the layout, and linking listeners to buttons.
     * @param frame -> the frame to create UI on.
     */
    public void startUI( JFrame frame ){
        JPanel coinPanel = new JPanel();
        JLabel currentCoin = new JLabel( "BTC");
        JLabel currentPeriod = new JLabel( "1 Month");
        JButton startButton = new JButton("Search");
        JLabel searchCoin;
        JLabel searchTime;

        coinPanel.add( currentCoin );
        searchCoin = currentCoin;
        coinPanel.add( currentPeriod );
        searchTime = currentPeriod;
        coinPanel.add(startButton);
        startButton.addActionListener( new StartListener(searchCoin, searchTime, chart, wrapped, this) );

        for( String s: coins ){
            JButton coinButton = new JButton( s );
            coinButton.addActionListener( new CustomButtonListener( searchCoin,s ) );
            coinPanel.add(coinButton, BorderLayout.LINE_START);
        }

        for( String s: timeperiods ){
            JButton timeButton = new JButton( s );
            timeButton.addActionListener( new CustomButtonListener( searchTime,s ) );
            coinPanel.add(timeButton, BorderLayout.LINE_START);
        }

        frame.add( coinPanel, BorderLayout.SOUTH );
    }

    /**
     * This method calls the crypto-compare api to get data which can be displayed on the screen, the url is created off
     * the parameters entered, and the request is an Http GET, which sends back a json response. This response is then
     * parsed and displayed using the XChart Opensource library
     *
     * @param coin -> The coin to search for
     * @param wrapped -> the wrapper with our chart in it
     * @param chart -> the chart itself
     * @param period -> the time period we want data from
     */
    public void displayAnyChart(String coin, SwingWrapper wrapped, XYChart chart, String period )
            throws IOException, org.json.simple.parser.ParseException {
        PriceTimestampPairs results = this.httpRequestHandler.makeURICall( coin, period );
        LinkedList<Date> timestamp = results.getTimestamps();
        LinkedList<Double> price = results.getPrices();
        //get high and low here
        DoubleIndexPair max = getMaxFromList( price );
        DoubleIndexPair min = getMinFromList( price );

        clearChart( chart );
        chart.setTitle( period + " price of: " + coin );
        chart.addSeries(coin + " Price", timestamp, price );

        displayMaxPrice( max, timestamp );
        displayMinPrice( min, timestamp );

        displayOpenPrice( price.getFirst(), timestamp.getFirst() );
        displayClosePrice( price.getLast(), timestamp.getLast() );
        displayPercentChange(price.getFirst(), price.getLast(), timestamp.getFirst(), timestamp.getLast() );
        wrapped.repaintChart();
    }
    
    /**
     * This method clears any series which currently exists on the XYChart.
     * @param chart -> chart to clear.
     */
    public void clearChart( XYChart chart ){
        HashSet<String> remove = new HashSet<>();
        for( Map.Entry<String, XYSeries> entry: chart.getSeriesMap().entrySet() )
            remove.add(entry.getKey());
        for( String s: remove ) chart.removeSeries( s );
    }

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

    /**
     * This method creates a small line (8% of chart) on the maximum price on the chart,
     * as well as displaying it on the side
     * @param pair -> The maximum value/index pair
     * @param stamps -> index of all read timestamps
     */
    public void displayMaxPrice(DoubleIndexPair pair, LinkedList<Date> stamps ){
        LinkedList<Double> maxPrice = new LinkedList<>();
        LinkedList<Date> timestamps = new LinkedList<>();
        maxPrice.add( pair.getValue() );
        timestamps.add( stamps.get(pair.getIndex()) );
        int visualSpacing = (int) (( double ) stamps.size() / 25.0);

        if( pair.getIndex()-visualSpacing >= 0 ) {
            maxPrice.add( pair.getValue() );
            timestamps.add( stamps.get(pair.getIndex()-visualSpacing) );
        }

        if( pair.getIndex()+visualSpacing < stamps.size() ){
            maxPrice.add(pair.getValue());
            timestamps.add( stamps.get(pair.getIndex()+visualSpacing));
        }

        chart.addSeries("Max Price For Period: " + pair.getValue(), timestamps, maxPrice );
    }

    /**
     * This method creates a small line (8% of chart) on the minimum price on the chart,
     * as well as displaying it on the side
     * @param pair -> The minimum value/index pair
     * @param stamps -> index of all read timestamps
     */
    public void displayMinPrice(DoubleIndexPair pair, LinkedList<Date> stamps ){
        LinkedList<Double> minPrice = new LinkedList<>();
        LinkedList<Date> timestamps = new LinkedList<>();
        minPrice.add( pair.getValue() );
        timestamps.add( stamps.get(pair.getIndex()) );
        int visualSpacing = (int) (( double ) stamps.size() / 25.0);

        if( pair.getIndex()-visualSpacing >= 0 ) {
            minPrice.add( pair.getValue() );
            timestamps.add( stamps.get(pair.getIndex()-visualSpacing) );
        }

        if( pair.getIndex()+visualSpacing < stamps.size() ){
            minPrice.add(pair.getValue());
            timestamps.add( stamps.get(pair.getIndex()+visualSpacing));
        }

        chart.addSeries("Min Price For Period: " + pair.getValue(), timestamps, minPrice );
    }

    /**
     * This creates a dot on the chart defining the open price, and displays it on the side.
     * @param open -> the open price
     * @param stamp -> the open timestamp
     */
    public void displayOpenPrice( double open, Date stamp  ){
        LinkedList<Double> openPrice = new LinkedList<>();
        LinkedList<Date> timestamps = new LinkedList<>();
        openPrice.add( open );
        timestamps.add( stamp );
        chart.addSeries("Open Price For Period: " + open, timestamps, openPrice );
    }

    /**
     * This creates a dot on the chart defining the close price, and displays it on the side.
     * @param close -> the close price
     * @param stamp -> the close timestamp
     */
    public void displayClosePrice( double close, Date stamp  ){
        LinkedList<Double> closePrice = new LinkedList<>();
        LinkedList<Date> timestamps = new LinkedList<>();
        closePrice.add( close );
        timestamps.add( stamp );
        chart.addSeries("Close Price For Period: " + close, timestamps, closePrice );
    }

    /**
     * This creates a line on the graph and a text on the side displaying the percentage change of this period
     * @param open -> the open price
     * @param close -> the close price
     * @param openStamp -> open timestamp
     * @param closeStamp -> close timestamp
     */
    public void displayPercentChange( double open, double close, Date openStamp, Date closeStamp  ){
        LinkedList<Double> edgePrice = new LinkedList<>();
        LinkedList<Date> timestamps = new LinkedList<>();
        double percentChange = (( close / open ) - 1 ) * 100;
        DecimalFormat df = new DecimalFormat("#.##");
        edgePrice.add( open );
        edgePrice.add( close );
        timestamps.add( openStamp );
        timestamps.add( closeStamp );
        chart.addSeries("Percent Change For Period: " + df.format(percentChange), timestamps, edgePrice );
    }
}
