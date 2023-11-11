package com.company.http;

import com.company.util.PriceTimestampPairs;
import com.company.util.StringPair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

@Configuration
public class HttpRequestHandler {
    private HashMap<String, StringPair> timePeriodToUnitsPoints;
    private  final String[] timeunits = { "day", "day", "day", "hour", "hour", "minute" };
    private  final String[] pointsToPoll = { "365", "90", "30", "168", "24", "60" };
    private  final String[] timeperiods = { "1 Year", "3 Month", "1 Month", "1 Week", "1 Day", "1 Hour" };

    public HttpRequestHandler() {
        this.timePeriodToUnitsPoints = new HashMap<>();
        for( int i = 0; i < timeunits.length; i++ )
            timePeriodToUnitsPoints.put( timeperiods[i], new StringPair(timeunits[i], pointsToPoll[i]));
    }

    public PriceTimestampPairs makeURICall(String coin, String period ) throws IOException, ParseException {
        System.out.println(period);
        System.out.println(timePeriodToUnitsPoints);
        String timeunit = timePeriodToUnitsPoints.get(period).getUnit();
        String points = timePeriodToUnitsPoints.get(period).getPoints();
        URL url = new URL("https://min-api.cryptocompare.com/data/index/histo/underlying/"
                + timeunit + "?market=CCMVDA&base=" + coin + "&quote=USD&limit=" + points
                + "&api_key=d217db56f263194685d0ad74d721d7925ed587b6067b46e6ca56ee889d286286");

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

        return new PriceTimestampPairs( timestamp, price );
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
    private void addTodaysPrice(URL url, JSONParser parse, LinkedList<Date> timestamp, LinkedList<Double> price )
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
    private double doubleValue( Object value ){
        return( value instanceof Number ) ? ((Number)value).doubleValue() : -1.0;
    }
}
