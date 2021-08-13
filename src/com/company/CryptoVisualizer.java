package com.company;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.knowm.xchart.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.*;

/**
 * This program displays the price feeds of popular cryptocurrencies on popular time intervals.
 * Currently contains Bitcoin, Ethereum, Litecoin, and Ripple (XRP).
 * Currently spans times of 1 year, 3 months, 1 month, 1 week, 1 day, and 1 hour.
 * USes the XChart Opensource library in order to visualize the data.
 * Price feeds are gotten from min-api.cryptocompare.com.
 */
public class CryptoVisualizer {
    private static final String[] coins = { "BTC", "ETH", "LTC", "XRP" };
    private static final String[] timeperiods = { "1 Year", "3 Month", "1 Month", "1 Week", "1 Day", "1 Hour" };
    private static final String[] timeunits = { "day", "day", "day", "hour", "hour", "minute" };
    private static final String[] pointsToPoll = { "365", "90", "30", "168", "24", "60" };
    private static HashMap<String, StringPair> timePeriodToUnitsPoints = new HashMap<>();
    public static JLabel searchCoin;
    public static JLabel searchTime;
    public static XYChart chart;
    public static SwingWrapper<XYChart> wrapped;

    /**
     * The main method which start the cryptovisualizer.
     */
    public static void main(String[] args) {
        for( int i = 0; i < timeunits.length; i++ )
            timePeriodToUnitsPoints.put( timeperiods[i], new StringPair(timeunits[i], pointsToPoll[i]));
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
    public static void startUI( JFrame frame ){
        JPanel coinPanel = new JPanel();
        JLabel currentCoin = new JLabel( "BTC");
        JLabel currentPeriod = new JLabel( "1 Month");
        JButton startButton = new JButton("Search");

        coinPanel.add( currentCoin );
        searchCoin = currentCoin;
        coinPanel.add( currentPeriod );
        searchTime = currentPeriod;
        coinPanel.add(startButton);
        startButton.addActionListener( new StartListener() );

        for( String s: coins ){
            JButton coinButton = new JButton( s );
            coinButton.addActionListener( new CoinListener(s) );
            coinPanel.add(coinButton, BorderLayout.LINE_START);
        }

        for( String s: timeperiods ){
            JButton timeButton = new JButton( s );
            timeButton.addActionListener( new TimeListener(s) );
            coinPanel.add(timeButton, BorderLayout.LINE_START);
        }

        frame.add( coinPanel, BorderLayout.SOUTH );
    }

    /**
     * This method clears any series which currently exists on the XYChart.
     * @param chart -> chart to clear.
     */
    public static void clearChart( XYChart chart ){
        HashSet<String> remove = new HashSet<>();
        for( Map.Entry<String, XYSeries> entry: chart.getSeriesMap().entrySet() )
            remove.add(entry.getKey());
        for( String s: remove ) chart.removeSeries( s );
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
    public static void displayAnyChart(String coin, SwingWrapper wrapped, XYChart chart, String period )
            throws IOException, org.json.simple.parser.ParseException {
        String timeunit = timePeriodToUnitsPoints.get(period).unit;
        String points = timePeriodToUnitsPoints.get(period).points;
        URL url = new URL("https://min-api.cryptocompare.com/data/index/histo/underlying/"
                + timeunit + "?market=CCMVDA&base=" + coin + "&quote=USD&limit=" + points
                + "&api_key=YourAPIKeyGoesHere");

        URLConnection connection = url.openConnection();
        BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
        JSONParser parse = new JSONParser();
        JSONObject jObject = (JSONObject) parse.parse(in.readLine());
        JSONArray jArray = (JSONArray) parse.parse( jObject.get("Data").toString() );
        LinkedList<Date> timestamp = new LinkedList<>();
        LinkedList<Double> price = new LinkedList<>();

        for( int i = 0; i < jArray.size(); i++ ){
            jObject = (JSONObject) jArray.get(i);
            Date date = new Date( new Timestamp( (Long) jObject.get("time") * 1000 ).getTime() );

            double tryDouble = doubleValue(jObject.get("open"));
            if( tryDouble != 0 && tryDouble != -1.0 ){ //0 = data not available, -1.0 means error in json data
                timestamp.addLast( date );
                price.addLast( tryDouble );
            }
        }

        url = new URL( "https://min-api.cryptocompare.com/data/price?fsym=" + coin + "&tsyms=USD" );
        addTodaysPrice( url, parse, timestamp, price );

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
     * This method return the maximum from a linked list
     * @return -> A Pair Containing the max double value and its index.
     */
    public static DoubleIndexPair getMaxFromList( LinkedList<Double> list ){
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
    public static DoubleIndexPair getMinFromList( LinkedList<Double> list ){
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
    public static void displayMaxPrice( DoubleIndexPair pair, LinkedList<Date> stamps ){
        LinkedList<Double> maxPrice = new LinkedList<>();
        LinkedList<Date> timestamps = new LinkedList<>();
        maxPrice.add( pair.value );
        timestamps.add( stamps.get(pair.index) );
        int visualSpacing = (int) (( double ) stamps.size() / 25.0);

        if( pair.index-visualSpacing >= 0 ) {
            maxPrice.add( pair.value );
            timestamps.add( stamps.get(pair.index-visualSpacing) );
        }

        if( pair.index+visualSpacing < stamps.size() ){
            maxPrice.add(pair.value);
            timestamps.add( stamps.get(pair.index+visualSpacing));
        }

        chart.addSeries("Max Price For Period: " + pair.value, timestamps, maxPrice );
    }

    /**
     * This method creates a small line (8% of chart) on the minimum price on the chart,
     * as well as displaying it on the side
     * @param pair -> The minimum value/index pair
     * @param stamps -> index of all read timestamps
     */
    public static void displayMinPrice( DoubleIndexPair pair, LinkedList<Date> stamps ){
        LinkedList<Double> minPrice = new LinkedList<>();
        LinkedList<Date> timestamps = new LinkedList<>();
        minPrice.add( pair.value );
        timestamps.add( stamps.get(pair.index) );
        int visualSpacing = (int) (( double ) stamps.size() / 25.0);

        if( pair.index-visualSpacing >= 0 ) {
            minPrice.add( pair.value );
            timestamps.add( stamps.get(pair.index-visualSpacing) );
        }

        if( pair.index+visualSpacing < stamps.size() ){
            minPrice.add(pair.value);
            timestamps.add( stamps.get(pair.index+visualSpacing));
        }

        chart.addSeries("Min Price For Period: " + pair.value, timestamps, minPrice );
    }

    /**
     * This creates a dot on the chart defining the open price, and displays it on the side.
     * @param open -> the open price
     * @param stamp -> the open timestamp
     */
    public static void displayOpenPrice( double open, Date stamp  ){
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
    public static void displayClosePrice( double close, Date stamp  ){
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
    public static void displayPercentChange( double open, double close, Date openStamp, Date closeStamp  ){
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

    /**
     * Adds todays current price as the last piece of data on a chart, following polls for historical information.
     * @param url -> The url to httpRequest to
     * @param parse -> the parser for the json response
     * @param timestamp -> the timestamp list for the x-axis
     * @param price -> the price list for the y-axis
     * @throws IOException -> invalid url
     * @throws org.json.simple.parser.ParseException -> cannot parse response
     */
    public static void addTodaysPrice(URL url, JSONParser parse, LinkedList<Date> timestamp, LinkedList<Double> price )
            throws IOException, org.json.simple.parser.ParseException {
        String inputLine;
        JSONObject jObject;
        BufferedReader in;
        URLConnection connection = url.openConnection();
        in = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
        inputLine = in.readLine();
        jObject = (JSONObject) parse.parse(inputLine);
        Date date = new Date( new Timestamp( System.currentTimeMillis() ).getTime() );
        timestamp.addLast( date );
        price.addLast( doubleValue(jObject.get("USD")) );
    }

    /**
     * Deals with the fact that the JSonArrays inner workings may be a Long or a Double, which cannot convert between.
     * @param value -> the value to cast to a double
     * @return -> the value, or an error code (-1.0)
     */
    private static double doubleValue( Object value ){
        return( value instanceof Number ) ? ((Number)value).doubleValue() : -1.0;
    }

    /**
     * This class defines a listener which is linked to the search button, which tells it to display the chart with the
     * currently entered search parameters.
     */
    private static class StartListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            String coin = searchCoin.getText();
            String period = searchTime.getText();
            try { displayAnyChart( coin, wrapped, chart , period); }
            catch (IOException | ParseException exception ) { exception.printStackTrace(); }
        }
    }

    /**
     * This class defines a listener which is linked to any button has a coin label, in order to update the search term
     * for the coin we want to look for to the one on the button.
     */
    private static class CoinListener implements ActionListener{
        String coin;
        public CoinListener( String coin ){
            this.coin = coin;
        }
        @Override public void actionPerformed(ActionEvent e) { searchCoin.setText( this.coin ); }
    }

    /**
     * This class defines a listener which is linked to any button has a time label, in order to update the search term
     * for the time we want to look for to the one on the button.
     */
    private static class TimeListener implements ActionListener{
        String period;
        public TimeListener( String period ){
            this.period = period;
        }
        @Override public void actionPerformed(ActionEvent e) { searchTime.setText( this.period ); }
    }

    /**
     * Class defined for mapping a timeperiod (i.e 1 year) to the units/#OfPointsToPoll (i.e. days/365 for 1 year)
     */
    private static class StringPair{
        String unit; String points;

        private StringPair( String u, String p ){
            this.unit = u; this.points = p;
        }
    }

    /**
     * Class defined for mapping a maximum/minimum double value to its index in a list/array
     */
    private static class DoubleIndexPair{
        double value; int index;
        public DoubleIndexPair( double val, int index ){
            this.value = val;
            this.index = index;
        }
    }
}
