package si.nerve.flightchecker.pages;

import java.awt.Color;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import javax.swing.JLabel;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import si.nerve.flightchecker.FlightsGui;
import si.nerve.flightchecker.components.MultiCityFlightTableModel;
import si.nerve.flightchecker.data.FlightLeg;
import si.nerve.flightchecker.data.MultiCityFlightData;
import si.nerve.flightchecker.data.PriceType;
import si.nerve.flightchecker.helper.Helper;

/**
 * Created: 10.8.13 20:39
 */
public class EdreamsFlightObtainer implements MultiCityFlightObtainer
{
  private SimpleDateFormat m_formatter;
  private static final Logger LOG = Logger.getLogger(EdreamsFlightObtainer.class);

  @Override
  public void search(
      final FlightsGui flightGui, JLabel statusLabel, String addressRoot, String from1, String to1, Date date1,
      String from2, String to2, Date date2, String from3, String to3, Date date3, Integer numOfPersons) throws Exception
  {
    if ("com".equals(addressRoot))
    {
      m_formatter = new SimpleDateFormat("dd/MM/yyyy");
    }
    else if ("de".equals(addressRoot) || "dk".equals(addressRoot) || "at".equals(addressRoot))
    {
      m_formatter = new SimpleDateFormat("dd.MM.yyyy");
    }
    else if ("nl".equals(addressRoot))
    {
      m_formatter = new SimpleDateFormat("dd-MM-yyyy");
    }
    else if ("it".equals(addressRoot) || "co.uk".equals(addressRoot) || "es".equals(addressRoot) || "fr".equals(addressRoot) || "pl".equals(addressRoot)
        || "ca".equals(addressRoot) || "ie".equals(addressRoot) || "be".equals(addressRoot) || "ir".equals(addressRoot))
    {
      m_formatter = new SimpleDateFormat("dd/MM/yyyy");
    }
    else if ("se".equals(addressRoot))
    {
      m_formatter = new SimpleDateFormat("yyyy-MM-dd");
    }
    else
    {
      m_formatter = new SimpleDateFormat("MM/dd/yyyy");
    }

    String hostAddress = "http://www.edreams." + addressRoot;
    String address = hostAddress + "/flights/search/multidestinations/?tripT=MULTI_SEGMENT&isIframe=undefined"/* +
        "&leg1=" + URLEncoder.encode("from:" + from1 + ",frCode:undefined,to:" + to1 + ",toCode:undefined,departure:" + m_formatter.format(date1) + "TANYT", "UTF-8") +
        "&leg2=" + URLEncoder.encode("from:" + from2 + ",frCode:undefined,to:" + to2 + ",toCode:undefined,departure:" + m_formatter.format(date2) + "TANYT", "UTF-8")*/;
    //if (from3 != null && to3 != null && date3 != null)
    //{
    //  address += "&leg3=" + URLEncoder.encode("from:" + from3 + ",frCode:undefined,to:" + to3 + ",toCode:undefined,departure:" + m_formatter.format(date3) + "TANYT", "UTF-8");
    //}
    //address += "&passengers=" + URLEncoder.encode("children:0,adults:" + numOfPersons + ",seniors:0,infantinlap:Y", "UTF-8") +
    //    "&options=" + URLEncoder.encode("cabinclass:economy,nopenalty:N,sortby:price", "UTF-8") +
    //    "&mode=search";

    URL url = new URL(address);
    HttpURLConnection connection = createHttpConnection(url);
    InputStream ins = connection.getInputStream();
    String encoding = connection.getHeaderField("Content-Encoding");
    if (encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }

    //String response = Helper.readResponse(ins, getCharSetFromConnection(connection));
    Map<String, List<String>> headerFields = connection.getHeaderFields();
    url = new URL(address);
    connection = createHttpConnection(url);
    StringBuilder sbuf = new StringBuilder();
    for (String cookie : headerFields.get("Set-Cookie"))
    {
      if (sbuf.length() > 0)
      {
        sbuf.append("; ");
      }
      sbuf.append(cookie);
    }
    connection.addRequestProperty("Cookie", sbuf.toString());
    connection.addRequestProperty("Host", "www.edreams." + addressRoot);
    connection.addRequestProperty("Origin", "http://www.edreams." + addressRoot);
    connection.addRequestProperty("Referer", address);
    connection.setDoOutput(true);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestMethod("POST");
    Map<String, String> data = new HashMap<String, String>();
    data.put("buyPath", "36");
    data.put("auxOrBt", "");
    data.put("searchMainProductTypeName", "FLIGHT");
    data.put("tripTypeName", "MULTI_SEGMENT");
    data.put("departureLocationGeoNodeId0", "");
    data.put("departureLocation0", from1);
    data.put("arrivalLocationGeoNodeId0", "");
    data.put("arrivalLocation0", to1);
    data.put("departureDate0", m_formatter.format(date1));
    data.put("departureLocationGeoNodeId1", "");
    data.put("departureLocation1", from2);
    data.put("arrivalLocationGeoNodeId1", "");
    data.put("arrivalLocation1", to2);
    data.put("departureDate1", m_formatter.format(date2));
    data.put("departureLocationGeoNodeId2", "");
    data.put("departureLocation2", from3 == null ? "" : from3);
    data.put("arrivalLocationGeoNodeId2", "");
    data.put("arrivalLocation2", to3 == null ? "" : to3);
    data.put("departureDate2", date3 == null ? "" : m_formatter.format(date3));
    data.put("numAdults", String.valueOf(numOfPersons));
    data.put("numChilds", "0");
    data.put("numInfants", "0");
    data.put("cabinClassName", "TOURIST");
    data.put("filteringCarrier", "");
    data.put("fake_filteringCarrier", "All Airlines");
    data.put("collectionTypeEstimationNeeded", "false");
    data.put("applyAllTaxes", "false");

    DataOutputStream out = new DataOutputStream(connection.getOutputStream());

    Set keys = data.keySet();
    Iterator keyIter = keys.iterator();
    String content = "";
    for (int i = 0; keyIter.hasNext(); i++)
    {
      Object key = keyIter.next();
      if (i != 0)
      {
        content += "&";
      }
      content += key + "=" + URLEncoder.encode(data.get(key), "UTF-8");
    }
    out.writeBytes(content);
    out.flush();
    out.close();

    ins = connection.getInputStream();
    encoding = connection.getHeaderField("Content-Encoding");
    if (encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }

    String response = Helper.readResponse(ins, getCharSetFromConnection(connection));
    int streamingStartLocation = response.indexOf("Flight-SearchResults");
    //int streamingEndLocation = response.indexOf("';", streamingStartLocation);
    //try
    //{
    //  address = hostAddress + response.substring(streamingStartLocation, streamingEndLocation);
    //}
    //catch (Exception e)
    //{
    //  LOG.error("ExpediaFlightObtainer: Flight-SearchResults not found!", e);
    //  return;
    //}
    //url = new URL(address);
    //StringBuilder sbuf = new StringBuilder();
    //
    //for (String cookie : connection.getHeaderFields().get("Set-Cookie"))
    //{
    //  if (sbuf.length() > 0)
    //  {
    //    sbuf.append(";");
    //  }
    //  sbuf.append(cookie);
    //}
    //String cookies = sbuf.toString();
    //
    //connection = createHttpConnection(url);
    //connection.addRequestProperty("Cookie", cookies);
    //ins = connection.getInputStream();
    //encoding = connection.getHeaderField("Content-Encoding");
    //if (encoding.equals("gzip"))
    //{
    //  ins = new GZIPInputStream(ins);
    //}
    //response = Helper.readResponse(ins, getCharSetFromConnection(connection));
    //String startJsonString = "<script id='jsonData' type=\"text/x-jquery-tmpl\">";
    //int startIndex = response.indexOf(startJsonString);
    //if (startIndex == -1)
    //{
    //  LOG.error("ExpediaFlightObtainer: JSON decoding failed for www.expedia." + addressRoot);
    //  statusLabel.setForeground(Color.RED);
    //  return;
    //}
    //streamingStartLocation = startIndex + startJsonString.length();
    //streamingEndLocation = response.indexOf("</script>", streamingStartLocation);
    //try
    //{
    //  String jsonString = response.substring(streamingStartLocation, streamingEndLocation);
    //  jsonString = jsonString.substring(jsonString.indexOf('['), jsonString.length());
    //  SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    //  SimpleDateFormat localFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    //
    //  Object obj = JSONValue.parse(jsonString);
    //  JSONArray array = (JSONArray)obj;
    //  List<MultiCityFlightData> returnList = new ArrayList<MultiCityFlightData>();
    //  for (Object anArray : array)
    //  {
    //    JSONObject flight = (JSONObject)anArray;
    //    String localeTotalPrice = (String)flight.get("localeTotalPrice");
    //    JSONArray legs = (JSONArray)flight.get("legs");
    //    LinkedList<FlightLeg> legList = new LinkedList<FlightLeg>();
    //    for (Object leg1 : legs)
    //    {
    //      JSONObject leg = (JSONObject)leg1;
    //      JSONObject departureAirport = (JSONObject)leg.get("departureAirport");
    //      String airportFromCode = (String)departureAirport.get("airportCode");
    //      String airportFromName = (String)departureAirport.get("airportName");
    //      Date departureTime = formatter.parse((String)leg.get("departureTime"));
    //      JSONObject arrivalAirport = (JSONObject)leg.get("arrivalAirport");
    //      String airportToCode = (String)arrivalAirport.get("airportCode");
    //      String airportToName = (String)arrivalAirport.get("airportName");
    //      Date arrivalTime = formatter.parse((String)leg.get("arrivalTime"));
    //      String duration = getDuration((JSONObject)leg.get("totalTravelingHours"));
    //      String layover = getLayover((JSONArray)leg.get("segments"));
    //      legList.add(new FlightLeg(
    //          airportFromName,
    //          airportFromCode,
    //          localFormatter.format(departureTime),
    //          airportToName,
    //          airportToCode,
    //          localFormatter.format(arrivalTime),
    //          duration,
    //          layover
    //      ));
    //    }
    //
    //    if (legList.size() > 1)
    //    {
    //      String priceNumber = localeTotalPrice.replaceAll("[\\D]", "");
    //      MultiCityFlightData flightData = new MultiCityFlightData(
    //          0,
    //          (String)flight.get("airFareBasisCode"),
    //          Integer.parseInt(priceNumber),
    //          PriceType.getInstance(localeTotalPrice),
    //          legList,
    //          "Tickets left: " + flight.get("noOfTicketsLeft"),
    //          address
    //      );
    //      returnList.add(flightData);
    //      statusLabel.setForeground(statusLabel.getForeground().equals(Color.BLACK) ? Color.DARK_GRAY : Color.BLACK);
    //    }
    //  }
    //
    //  synchronized (flightGui.getFlightQueue())
    //  {
    //    for (MultiCityFlightData flightData : returnList)
    //    {
    //      if (!flightGui.getFlightQueue().contains(flightData))
    //      {
    //        flightGui.getFlightQueue().add(flightData);
    //
    //        if (flightGui.getFlightQueue().size() > FlightsGui.QUEUE_SIZE)
    //        {
    //          flightGui.getFlightQueue().remove();
    //        }
    //      }
    //    }
    //
    //    if (returnList.size() > 0)
    //    {
    //      MultiCityFlightTableModel tableModel = (MultiCityFlightTableModel)flightGui.getMainTable().getModel();
    //      tableModel.setEntityList(new ArrayList<MultiCityFlightData>(flightGui.getFlightQueue()));
    //      tableModel.fireTableDataChanged();
    //    }
    //  }
    //}
    //catch (Exception e)
    //{
    //  LOG.error("ExpediaFlightObtainer: JSON decoding failed!", e);
    //  statusLabel.setForeground(Color.RED);
    //}
  }

  private String getLayover(JSONArray segments)
  {
    StringBuilder builder = new StringBuilder();
    long days = 0, hours = 0, minutes = 0;
    if (segments.size() < 2)
    {
      return "";
    }
    else
    {
      for (int i = 0; i < segments.size() - 1; i++)
      {
        if (builder.length() > 0)
        {
          builder.append(",");
        }
        JSONObject segment = (JSONObject)segments.get(i);
        JSONObject layover = (JSONObject)segment.get("layover");
        if (layover != null)
        {
          days += (Long)layover.get("numOfDays");
          hours += (Long)layover.get("hours");
          minutes += (Long)layover.get("minutes");
        }
        JSONObject arrivalAirport = (JSONObject)segment.get("arrivalAirport");
        builder.append((String)arrivalAirport.get("airportCode"));
      }
      builder.append(" (");
      if (days > 0)
      {
        builder.append(days).append("days ");
      }
      builder.append(hours).append("h ").append(minutes).append("min)");
      return builder.toString();
    }
  }

  private String getDuration(JSONObject totalTravelingHours)
  {
    Long numOfDays = (Long)totalTravelingHours.get("numOfDays");
    Long hours = (Long)totalTravelingHours.get("hours");
    Long minutes = (Long)totalTravelingHours.get("minutes");
    if (0 == numOfDays)
    {
      if (0 == hours)
      {
        return minutes + "min";
      }
      else
      {
        return hours + "h " + minutes + "min";
      }
    }
    else
    {
      return numOfDays + "days " + hours + "h " + minutes + "min";
    }
  }

  private String getCharSetFromConnection(URLConnection connection)
  {
    String contentType = connection.getHeaderField("Content-Type");

    String[] values = contentType.split(";");
    String charset = null;

    for (String value : values)
    {
      value = value.trim();

      if (value.toLowerCase().startsWith("charset="))
      {
        charset = value.substring("charset=".length());
      }
    }

    if (charset == null)
    {
      charset = "UTF-8";
    }
    return charset;
  }

  private HttpURLConnection createHttpConnection(URL url) throws IOException
  {
    URLConnection connection = url.openConnection();
    connection.addRequestProperty("Connection", "keep-alive");
        connection.addRequestProperty("Cache-Control", "max-age=0");
    connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36");
    connection.addRequestProperty("Host", url.getHost());
    connection.addRequestProperty("Referer", url.toString());
    connection.addRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
    connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
    return (HttpURLConnection)connection;
  }

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new EdreamsFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      obtainer.search(null, null, "com", "LJU", "NYC", formatter.parse("18.12.2013"), "NYC", "VIE", formatter.parse("07.01.2014"), "NYC", "VIE", formatter.parse("07.01.2014"), 1);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
