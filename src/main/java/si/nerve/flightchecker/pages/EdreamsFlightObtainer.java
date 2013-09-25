package si.nerve.flightchecker.pages;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import si.nerve.flightchecker.FlightsGui;
import si.nerve.flightchecker.components.MultiCityFlightTableModel;
import si.nerve.flightchecker.data.FlightLeg;
import si.nerve.flightchecker.data.MultiCityFlightData;
import si.nerve.flightchecker.data.PriceType;
import si.nerve.flightchecker.helper.Helper;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created: 10.8.13 20:39
 */
public class EdreamsFlightObtainer implements MultiCityFlightObtainer
{
  private SimpleDateFormat m_formatter;
  public static final String COOKIE_KEY = "Set-Cookie";
  public static final String INSTANCE_NAME = "[EdreamsFlightObtainer] ";
  private static final Logger LOG = Logger.getLogger(EdreamsFlightObtainer.class);
  @Override
  public void search(
      final FlightsGui flightGui, JLabel statusLabel, String addressRoot, String from1, String to1, Date date1,
      String from2, String to2, Date date2, String from3, String to3, Date date3, Integer numOfPersons, boolean changeProxy) throws Exception
  {
    try
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
      String address = hostAddress + "/flights/search/multidestinations/?tripT=MULTI_SEGMENT&isIframe=undefined";

      Proxy currentProxy = Helper.pullFreeProxy(changeProxy);
      HttpURLConnection connection = createHttpProxyConnection(address, currentProxy);

      Map<String, List<String>> headerFields = connection.getHeaderFields();
      String newAddress = hostAddress + "/engine/ItinerarySearch/search";
      connection = createHttpProxyConnection(newAddress, currentProxy);
      StringBuilder sbuf = new StringBuilder();

      if (headerFields != null && headerFields.containsKey(COOKIE_KEY))
      {
        for (String cookie : headerFields.get(COOKIE_KEY))
        {
          if (sbuf.length() > 0 && !sbuf.toString().trim().endsWith(";"))
          {
            sbuf.append("; ");
          }
          sbuf.append(cookie);
        }
      }
      connection.addRequestProperty("Cookie", sbuf.toString());
      connection.addRequestProperty("Host", "www.edreams." + addressRoot);
      connection.addRequestProperty("Origin", "http://www.edreams." + addressRoot);
      connection.addRequestProperty("Referer", address);
      connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      connection.setDoInput(true);
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

      String postString = getPostFormData(data);
      connection.addRequestProperty("Content-Length", String.valueOf(postString.length()));

      DataOutputStream out = new DataOutputStream(connection.getOutputStream());
      out.writeBytes(postString);
      out.flush();
      out.close();

      InputStream ins = null;
      try
      {
        ins = connection.getInputStream();
      }
      catch (IOException e)
      {
        if ("Unexpected end of file from server".equals(e.getLocalizedMessage()))
        {
          LOG.error(INSTANCE_NAME + "Proxy " + currentProxy.address().toString() + " error: Unexpected end of file from server. Changing proxy.");
          this.search(flightGui, statusLabel, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
          return;
        }
      }
      String encoding = connection.getHeaderField("Content-Encoding");
      if (encoding != null && encoding.equals("gzip"))
      {
        ins = new GZIPInputStream(ins);
      }

      String response = Helper.readResponse(ins, getCharSetFromConnection(connection));

      if (response.contains("singleItineray-content-body"))
      {
        for (int i = 2; i < 10; i++)
        {
          Document doc = Jsoup.parse(response);
          Elements links = doc.select("div[class^=singleItineray-content-body]");

          List<MultiCityFlightData> returnList = new ArrayList<MultiCityFlightData>();
          for (Element link : links)
          {
            String price = "", priceNumber = null;
            for (Node node : link.select("div[class^=singleItinerayPrice]").first().childNodes())
            {
              if (node instanceof TextNode && ((TextNode)node).text().trim().length() > 0)
              {
                price = ((TextNode)node).text().trim();
                priceNumber = price.replaceAll("[\\D]", "");
                if (priceNumber != null && priceNumber.length() > 0)
                {
                  break;
                }
              }
            }
            Elements legs = link.select("td.singleItineray-content-flights table[id^=tableItineraryContent]");
            LinkedList<FlightLeg> legList = new LinkedList<FlightLeg>();
            int j = 1;

            for (Element leg : legs)
            {
              String summary = leg.select("span[id^=segmentOrigin]").text();
              String[] split = summary.split("\\u00a0");
              String departureTime = null, fromAirportName = null;
              if (split.length > 2)
              {
                departureTime = split[1];
                fromAirportName = split[2];
              }
              else
              {
                LOG.error(INSTANCE_NAME + "departureTime or fromAirportName not found!");
              }

              summary = leg.select("span[id^=segmentDestination]").text();
              split = summary.split("\\u00a0");
              String arrivalTime = null, toAirportName = null;
              if (split.length > 2)
              {
                arrivalTime = split[1];
                toAirportName = split[2];
              }
              else
              {
                LOG.error(INSTANCE_NAME + "arrivalTime or toAirportName not found!");
              }

              summary = leg.select("span[id^=segmentElapsedTime]").text();
              split = summary.split("\\u00a0");
              String duration = null;
              if (split.length > 1)
              {
                duration = split[1].replace('\'', 'm');
              }
              else
              {
                LOG.error(INSTANCE_NAME + "duration not found!");
              }
              summary = leg.select("span[id^=segmentStopsOvers]").text();
              split = summary.split("\\u00a0");
              String layover = null;
              if (split.length > 1)
              {
                layover = split[0] + " layover";
              }
              else
              {
                LOG.error(INSTANCE_NAME + "layover not found!");
              }

              legList.add(new FlightLeg(
                  fromAirportName,
                  j == 1 ? from1 : j == 2 ? from2 : from3,
                  departureTime,
                  toAirportName,
                  j == 1 ? to1 : j == 2 ? to2 : to3,
                  arrivalTime,
                  duration,
                  layover
              ));
              j++;
            }

            if (legList.size() > 1)
            {
              String s = link.toString();
              int beginIndex = s.indexOf("searchId=") + "searchId=".length();
              MultiCityFlightData flightData = new MultiCityFlightData(
                  0,
                  s.substring(beginIndex, s.indexOf('&', beginIndex)),
                  Integer.parseInt(priceNumber),
                  PriceType.getInstance(price),
                  legList,
                  "",
                  address
              );
              returnList.add(flightData);
              statusLabel.setForeground(!statusLabel.getForeground().equals(Color.DARK_GRAY) ? Color.DARK_GRAY : Color.BLACK);
            }
          }

          synchronized (flightGui.getFlightQueue())
          {
            for (MultiCityFlightData flightData : returnList)
            {
              if (!flightGui.getFlightQueue().contains(flightData))
              {
                flightGui.getFlightQueue().add(flightData);

                if (flightGui.getFlightQueue().size() > FlightsGui.QUEUE_SIZE)
                {
                  flightGui.getFlightQueue().remove();
                }
              }
            }

            if (returnList.size() > 0)
            {
              MultiCityFlightTableModel tableModel = (MultiCityFlightTableModel)flightGui.getMainTable().getModel();
              tableModel.setEntityList(new ArrayList<MultiCityFlightData>(flightGui.getFlightQueue()));
              tableModel.fireTableDataChanged();
            }
          }

          newAddress = hostAddress + "/engine/ItinerarySearch/paging?page=" + i + "&isSEM=";
          connection = createHttpProxyConnection(newAddress, currentProxy);
          sbuf = new StringBuilder();
          if (headerFields != null && headerFields.containsKey(COOKIE_KEY))
          {
            for (String cookie : headerFields.get(COOKIE_KEY))
            {
              if (sbuf.length() > 0 && !sbuf.toString().trim().endsWith(";"))
              {
                sbuf.append("; ");
              }
              sbuf.append(cookie);
            }
          }
          connection.addRequestProperty("Cookie", sbuf.toString());
          ins = connection.getInputStream();
          encoding = connection.getHeaderField("Content-Encoding");
          if (encoding != null && encoding.equals("gzip"))
          {
            ins = new GZIPInputStream(ins);
          }
          response = Helper.readResponse(ins, getCharSetFromConnection(connection));
          if (!response.contains("singleItineray-content-body"))
          {
            break;
          }
        }
      }
      else
      {
        if (response.contains("We cannot find the cities you selected."))
        {
          LOG.debug(INSTANCE_NAME + "We cannot find the cities you selected.");
        }
        else if (response.contains("no results available"))
        {
          LOG.debug(INSTANCE_NAME + "No results available.");
        }
        else if (response.contains("Your IP was blocked by our system due to suspicious query load."))
        {
          LOG.debug(INSTANCE_NAME + "Proxy " + currentProxy.address().toString() + " is banned! Changing proxy.");
          this.search(flightGui, statusLabel, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
        }
      }
    }
    catch (ConnectException e)
    {
      this.search(flightGui, statusLabel, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
    }
    catch (Exception e)
    {
      LOG.error("EdreamsFlightObtainer: ", e);
      statusLabel.setForeground(Color.RED);
    }
  }

  private String getPostFormData(Map<String, String> data) throws UnsupportedEncodingException
  {
    Set keys = data.keySet();
    Iterator keyIter = keys.iterator();
    StringBuilder builder = new StringBuilder();
    for (int i = 0; keyIter.hasNext(); i++)
    {
      Object key = keyIter.next();
      if (i != 0)
      {
        builder.append('&');
      }
      builder.append(key).append('=').append(URLEncoder.encode(data.get(key), "UTF-8"));
    }
    return builder.toString();
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

  private HttpURLConnection createHttpProxyConnection(String address, Proxy proxy) throws IOException
  {
    URL url = new URL(address);
    HttpURLConnection conn = proxy == null ? (HttpURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection(proxy);
    conn.addRequestProperty("Connection", "keep-alive");
    conn.addRequestProperty("Cache-Control", "max-age=0");
    conn.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36");
    conn.addRequestProperty("Host", url.getHost());
    conn.addRequestProperty("Referer", url.toString());
    conn.addRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
    conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
    return conn;
  }

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new EdreamsFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      obtainer.search(null, null, "com", "VCE", "BKK", formatter.parse("07.10.2013"), "BKK", "VCE", formatter.parse("21.10.2013"), "VCE", "NYC", formatter.parse("31.10.2013"), 1, false);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
