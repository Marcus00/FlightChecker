package si.nerve.flightchecker;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import si.nerve.flightchecker.data.MultiCityFlightData;
import sun.net.www.http.HttpClient;

/**
 * Created: 10.8.13 20:39
 */
public class CleartripFlightObtainer implements MultiCityFlightObtainer
{

  @Override
  public Set<MultiCityFlightData> get(String addressRoot, String from1, String to1, Date date1, String from2, String to2, Date date2) throws IOException
  {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    String address = "http://api.staging.cleartrip.com/air/1.0/search?from=" + from1 + "&to=" + to1 + "&depart-date=" + formatter.format(date1) +
        "&return-date=" + formatter.format(date2);
    URL url = new URL(address);
    HttpURLConnection conn = createHttpConnection(url);

    if (conn.getResponseCode() != 200)
    {
      throw new IOException(conn.getResponseMessage());
    }

    // Buffer the result into a string
    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = rd.readLine()) != null)
    {
      sb.append(line);
    }
    rd.close();

    conn.disconnect();
    //InputStream errorStream = connection.getErrorStream();
    //InputStream ins = connection.getInputStream();
    //String encoding = connection.getHeaderField("Content-Encoding");
    //if (encoding.equals("gzip"))
    //{
    //  ins = new GZIPInputStream(ins);
    //}
    //String response = readResponse(ins);

    Set<MultiCityFlightData> returnSet = new HashSet<MultiCityFlightData>();

    //if (response.contains("content_div"))
    //{
    //  try
    //  {
    //    Document doc = Jsoup.parse(response);
    //    Elements links = doc.select("#content_div > div[class^=flightresult]");
    //    for (Element link : links)
    //    {
    //      LinkedList<FlightLeg> legs = new LinkedList<FlightLeg>();
    //      for (Element leg : link.select("div[class^=singleleg singleleg]"))
    //      {
    //        Elements airports = leg.select("div.airport");
    //        if (airports.size() == 2)
    //        {
    //          legs.add(new FlightLeg(
    //              airports.get(0).attr("title"),
    //              airports.get(0).text(),
    //              leg.select("div[class^=flighttime]").get(0).text(),
    //              airports.get(1).attr("title"),
    //              airports.get(1).text(),
    //              leg.select("div[class^=flighttime]").get(1).text(),
    //              leg.select("div.duration").text(),
    //              leg.select("div.stopsLayovers > span.airportslist").text()
    //          ));
    //        }
    //        else
    //        {
    //          System.out.println();
    //        }
    //      }
    //
    //      if (legs.size() > 1)
    //      {
    //        String price = link.select("a[class^=results_price]").text();
    //        returnSet.add(new MultiCityFlightData(
    //            Integer.parseInt(link.attr("data-index")),
    //            link.attr("data-resultid"),
    //            Integer.parseInt(price.replaceAll("[\\D]", "")),
    //            price.contains("€") ? PriceType.EURO : PriceType.DOLLAR,
    //            legs,
    //            link.select("div.seatsPromo").text()
    //        ));
    //      }
    //    }
    //  }
    //  catch (Exception e)
    //  {
    //    e.printStackTrace();
    //  }
    //}

    return returnSet;
  }

  private String readString(String input, String mainMarker, String markerStart, String markerEnd)
  {
    int mainMarkerIndex = input.indexOf(mainMarker);
    int resultStart = input.indexOf(markerStart, mainMarkerIndex) + markerStart.length() - 1;
    int resultEnd = input.indexOf(markerEnd, resultStart);
    return input.substring(resultStart, resultEnd);
  }

  private HttpURLConnection createHttpConnection(URL url) throws IOException
  {
    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    //connection.setRequestMethod("GET");
    //connection.addRequestProperty("X-CT-API-KEY", "2b716e6a6188795193ed97a590cff33c");
      //Send request

    connection.addRequestProperty("Connection", "keep-alive");
    //connection.addRequestProperty("Cache-Control", "max-age");
    //connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    //connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36");
    //connection.addRequestProperty("Referer", url.toString());

    //connection.addRequestProperty("Accept-Language", "sl-SI,sl;q=0.8,en-GB;q=0.6,en;q=0.4");
    return connection;
  }

  private String readResponse(InputStream ins) throws IOException
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
    return bos.toString();
  }

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new CleartripFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      Set<MultiCityFlightData> multiCityFlightData = obtainer.get("de", "LJU", "NYC", formatter.parse("18.9.2013"), "NYC", "VIE", formatter.parse("25.9.2013"));
      System.out.println();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
