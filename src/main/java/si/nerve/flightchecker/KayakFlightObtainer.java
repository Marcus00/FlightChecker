package si.nerve.flightchecker;

import java.io.ByteArrayOutputStream;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import si.nerve.flightchecker.data.FlightLeg;
import si.nerve.flightchecker.data.MultiCityFlightData;
import si.nerve.flightchecker.data.PriceType;

/**
 * Created: 10.8.13 20:39
 */
public class KayakFlightObtainer implements MultiCityFlightObtainer
{
  private static final String ADDRESS_ROOT = "http://www.kayak.com/flights";
  private static final int ADDRESS_PORT = 80;
  private static final int MAX_RETRIES = 13;

  @Override
  public Set<MultiCityFlightData> get(String from1, String to1, Date date1, String from2, String to2, Date date2) throws IOException
  {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    String address = ADDRESS_ROOT + "/" + from1 + "-" + to1 + "/" + formatter.format(date1) +
        "/" + from2 + "-" + to2 + "/" + formatter.format(date2);
    URL url = new URL(address);
    URLConnection connection = createHttpConnection(url);
    InputStream ins = connection.getInputStream();
    String encoding = connection.getHeaderField("Content-Encoding");
    if (encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }
    String response = readResponse(ins);

    int streamingStartLocation = response.indexOf("window.Streaming");
    int streamingEndLocation = response.indexOf(";", streamingStartLocation);
    String streamingCommand = response.substring(streamingStartLocation, streamingEndLocation);
    int timeStart = streamingCommand.indexOf(",") + 1;
    int timeEnd = streamingCommand.indexOf(",", timeStart) - 1;
    long time = Long.parseLong(streamingCommand.substring(timeStart, timeEnd).trim());

    int searchIdCmdStartLocation = response.indexOf("setTargeting('searchid'");
    int searchIdCmdEndLocation = response.indexOf(")", searchIdCmdStartLocation);
    String searchLocationCommand = response.substring(searchIdCmdStartLocation, searchIdCmdEndLocation);
    int searchIdStartLocation = searchLocationCommand.indexOf(",") + 3;
    int searchIdEndLocation = searchLocationCommand.indexOf("'", searchIdStartLocation);
    String searchId = searchLocationCommand.substring(searchIdStartLocation, searchIdEndLocation);

    Set<MultiCityFlightData> returnSet = new HashSet<MultiCityFlightData>();
    for (int i = 1; i <= MAX_RETRIES; i++)
    {
      try
      {
        Thread.currentThread().sleep(1000);
      }
      catch (InterruptedException e)
      {
        e.printStackTrace();
      }

      KayakResult result = fetchResult(i, time, i == MAX_RETRIES, address, searchId, connection.getHeaderFields());
      time = result.getTime();
      response = result.getResponse();
      if (response.length() > 100000)
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
              returnSet.add(new MultiCityFlightData(
                  Integer.parseInt(link.attr("data-index")),
                  link.attr("data-resultid"),
                  Integer.parseInt(price.substring(1)),
                  PriceType.getInstance(price.charAt(0)),
                  legs,
                  link.select("div.seatsPromo").text()
              ));
            }
          }
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }

    return returnSet;
  }

  private String readString(String input, String mainMarker, String markerStart, String markerEnd)
  {
    int mainMarkerIndex = input.indexOf(mainMarker);
    int resultStart = input.indexOf(markerStart, mainMarkerIndex) + markerStart.length() - 1;
    int resultEnd = input.indexOf(markerEnd, resultStart);
    return input.substring(resultStart, resultEnd);
  }

  private KayakResult fetchResult(
      int counter,
      long time,
      boolean finalCall,
      String referrer,
      String searchId,
      Map<String, List<String>> headerFields)
      throws IOException
  {
    String address = "http://www.kayak.com/s/jsresults?ss=1&poll=" + counter + "&final=" + finalCall + "&updateStamp=" + time;
    URL url = new URL(address);
    HttpURLConnection connection = createHttpConnection(url);
    StringBuffer sbuf = new StringBuffer();
    for (String cookie : headerFields.get("Set-Cookie"))
    {
      if (sbuf.length() > 0)
      {
        sbuf.append("; ");
      }
      sbuf.append(cookie);
    }
    connection.addRequestProperty("Cookie", sbuf.toString());
    connection.addRequestProperty("Host", "www.kayak.com");
    connection.addRequestProperty("Origin", "http://www.kayak.com");
    connection.addRequestProperty("Referer", referrer);
    connection.setDoOutput(true);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestMethod("POST");
    Map<String, String> data = new HashMap<String, String>();
    data.put("lmname", "");
    data.put("lmname2", "");
    data.put("c", "15");
    data.put("s", "price");
    data.put("searchid", searchId);
    data.put("idt", "");
    data.put("poll", Integer.toString(counter));
    data.put("vw", "list");
    data.put("urlViewState", "");
    data.put("streaming", "true");

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

    InputStream ins = connection.getInputStream();
    String encoding = connection.getHeaderField("Content-Encoding");
    if (encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }
    String response = readResponse(ins);

    int newTimeCommandStart = response.indexOf("Streaming.lastPoll=");
    newTimeCommandStart = response.indexOf("=", newTimeCommandStart) + 1;
    int newTimeCommandStop = response.indexOf(";", newTimeCommandStart) - 1;
    long newTime = 0;
    try
    {
      newTime =    Long.parseLong(response.substring(newTimeCommandStart, newTimeCommandStop).trim());
    }
    catch (NumberFormatException e)
    {
      e.printStackTrace();
    }

    return new KayakResult(newTime, response);
  }

  private HttpURLConnection createHttpConnection(URL url) throws IOException
  {
    URLConnection connection = url.openConnection();
    connection.addRequestProperty("Connection", "keep-alive");
    connection.addRequestProperty("Cache-Control", "max-age");
    connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36");
    connection.addRequestProperty("Referer", "http://www.kayak.com/flights/LJU-NYC/2013-08-11/NYC-VIE/2013-08-17");
    connection.addRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
    connection.addRequestProperty("Accept-Language", "sl-SI,sl;q=0.8,en-GB;q=0.6,en;q=0.4");
    return (HttpURLConnection)connection;
  }

  private String readResponse(InputStream ins) throws IOException
  {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int cnt = 0;
    while ((cnt = ins.read(buffer)) >= 0)
    {
      if (cnt > 0)
      {
        bos.write(buffer, 0, cnt);
      }
    }
    return bos.toString();
  }

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new KayakFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      Set<MultiCityFlightData> multiCityFlightData = obtainer.get("LJU", "NYC", formatter.parse("18.8.2013"), "NYC", "VIE", formatter.parse("25.8.2013"));
      System.out.println();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
