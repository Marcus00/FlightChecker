package si.nerve.flightchecker.pages.obtainers;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import si.nerve.flightchecker.FlightsGui;
import si.nerve.flightchecker.components.MultiCityFlightTableModel;
import si.nerve.flightchecker.data.FlightLeg;
import si.nerve.flightchecker.data.MultiCityFlightData;
import si.nerve.flightchecker.data.PriceType;
import si.nerve.flightchecker.helper.Helper;
import si.nerve.flightchecker.pages.KayakResult;
import si.nerve.flightchecker.pages.MultiCityFlightObtainer;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created: 10.8.13 20:39
 */
public class KayakFlightObtainer implements MultiCityFlightObtainer
{
  private String m_addressDot, m_address;
  private SimpleDateFormat m_formatter;
  private static final int MAX_RETRIES = 45;
  public static final int TIME_MILLIS = 30000;
  private static final Logger LOG = Logger.getLogger(KayakFlightObtainer.class);

  @Override
  public void search(final FlightsGui flightGui, JLabel statusLabel, String addressRoot, String from1, String to1, Date date1,
      String from2, String to2, Date date2, String from3, String to3, Date date3, Integer numOfPersons, boolean changeProxy)
      throws Exception
  {
    m_formatter = new SimpleDateFormat("yyyy-MM-dd");

    m_addressDot = addressRoot;
    String hostAddress = "http://www.kayak." + m_addressDot;
    m_address = hostAddress + "/flights/" + from1 + "-" + to1 + "/" + m_formatter.format(date1) +
        "/" + from2 + "-" + to2 + "/" + m_formatter.format(date2);
    if (from3 != null && to3 != null && date3 != null)
    {
      m_address += "/" + from3 + "-" + to3 + "/" + m_formatter.format(date3);
    }
    if (numOfPersons > 1)
    {
      m_address += "/" + numOfPersons + "adults";
    }
    URL url = new URL(m_address);
    URLConnection connection = createHttpConnection(url);
    InputStream ins = connection.getInputStream();
    String encoding = connection.getHeaderField("Content-Encoding");
    if (encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }
    String response = Helper.readResponse(ins, getCharSetFromConnection(connection));

    int streamingStartLocation = response.indexOf("window.Streaming");
    int streamingEndLocation = response.indexOf(";", streamingStartLocation);
    String streamingCommand;
    try
    {
      streamingCommand = response.substring(streamingStartLocation, streamingEndLocation);
    }
    catch (Exception e)
    {
      LOG.error("KayakFlightObtainer: window.Streaming not found!", e);
      return;
    }
    int timeStart = streamingCommand.indexOf(",") + 1;
    int timeEnd = streamingCommand.indexOf(",", timeStart) - 1;
    long time = Long.parseLong(streamingCommand.substring(timeStart, timeEnd).trim());

    int searchIdCmdStartLocation = response.indexOf("setTargeting('searchid'");
    int searchIdCmdEndLocation = response.indexOf(")", searchIdCmdStartLocation);
    String searchLocationCommand = response.substring(searchIdCmdStartLocation, searchIdCmdEndLocation);
    int searchIdStartLocation = searchLocationCommand.indexOf(",") + 3;
    int searchIdEndLocation = searchLocationCommand.indexOf("'", searchIdStartLocation);
    String searchId = searchLocationCommand.substring(searchIdStartLocation, searchIdEndLocation);

    long startTime = System.currentTimeMillis();
    //long middleTime;
    int i = 1;

    while ((System.currentTimeMillis() - startTime < TIME_MILLIS) && i <= MAX_RETRIES)
    {
      KayakResult result = fetchResult(i, time, i++ == MAX_RETRIES, m_address, searchId, connection.getHeaderFields());
      if (result != null)
      {
        time = result.getTime();
        response = result.getResponse();
        addToQueue(flightGui, statusLabel, response);
      }
      else
      {
        return;
      }
    }
  }

  private void addToQueue(FlightsGui flightGui, JLabel kayakStatusLabel, String response)
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
                m_address
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
                  MultiCityFlightTableModel tableModel = (MultiCityFlightTableModel) flightGui.getMainTable().getModel();
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
        LOG.error("KayakFlightObtainer: Jsoup parsing failed!", e);
      }
    }
    else
    {
      LOG.error("KayakFlightObtainer: content_div not found! Instead:\n" + response);
    }
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
    String address = "http://www.kayak." + m_addressDot + "/s/jsresults?ss=1&poll=" + counter + "&final=" + finalCall + "&updateStamp=" + time;
    URL url = new URL(address);
    HttpURLConnection connection = createHttpConnection(url);
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
    connection.addRequestProperty("Host", "www.kayak." + m_addressDot);
    connection.addRequestProperty("Origin", "http://www.kayak." + m_addressDot);
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

    String response = Helper.readResponse(ins, getCharSetFromConnection(connection));

    int newTimeCommandStart = response.indexOf("Streaming.lastPoll=");
    if (newTimeCommandStart < 0)
    {
      LOG.error("KayakFlightObtainer: Streaming.lastPoll= not found in response!");
      return null;
    }
    newTimeCommandStart = response.indexOf("=", newTimeCommandStart) + 1;
    int newTimeCommandStop = response.indexOf(";", newTimeCommandStart) - 1;
    long newTime;
    try
    {
      newTime = Long.parseLong(response.substring(newTimeCommandStart, newTimeCommandStop).trim());
    }
    catch (NumberFormatException e)
    {
      LOG.error("KayakFlightObtainer: Next call time parse failed", e);
      return null;
    }

    return new KayakResult(newTime, response);
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
    connection.addRequestProperty("Cache-Control", "max-age");
    connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36");
    connection.addRequestProperty("Referer", url.toString());
    connection.addRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
    connection.addRequestProperty("Accept-Language", "sl-SI,sl;q=0.8,en-GB;q=0.6,en;q=0.4");
    return (HttpURLConnection) connection;
  }

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new KayakFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      obtainer.search(null, null, "com", "vce", "bkk", formatter.parse("20.12.2013"), "bkk", "vce", formatter.parse("07.01.2014"), "bkk", "vce", formatter.parse("07.01.2014"), 1, false);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
