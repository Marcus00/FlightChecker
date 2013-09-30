package si.nerve.flightchecker.pages.obtainers;

import java.awt.Color;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketException;
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

/**
 * Created: 10.8.13 20:39
 */
public class KayakFlightObtainer implements MultiCityFlightObtainer
{
  private String m_addressDot, m_address, m_logName;
  private SimpleDateFormat m_formatter;
  private Proxy m_currentProxy;
  private static final int MAX_RETRIES = 45;
  public static final int TIME_MILLIS = 30000;
  private static final Logger LOG = Logger.getLogger(KayakFlightObtainer.class);

  @Override
  public void search(
      final FlightsGui flightGui, JLabel statusLabel, String addressRoot, String from1, String to1, Date date1,
      String from2, String to2, Date date2, String from3, String to3, Date date3, Integer numOfPersons, boolean changeProxy)
      throws Exception
  {
    try
    {
      m_formatter = new SimpleDateFormat("yyyy-MM-dd");

      m_addressDot = addressRoot;
      String hostAddress = "http://www.kayak." + m_addressDot;
      m_logName = "[" + hostAddress + "] ";
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
      URLConnection connection;

      if (changeProxy && Thread.currentThread().isInterrupted())
      {
        throw new InterruptedException();
      }

      m_currentProxy = Helper.peekFreeProxy(hostAddress, changeProxy);
      try
      {
        connection = Helper.createHttpProxyConnection(m_address, m_currentProxy);
      }
      catch (Exception e)
      {
        e.printStackTrace();
        return;
      }
      InputStream ins = connection.getInputStream();
      String encoding = connection.getHeaderField("Content-Encoding");
      if (encoding != null && encoding.equals("gzip"))
      {
        ins = new GZIPInputStream(ins);
      }
      String response = Helper.readResponse(ins, connection);

      int streamingStartLocation = response.indexOf("window.Streaming");
      if (streamingStartLocation == -1)
      {
        if (response.contains("/help/human.html"))
        {
          LOG.error(m_logName + "Proxy " + m_currentProxy.address().toString() + " is banned. Changing proxy.");
          this.search(flightGui, statusLabel, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
          return;
        }
        else
        {
          return;
        }
      }
      int streamingEndLocation = response.indexOf(";", streamingStartLocation);
      String streamingCommand;
      try
      {
        streamingCommand = response.substring(streamingStartLocation, streamingEndLocation);
      }
      catch (Exception e)
      {
        LOG.error(m_logName + "window.Streaming not found!", e);
        return;
      }
      int timeStart = streamingCommand.indexOf(",") + 1;
      int timeEnd = streamingCommand.indexOf(",", timeStart) - 1;
      Long time = Long.parseLong(streamingCommand.substring(timeStart, timeEnd).trim());

      int searchIdCmdStartLocation = response.indexOf("setTargeting('searchid'");
      int searchIdCmdEndLocation = response.indexOf(")", searchIdCmdStartLocation);
      String searchLocationCommand = response.substring(searchIdCmdStartLocation, searchIdCmdEndLocation);
      int searchIdStartLocation = searchLocationCommand.indexOf(",") + 3;
      int searchIdEndLocation = searchLocationCommand.indexOf("'", searchIdStartLocation);
      String searchId = searchLocationCommand.substring(searchIdStartLocation, searchIdEndLocation);

      long startTime = System.currentTimeMillis();
      //long middleTime;
      int i = 1;

      while (time != null && (System.currentTimeMillis() - startTime < TIME_MILLIS) && i <= MAX_RETRIES)
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
    catch (IOException e)
    {
      if (e.getLocalizedMessage().contains(" 502 "))
      {
        LOG.debug(m_logName + "Last page.");
      }
      else if (e instanceof SocketException || e.getLocalizedMessage().contains(" 400 ") || e.getLocalizedMessage().contains("Premature EOF"))
      {
        this.search(flightGui, statusLabel, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
      }
      else
      {
        LOG.error(m_logName, e);
      }
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
    HttpURLConnection connection;
    try
    {
      connection = Helper.createHttpProxyConnection(address, m_currentProxy);
    }
    catch (Exception e)
    {
      LOG.error(e);
      return null;
    }
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
    if (encoding != null && encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }

    String response = Helper.readResponse(ins, connection);

    int newTimeCommandStart = response.indexOf("Streaming.lastPoll=");
    if (newTimeCommandStart < 0)
    {
      if (response.contains("content_div"))
      {
        return new KayakResult(null, response);
      }
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

  private void addToQueue(FlightsGui flightGui, JLabel statusLabel, String response)
  {
    if (response.contains("addAdt('results/details/button')"))
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

            statusLabel.setForeground(!statusLabel.getForeground().equals(Color.DARK_GRAY) ? Color.DARK_GRAY : Color.BLACK);

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
        LOG.error(m_logName + "Jsoup parsing failed!", e);
      }
    }
    else
    {
      if ("pl".equals(m_addressDot))
      {
        if (response.contains("Przeszukiwane przez nas linie lotnicze nie zwróciły żadnych wyników."))
        {
          LOG.debug(m_logName + "No results available. (Przeszukiwane przez nas linie lotnicze nie zwróciły żadnych wyników.)");
        }
        else if (response.length() > 1000)
        {
          LOG.debug(m_logName + "No results available. (Aucun résultat n'a été trouvé parmi les compagnies recherchées.)");
        }
      }
      else if ("nl".equals(m_addressDot))
      {
        if (response.contains("Geen resultaten gevonden tussen de vliegmaatschappijen waar wij zoeken."))
        {
          LOG.debug(m_logName + "No results available. (Geen resultaten gevonden tussen de vliegmaatschappijen waar wij zoeken.)");
        }
        else if (response.length() > 1000)
        {
          LOG.debug(m_logName + "No results available. (Aucun résultat n'a été trouvé parmi les compagnies recherchées.)");
        }
      }
      else if ("fr".equals(m_addressDot))
      {
        if (response.contains("Aucun résultat n'a été trouvé parmi les compagnies recherchées."))
        {
          LOG.debug(m_logName + "No results available. (Aucun résultat n'a été trouvé parmi les compagnies recherchées.)");
        }
        else if (response.length() > 1000)
        {
          LOG.debug(m_logName + "No results available. (Aucun résultat n'a été trouvé parmi les compagnies recherchées.)");
        }
      }
      else if ("com".equals(m_addressDot))
      {
        if (response.contains("No results were found among the airlines we search."))
        {
          LOG.debug(m_logName + "No results available. (No results were found among the airlines we search.)");
        }
        else if (response.length() > 1000)
        {
          LOG.debug(m_logName + "No results available. (Aucun résultat n'a été trouvé parmi les compagnies recherchées.)");
        }
      }
      else if ("es".equals(m_addressDot))
      {
        if (response.contains("No se encontraron resultados con las aerolíneas elegidas."))
        {
          LOG.debug(m_logName + "No results available. (No se encontraron resultados con las aerolíneas elegidas.)");
        }
        else if (response.length() > 1000)
        {
          LOG.debug(m_logName + "No results available. (Aucun résultat n'a été trouvé parmi les compagnies recherchées.)");
        }
      }
      else if ("it".equals(m_addressDot))
      {
        if (response.contains("Nessun risultato trovato tra le compagnie aeree che confrontiamo."))
        {
          LOG.debug(m_logName + "No results available. (Nessun risultato trovato tra le compagnie aeree che confrontiamo.)");
        }
        else if (response.length() > 1000)
        {
          LOG.debug(m_logName + "No results available. (Aucun résultat n'a été trouvé parmi les compagnies recherchées.)");
        }
      }
      else if ("de".equals(m_addressDot))
      {
        if (response.contains("Es wurden keine Ergebnisse unter den Airlines gefunden, die wir durchsuchen."))
        {
          LOG.debug(m_logName + "No results available. (Es wurden keine Ergebnisse unter den Airlines gefunden, die wir durchsuchen.)");
        }
        else if (response.length() > 1000)
        {
          LOG.debug(m_logName + "No results available. (Aucun résultat n'a été trouvé parmi les compagnies recherchées.)");
        }
      }
      else if ("co.uk".equals(m_addressDot))
      {
        if (response.contains("No results were found among the airlines we search."))
        {
          LOG.debug(m_logName + "No results available. (No results were found among the airlines we search.)");
        }
        else if (response.length() > 1000)
        {
          LOG.debug(m_logName + "No results available. (Aucun résultat n'a été trouvé parmi les compagnies recherchées.)");
        }
      }
      else
      {
        LOG.error(m_logName + "Streaming.lastPoll= not found in response (length=" + response.length() + ")!");
      }
    }
  }

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new KayakFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      obtainer.search(null, null, "com", "vce", "bkk", formatter.parse("20.12.2013"), "bkk", "vce", formatter.parse("07.01.2014"), "bkk", "vce", formatter.parse("07.01.2014"), 1,
          false);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
