package com.company.ui;

import com.company.http.HttpRequestHandler;
import com.company.listeners.CustomButtonListener;
import com.company.listeners.StartListener;
import com.company.util.DoubleIndexPair;
import com.company.util.ListUtils;
import com.company.util.PriceTimestampPairs;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class ChartUIDisplay {
    @Autowired
    HttpRequestHandler httpRequestHandler;

    @Autowired
    ListUtils utils;

    private  final String[] coins = { "BTC", "ETH", "LTC", "XRP" };
    private  final String[] timeperiods = { "1 Year", "3 Month", "1 Month", "1 Week", "1 Day", "1 Hour" };
    public  XYChart chart;
    public  SwingWrapper<XYChart> wrapped;

    @Autowired
    public ChartUIDisplay( HttpRequestHandler h1 ){
        this.httpRequestHandler = h1;
    }

    public void UI_main() throws IOException {
        chart = new XYChartBuilder().width( 1000 ).height( 800 ).title("CryptoTracker").
                xAxisTitle("Date").yAxisTitle("Price").build();
        wrapped = new SwingWrapper<>(chart);
        JFrame frame = wrapped.displayChart();
        frame.setLayout( new BorderLayout() );
        frame.setSize( chart.getWidth() + 700, chart.getHeight() + 75 );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        startUI( frame );
        frame.setVisible( true );
    }

    /**
     * This method handles starting the UI, setting the layout, and linking listeners to buttons.
     * @param frame -> the frame to create UI on.
     */
    public void startUI( JFrame frame ) throws IOException {
        JPanel coinPanel = new JPanel();
        JLabel currentCoin = new JLabel( "BTC");
        JLabel currentPeriod = new JLabel( "1 Month");



        JButton startButton = new JButton("Search");
        JLabel searchCoin, searchTime;

        coinPanel.add( currentCoin );
        searchCoin = currentCoin;
        coinPanel.add( currentPeriod );
        searchTime = currentPeriod;
        coinPanel.add(startButton);
        startButton.addActionListener( new StartListener(searchCoin, searchTime, chart, wrapped, this) );

        Arrays.stream(coins).forEach( ( c -> {
            JButton coinButton = new JButton( c );
            coinButton.addActionListener( new CustomButtonListener( searchCoin,c ) );
            coinPanel.add(coinButton, BorderLayout.LINE_START);
        }));

        Arrays.stream(timeperiods).forEach( (s) -> {
            JButton timeButton = new JButton( s );
            timeButton.addActionListener( new CustomButtonListener( searchTime,s ) );
            coinPanel.add(timeButton, BorderLayout.LINE_START);
        } );

        DefaultListModel listModel = new DefaultListModel();
        List<String> txnsApi = this.httpRequestHandler.makeURICall3();
        JList transactions = new JList(listModel);
        for( String s: txnsApi ){
            listModel.addElement(s);
        }
        JScrollPane txnPanel = new JScrollPane(transactions);
        ( (DefaultListCellRenderer) transactions.getCellRenderer() ).setHorizontalAlignment( SwingConstants.LEFT );
        txnPanel.setViewportView( transactions );
        txnPanel.setSize(1000, 800);
        frame.add( txnPanel, BorderLayout.EAST );

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
    public void displayAnyChart(String coin, SwingWrapper wrapped, XYChart chart, String period)
            throws IOException, org.json.simple.parser.ParseException {
        this.httpRequestHandler.makeURICall2();
        this.httpRequestHandler.makeURICall3();
        PriceTimestampPairs results = this.httpRequestHandler.makeURICall( coin, period );
        LinkedList<Date> timestamp = results.getTimestamps();
        LinkedList<Double> price = results.getPrices();

        clearChart( chart );
        chart.setTitle( period + " price of: " + coin );
        chart.addSeries(coin + " Price", timestamp, price );

        displayMaxOrMinPrice( utils.getMaxFromList( price ), timestamp, "MIN" );
        displayMaxOrMinPrice( utils.getMinFromList( price ), timestamp, "MAX" );

        displayOpenOrClosePrice( price.getFirst(), timestamp.getFirst(), "OPEN" );
        displayOpenOrClosePrice( price.getLast(), timestamp.getLast(), "CLOSE" );
        displayPercentChange(price.getFirst(), price.getLast(), timestamp.getFirst(), timestamp.getLast() );

        wrapped.repaintChart();
    }
    
    /**
     * This method clears any series which currently exists on the XYChart.
     * @param chart -> chart to clear.
     */
    public void clearChart( XYChart chart ){
        HashSet<String> remove = new HashSet<>();
        chart.getSeriesMap().forEach((key, value) -> remove.add(key));
        remove.forEach(chart::removeSeries);
    }

    /**
     * This method creates a small line (8% of chart) on the maximum price on the chart,
     * as well as displaying it on the side
     * @param pair -> The maximum value/index pair
     * @param stamps -> index of all read timestamps
     */
    public void displayMaxOrMinPrice(DoubleIndexPair pair, LinkedList<Date> stamps, String minMax ){
        LinkedList<Double> priceSeries = new LinkedList<>( List.of(pair.getValue() ) );
        LinkedList<Date> timestamps = new LinkedList<>( List.of( stamps.get( pair.getIndex() ) ) );
        int visualSpacing = (int) (( double ) stamps.size() / 25.0);

        if( pair.getIndex()-visualSpacing >= 0 ) {
            priceSeries.add( pair.getValue() );
            timestamps.add( stamps.get(pair.getIndex()-visualSpacing) );
        }

        if( pair.getIndex()+visualSpacing < stamps.size() ){
            priceSeries.add(pair.getValue());
            timestamps.add( stamps.get(pair.getIndex()+visualSpacing));
        }

        if( minMax.equals("MAX") ){
            chart.addSeries("Max Price For Period: " + pair.getValue(), timestamps, priceSeries );
        } else if ( minMax.equals("MIN") ){
            chart.addSeries("Min Price For Period: " + pair.getValue(), timestamps, priceSeries );
        }
    }

    /**
     * This creates a dot on the chart defining the open price, and displays it on the side.
     * @param price-> the open price
     * @param stamp -> the open timestamp
     */
    public void displayOpenOrClosePrice( double price, Date stamp, String closeOpen ){
        LinkedList<Double> priceSeries = new LinkedList<>( List.of(price) );
        LinkedList<Date> timestamps = new LinkedList<>( List.of(stamp) );
        if( closeOpen.equals("OPEN") ){
            chart.addSeries("Open Price For Period: " + price, timestamps, priceSeries );
        } else if ( closeOpen.equals("CLOSE") ){
            chart.addSeries( "Close Price For Period: " + price, timestamps, priceSeries );
        }
    }

    /**
     * This creates a line on the graph and a text on the side displaying the percentage change of this period
     * @param open -> the open price
     * @param close -> the close price
     * @param openStamp -> open timestamp
     * @param closeStamp -> close timestamp
     */
    public void displayPercentChange( double open, double close, Date openStamp, Date closeStamp  ){
        LinkedList<Double> edgePrice = new LinkedList<>( List.of( open, close ) );
        LinkedList<Date> timestamps = new LinkedList<>( List.of( openStamp, closeStamp) );
        double percentChange = (( close / open ) - 1 ) * 100;
        DecimalFormat df = new DecimalFormat("#.##");
        chart.addSeries("Percent Change For Period: " + df.format(percentChange), timestamps, edgePrice );
    }
}