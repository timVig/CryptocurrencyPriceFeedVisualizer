package com.company.http;

import com.company.util.PriceTimestampPairs;
import com.company.util.StringPair;
import com.google.gson.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.*;

@Component
@PropertySource("classpath:application.properties")
public class HttpRequestHandler {

    @Value("${crypto-comp.token}")
    private String apiKey;

    @Value("${block-daemon.token}")
    private String apiKeyBlockDaemon;

    @Value("${wallet-address.address}")
    private String address;

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
        String timeunit = timePeriodToUnitsPoints.get(period).getUnit();
        String points = timePeriodToUnitsPoints.get(period).getPoints();
        URL url = new URL("https://min-api.cryptocompare.com/data/index/histo/underlying/"
                + timeunit + "?market=CCMVDA&base=" + coin + "&quote=USD&limit=" + points
                + "&api_key=" + apiKey );

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

    public void makeURICall2() throws IOException {
        URL url = new URL("https://svc.blockdaemon.com/universal/v1/ethereum/mainnet/account/" + address + "?apiKey=" + apiKeyBlockDaemon );
        URLConnection connection = url.openConnection();

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        JsonArray jObject = (JsonArray) new JsonParser().parse(in.readLine());

        JsonObject balanceObject = (JsonObject) jObject.get(0);
        BigInteger confirmedBalance = balanceObject.get("confirmed_balance").getAsBigInteger();
        BigInteger pendingBalance =  balanceObject.get("pending_balance").getAsBigInteger();

        JsonObject assetObject = (JsonObject) balanceObject.get("currency");
        String asset = assetObject.get("symbol").getAsString();

        System.out.println( "confirmed balance: " + confirmedBalance.divide( BigInteger.valueOf(1000000000L) ) );
        System.out.println( "pending balance: " + pendingBalance.divide( BigInteger.valueOf(1000000000L) ) );
        System.out.println( "asset: " + asset );
        System.out.println( jObject );

        //TODO: Get the symbols for the portfolio to display it
    }

    public List<String> makeURICall3() throws IOException {
        List<String> returnList = new ArrayList<>();
        URL url = new URL("https://svc.blockdaemon.com/universal/v1/ethereum/mainnet/account/" + address + "/txs?apiKey=" + apiKeyBlockDaemon );
        URLConnection connection = url.openConnection();

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        JsonObject jObject = (JsonObject) new JsonParser().parse(in);
        JsonArray dataObj = (JsonArray) jObject.get("data");
        
        StringBuilder blockSb = new StringBuilder();

        System.out.println( "-----------------------------------------------------" );
        for( JsonElement element : dataObj.asList() ){

            blockSb.append( "id:" + element.getAsJsonObject().get("id") );
            blockSb.append( "block_id:" + element.getAsJsonObject().get("block_id") );
            blockSb.append( "date:" + element.getAsJsonObject().get("date") );
            blockSb.append( "status:" + element.getAsJsonObject().get("status") );
            blockSb.append( "block_number:" + element.getAsJsonObject().get("block_number") );
            blockSb.append( "confirmations:" + element.getAsJsonObject().get("confirmations") );

            JsonElement events = element
                    .getAsJsonObject()
                    .get("events");

            StringBuilder eventSb;

            for( JsonElement event: events.getAsJsonArray() ){
                eventSb = new StringBuilder();
                eventSb.append("<html>").append("event id:")
                        .append(event.getAsJsonObject().get("id"));

                eventSb.append("<br>").append("transaction id:")
                        .append(event.getAsJsonObject().get("transaction_id"));

                eventSb.append("<br>").append("type:")
                        .append(event.getAsJsonObject().get("type"));

                eventSb.append("<br>").append("source:")
                        .append(event.getAsJsonObject().get("source"));

                eventSb.append("<br>").append("date:")
                        .append(new java.util.Date( event.getAsJsonObject().get("date").getAsLong() * 1000));

                eventSb.append("<br>").append("amount:")
                        .append(event.getAsJsonObject().get("amount").getAsBigInteger().divide(BigInteger.valueOf(1000000000L))
                        ).append(" ").append(event.getAsJsonObject().get("denomination").getAsString())
                        .append("<br>------------------------------------------------------------------------------------" +
                                "-------------------------------------------------------------</html>");
                returnList.add(eventSb.toString());
            }
            System.out.println( "-----------------------------------------------------" );
        }
        System.out.println( "-----------------------------------------------------" );

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement jsonElement = JsonParser.parseString(jObject.toString());
        String prettyJsonString = gson.toJson(jsonElement);
        //System.out.println(returnList);
        return returnList;
        //TODO: extract transaction data, no need to derive the securities
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
