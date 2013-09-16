package si.nerve.flightchecker.pages;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;
import javax.swing.JLabel;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import si.nerve.flightchecker.FlightsGui;
import si.nerve.flightchecker.components.MultiCityFlightTableModel;
import si.nerve.flightchecker.data.FlightLeg;
import si.nerve.flightchecker.data.MultiCityFlightData;
import si.nerve.flightchecker.data.PriceType;

/**
 * Created: 10.8.13 20:39
 */
public class ExpediaFlightObtainer implements MultiCityFlightObtainer
{
  private SimpleDateFormat m_formatter = new SimpleDateFormat("MM/dd/yyyy");

  @Override
  public void search(final FlightsGui flightGui, JLabel kayakStatusLabel, String addressRoot, String from1, String to1, Date date1, String from2, String to2, Date date2)
      throws Exception
  {
    String hostAddress = "http://www.expedia.com/";
    String address = hostAddress + "Flight-SearchResults?trip=multi&leg1=from:" + from1 + ",frCode:,to:" + to1 + ",toCode:,departure:" + m_formatter.format(date1)
        + "TANYT&leg2=from:" + from2 + "frCode:,to:" + to2 + ",toCode:,departure:" + m_formatter.format(date2)
        + "TANYT&passengers=children:0,adults:1,seniors:0,infantinlap:Y&options=cabinclass:economy,nopenalty:N,sortby:price&mode=search";

    URL url = new URL(address);
    URLConnection connection = createHttpConnection(url);
    InputStream ins = connection.getInputStream();
    String encoding = connection.getHeaderField("Content-Encoding");
    if (encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }
    String response = readResponse(ins, getCharSetFromConnection(connection));
    int streamingStartLocation = response.indexOf("Flight-SearchResults");
    int streamingEndLocation = response.indexOf("';", streamingStartLocation);
    try
    {
      address = hostAddress + response.substring(streamingStartLocation, streamingEndLocation);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.out.println("BANNED! " + address);
      return;
    }
    url = new URL(address);
    StringBuilder sbuf = new StringBuilder();
    for (String cookie : connection.getHeaderFields().get("Set-Cookie"))
    {
      if (sbuf.length() > 0)
      {
        sbuf.append("; ");
      }
      sbuf.append(cookie);
    }
    connection = createHttpConnection(url);
    connection.addRequestProperty("Cookie", sbuf.toString());
    ins = connection.getInputStream();
    encoding = connection.getHeaderField("Content-Encoding");
    if (encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }
    response = readResponse(ins, getCharSetFromConnection(connection));
    String startJsonString = "<script id='jsonData' type=\"text/x-jquery-tmpl\">";
    streamingStartLocation = response.indexOf(startJsonString) + startJsonString.length();
    streamingEndLocation = response.indexOf("</script>", streamingStartLocation);
    try
    {
      String jsonString = response.substring(streamingStartLocation, streamingEndLocation);
      jsonString = jsonString.substring(jsonString.indexOf('['), jsonString.length());

      SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/ss HH:mm:ss");
      SimpleDateFormat localFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

      Object obj = JSONValue.parse(jsonString);
      JSONArray array = (JSONArray)obj;
      for (int i = 0; i < array.size(); i++)
      {
        JSONObject flight = (JSONObject)array.get(i);
        String localeTotalPrice = (String)flight.get("localeTotalPrice");

        JSONArray legs = (JSONArray)flight.get("legs");
        LinkedList<FlightLeg> legList = new LinkedList<FlightLeg>();
        for (int j = 0; j < legs.size(); j++)
        {
          JSONObject leg = (JSONObject)legs.get(j);
          JSONObject departureAirport = (JSONObject)leg.get("departureAirport");
          String airportFromCode = (String)departureAirport.get("airportCode");
          String airportFromName = (String)departureAirport.get("airportName");
          Date departureTime = formatter.parse((String)leg.get("departureTime"));
          JSONObject arrivalAirport = (JSONObject)leg.get("arrivalAirport");
          String airportToCode = (String)arrivalAirport.get("airportCode");
          String airportToName = (String)arrivalAirport.get("airportName");
          Date arrivalTime = formatter.parse((String)leg.get("arrivalTime"));
          String duration = getDuration((JSONObject)leg.get("totalTravelingHours"));
          String layover = getLayover((JSONArray)leg.get("segments"));
          legList.add(new FlightLeg(
              airportFromName,
              airportFromCode,
              localFormatter.format(departureTime),
              airportToName,
              airportToCode,
              localFormatter.format(arrivalTime),
              duration,
              layover
          ));
        }

        if (legList.size() > 1)
        {
          String priceNumber = localeTotalPrice.replaceAll("[\\D]", "");
          MultiCityFlightData flightData = new MultiCityFlightData(
              0,
              (String)flight.get("airFareBasisCode"),
              Integer.parseInt(priceNumber),
              PriceType.getInstance(localeTotalPrice),
              legList,
              "Tickets left: " + flight.get("noOfTicketsLeft"),
              hostAddress
          );

          kayakStatusLabel.setForeground(kayakStatusLabel.getForeground().equals(Color.BLACK) ? Color.DARK_GRAY : Color.BLACK);

          synchronized (flightGui.getFlightQueue())
          {
            if (!flightGui.getFlightQueue().contains(flightData))
            {
              flightGui.getFlightQueue().add(flightData);

              MultiCityFlightData removed = null;
              if (flightGui.getFlightQueue().size() > FlightsGui.QUEUE_SIZE)
              {
                removed = flightGui.getFlightQueue().remove();
              }

              if (!flightData.equals(removed))
              {
                MultiCityFlightTableModel tableModel = (MultiCityFlightTableModel)flightGui.getMainTable().getModel();
                tableModel.setEntityList(new ArrayList<MultiCityFlightData>(flightGui.getFlightQueue()));
                tableModel.fireTableDataChanged();
              }
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      //System.out.println("BANNED! " + address);
      return;
    }
    System.out.println();
  }

  private String getLayover(JSONArray segments)
  {
    StringBuilder builder = new StringBuilder();
    long days = 0, hours = 0, minutes = 0;
    if (segments.size() < 2)
    {
      return null;
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
      builder.append(" (").append(days).append("days ").append(hours).append("h ").append(minutes).append("min)");
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

  private void addToQueue(FlightsGui flightGui, JLabel kayakStatusLabel, String hostAddress, String response)
  {
    if (response.contains("content_div"))
    {
      try
      {
        Document doc = Jsoup.parse(response);
        Elements links = doc.select("#content_div > div[class^=flightresult]");
        for (Element link : links)
        {
          LinkedList<FlightLeg> legs = new LinkedList<FlightLeg>();
          for (Element leg : link.select("div[class^=singleleg singleleg]"))
          {
            Elements airports = leg.select("div.airport");
            if (airports.size() == 2)
            {
              legs.add(new FlightLeg(
                  airports.get(0).attr("title"),
                  airports.get(0).text(),
                  leg.select("div[class^=flighttime]").get(0).text(),
                  airports.get(1).attr("title"),
                  airports.get(1).text(),
                  leg.select("div[class^=flighttime]").get(1).text(),
                  leg.select("div.duration").text(),
                  leg.select("div.stopsLayovers > span.airportslist").text()
              ));
            }
          }

          if (legs.size() > 1)
          {
            String price = link.select("a[class^=results_price]").text();
            String priceNumber = price.replaceAll("[\\D]", "");
            MultiCityFlightData flightData = new MultiCityFlightData(
                Integer.parseInt(link.attr("data-index")),
                link.attr("data-resultid"),
                Integer.parseInt(priceNumber),
                PriceType.getInstance(price),
                legs,
                link.select("div.seatsPromo").text(),
                hostAddress
            );

            kayakStatusLabel.setForeground(kayakStatusLabel.getForeground().equals(Color.BLACK) ? Color.DARK_GRAY : Color.BLACK);

            synchronized (flightGui.getFlightQueue())
            {
              if (!flightGui.getFlightQueue().contains(flightData))
              {
                flightGui.getFlightQueue().add(flightData);

                MultiCityFlightData removed = null;
                if (flightGui.getFlightQueue().size() > FlightsGui.QUEUE_SIZE)
                {
                  removed = flightGui.getFlightQueue().remove();
                }

                if (!flightData.equals(removed))
                {
                  MultiCityFlightTableModel tableModel = (MultiCityFlightTableModel)flightGui.getMainTable().getModel();
                  tableModel.setEntityList(new ArrayList<MultiCityFlightData>(flightGui.getFlightQueue()));
                  tableModel.fireTableDataChanged();
                }
              }
            }
          }
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  private String getCharSetFromConnection(URLConnection connection)
  {
    String contentType = connection.getHeaderField("Content-Type");

    String[] values = contentType.split(";"); //The values.length must be equal to 2...
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
      charset = "UTF-8"; //Assumption....it's the mother of all f**k ups...lol
    }
    return charset;
  }

  private HttpURLConnection createHttpConnection(URL url) throws IOException
  {
    URLConnection connection = url.openConnection();
    connection.addRequestProperty("Connection", "keep-alive");
    connection.addRequestProperty("Cache-Control", "max-age");
    connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36");
    connection.addRequestProperty("Referer", url.toString());
    connection.addRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
    connection.addRequestProperty("Accept-Language", "sl-SI,sl;q=0.8,en-GB;q=0.6,en;q=0.4");
    return (HttpURLConnection)connection;
  }

  private String readResponse(InputStream ins, String charset) throws IOException
  {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int cnt;
    while ((cnt = ins.read(buffer)) >= 0)
    {
      if (cnt > 0)
      {
        bos.write(buffer, 0, cnt);
      }
    }
    return bos.toString(charset);
  }

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new ExpediaFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      obtainer.search(null, null, "com", "LJU", "NYC", formatter.parse("18.12.2013"), "NYC", "VIE", formatter.parse("07.01.2014"));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
