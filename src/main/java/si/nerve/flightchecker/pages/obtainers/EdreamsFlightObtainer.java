package si.nerve.flightchecker.pages.obtainers;

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
import si.nerve.flightchecker.pages.MultiCityFlightObtainer;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URLEncoder;
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
  private static final Logger LOG = Logger.getLogger(EdreamsFlightObtainer.class);
    private String m_logName;

  @Override
  public void search(
      final FlightsGui flightGui, String addressRoot, String from1, String to1, Date date1,
      String from2, String to2, Date date2, String from3, String to3, Date date3, Integer numOfPersons, boolean changeProxy) throws Exception
  {
    HttpURLConnection connection;
    int sleepMillis = Helper.getSleepMillis(this);
    try
    {
      String langSpec1;
      if ("com".equals(addressRoot))
      {
        m_formatter = new SimpleDateFormat("dd/MM/yyyy");
        langSpec1 = "/flights/search/multidestinations/?tripT=MULTI_SEGMENT&isIframe=undefined";
      }
      else if ("de".equals(addressRoot))
      {
        m_formatter = new SimpleDateFormat("dd/MM/yyyy");
        langSpec1 = "/flug/suchen/rundreise/?tripT=MULTI_SEGMENT&isIframe=undefined";
      }
      else if ("it".equals(addressRoot))
      {
        m_formatter = new SimpleDateFormat("dd/MM/yyyy");
        langSpec1 = "/voli/ricerca/destinazioni-multiple/?tripT=MULTI_SEGMENT&isIframe=undefined";
      }
      else if ("es".equals(addressRoot))
      {
        m_formatter = new SimpleDateFormat("dd/MM/yyyy");
        langSpec1 = "/vuelos/buscador/multidestinos/?tripT=MULTI_SEGMENT&isIframe=undefined";
      }
      else
      {
        m_formatter = new SimpleDateFormat("MM/dd/yyyy");
        return;
      }

      String hostAddress = "http://www.edreams." + addressRoot;
      m_logName = "[" + hostAddress + "] ";
      String address = hostAddress + langSpec1;

      if (changeProxy && Thread.currentThread().isInterrupted())
      {
        throw new InterruptedException();
      }

      Proxy currentProxy = Helper.peekFreeProxy(hostAddress, changeProxy);
      try
      {
        connection = Helper.createHttpProxyConnection(address, currentProxy);
      }
      catch (Exception e)
      {
        LOG.error(m_logName, e);
        return;
      }

      Map<String, List<String>> headerFields = connection.getHeaderFields();
      String newAddress = hostAddress + "/engine/ItinerarySearch/search";
      connection = Helper.createHttpProxyConnection(newAddress, currentProxy);
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
      data.put("fake_filteringCarrier", getFakeFilteringCarrier(addressRoot));
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
      catch (Exception e)
      {
        if ("Unexpected end of file from server".equals(e.getLocalizedMessage())
            || "Connection reset".equals(e.getLocalizedMessage()) || "Connection refused: connect".equals(e.getLocalizedMessage())
            || e.getLocalizedMessage().contains(" 502 ") || e.getLocalizedMessage().contains(" 400 ")
            || e instanceof ConnectException || e instanceof FileNotFoundException)
        {
          LOG.error(m_logName + "Proxy " + currentProxy.address().toString() + " error: " + e.getLocalizedMessage() + " Changing proxy.");
          Thread.sleep(sleepMillis);
          this.search(flightGui, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
        }
        else if (e.getLocalizedMessage().contains(" 503 ")
            || e.getLocalizedMessage().contains(" 504 ")
            || e.getLocalizedMessage().contains(" 403 ")
            || e.getLocalizedMessage().contains(" 500 "))
        {
          LOG.error(m_logName + "Server overloaded. Calling again in " + sleepMillis + " millis.");
          Thread.sleep(sleepMillis);
          this.search(flightGui, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
        }
        else
        {
          LOG.debug(m_logName, e);
        }
        return;
      }

      int responseCode = connection.getResponseCode();
      if (responseCode != 200)
      {
        switch (responseCode)
        {
          case 302:
            LOG.error(m_logName + "Proxy " + currentProxy.address().toString() + " " + connection.getResponseMessage() + ". Changing proxy.");
            Thread.sleep(sleepMillis);
            this.search(flightGui, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
            return;
          default:
            LOG.debug(m_logName + connection.getResponseCode() + " " + connection.getResponseMessage());
            return;
        }
      }

      String encoding = connection.getHeaderField("Content-Encoding");
      if (encoding != null && encoding.equals("gzip"))
      {
        ins = new GZIPInputStream(ins);
      }

      String response = Helper.readResponse(ins, connection);

      if (response.contains("singleItineray-content-body"))
      {
        for (int i = 2; i < 10; i++)
        {
          Document doc = Jsoup.parse(response);
          List<MultiCityFlightData> returnList = extractFlightData(from1, to1, from2, to2, from3, to3, address, doc);

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
              MultiCityFlightTableModel tableModel = (MultiCityFlightTableModel) flightGui.getMainTable().getModel();
              tableModel.setEntityList(new ArrayList<MultiCityFlightData>(flightGui.getFlightQueue()));
              tableModel.fireTableDataChanged();
            }
          }

          newAddress = hostAddress + "/engine/ItinerarySearch/paging?page=" + i + "&isSEM=";
          connection = Helper.createHttpProxyConnection(newAddress, currentProxy);
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
          try
          {
            ins = connection.getInputStream();
          }
          catch (IOException e)
          {
            if (e.getLocalizedMessage().contains(" 502 "))
            {
              LOG.debug(m_logName + "Last page.");
            }
            else if (e instanceof ConnectException || e instanceof SocketException || e.getLocalizedMessage().contains(" 503 "))
            {
              Thread.sleep(sleepMillis);
              this.search(flightGui, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
            }
            else
            {
              LOG.error(m_logName, e);
            }
            return;
          }
          encoding = connection.getHeaderField("Content-Encoding");
          if (encoding != null && encoding.equals("gzip"))
          {
            ins = new GZIPInputStream(ins);
          }
          response = Helper.readResponse(ins, connection);
          if (!response.contains("singleItineray-content-body"))
          {
            break;
          }
        }
      }
      else
      {
        if ("com".equals(addressRoot))
        {
          if (response.contains("We cannot find the cities you selected."))
          {
            LOG.debug(m_logName + "We cannot find the cities you selected.");
          }
          else if (response.contains("no results available"))
          {
            LOG.debug(m_logName + "No results available.");
          }
          else if (response.contains("Your IP was blocked by our system due to suspicious query load."))
          {
            LOG.debug(m_logName + "Proxy " + currentProxy.address().toString() + " is banned! Changing proxy.");
            Thread.sleep(sleepMillis);
            this.search(flightGui, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
          }
          else if (!response.contains("We found multiple options for your trip"))
          {
            LOG.debug(m_logName + response);
          }
        }
        else if ("de".equals(addressRoot))
        {
          if (response.contains("Ihren Zielort nicht gefunden"))
          {
            LOG.debug(m_logName + "Did not find your destination (Ihren Zielort nicht gefunden)");
          }
          else if (response.contains("Wir konnten die von Ihnen ausgewählten Orte nicht finden"))
          {
            LOG.debug(m_logName + "We cannot find the cities you selected. (Wir konnten die von Ihnen ausgewählten Orte nicht finden.)");
          }
          else if (response.contains("Für Ihre Suche gibt es leider keine Ergebnisse."))
          {
            LOG.debug(m_logName + "No results available. (Für Ihre Suche gibt es leider keine Ergebnisse.)");
          }
          else if (response.contains("Your IP was blocked by our system due to suspicious query load."))
          {
            LOG.debug(m_logName + "Proxy " + currentProxy.address().toString() + " is banned! Changing proxy.");
            Thread.sleep(sleepMillis);
            this.search(flightGui, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
          }
          else if (!response.contains("Für Ihre Reise gibt es mehrere Optionen"))
          {
            LOG.debug(m_logName + response);
          }
        }
        else if ("it".equals(addressRoot))
        {
          if (response.contains("Non siamo riusciti a trovare le città richieste."))
          {
            LOG.debug(m_logName + "Did not find your destination (Non siamo riusciti a trovare le città richieste.)");
          }
          else if (response.contains("Non ci sono risultati per la tua ricerca."))
          {
            LOG.debug(m_logName + "No results available. (Non ci sono risultati per la tua ricerca.)");
          }
          else if (response.contains("I parametri di ricerca inseriti non sono corretti. Per favore verificali prima di andare avanti."))
          {
            LOG.debug(m_logName + "We cannot find the cities you selected. (I parametri di ricerca inseriti non sono corretti. Per favore verificali prima di andare avanti.)");
          }
          else if (response.contains("Your IP was blocked by our system due to suspicious query load."))
          {
            LOG.debug(m_logName + "Proxy " + currentProxy.address().toString() + " is banned! Changing proxy.");
            Thread.sleep(sleepMillis);
            this.search(flightGui, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
          }
          else if (!response.contains("Abbiamo trovato diverse opzioni per il tuo viaggio"))
          {
            LOG.debug(m_logName + response);
          }
        }
        else if ("es".equals(addressRoot))
        {
          if (response.contains("No hemos podido localizar las ciudades indicadas."))
          {
            LOG.debug(m_logName + "We cannot find the cities you selected. (No hemos podido localizar las ciudades indicadas.)");
          }
          else if (response.contains("No hay resultados disponibles para tu búsqueda."))
          {
            LOG.debug(m_logName + "No results available. (No hay resultados disponibles para tu búsqueda.)");
          }
          else if (response.contains("Your IP was blocked by our system due to suspicious query load."))
          {
            LOG.debug(m_logName + "Proxy " + currentProxy.address().toString() + " is banned! Changing proxy.");
            Thread.sleep(sleepMillis);
            this.search(flightGui, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
          }
          else if (!response.contains("Hemos encontrado varias opciones para tu viaje"))
          {
            LOG.debug(m_logName + response);
          }
        }
        else
        {
          System.out.println();
        }
      }
    }
    catch (IOException e)
    {
      if (e.getLocalizedMessage().contains(" 502 "))
      {
        LOG.debug(m_logName + "Last page.");
      }
      else if (e instanceof SocketException || e.getLocalizedMessage().contains("Premature EOF") || e.getLocalizedMessage().contains(" 400 "))
      {
        Thread.sleep(sleepMillis);
        this.search(flightGui, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
      }
      else
      {
        LOG.error(m_logName, e);
      }
    }
    catch (Exception e)
    {
      LOG.error(m_logName, e);
    }
  }

  private List<MultiCityFlightData> extractFlightData(String from1, String to1, String from2, String to2, String from3, String to3, String address, Document doc)
  {
    Elements links = doc.select("div[class^=singleItineray-content-body]");

    List<MultiCityFlightData> returnList = new ArrayList<MultiCityFlightData>();
    for (Element link : links)
    {
      String priceNumber = null;
      for (Node node : link.select("div[class^=singleItinerayPrice]").first().childNodes())
      {
        if (node instanceof TextNode)
        {
          final String text = ((TextNode) node).text().trim();
          if (text.length() > 0 && (priceNumber == null || priceNumber.length() == 0))
          {
            priceNumber = text.replaceAll("[\\D]", "");
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
          LOG.error(m_logName + "departureTime or fromAirportName not found!");
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
          LOG.error(m_logName + "arrivalTime or toAirportName not found!");
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
          LOG.error(m_logName + "duration not found!");
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
          LOG.error(m_logName + "layover not found!");
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
            PriceType.getInstance(s),
            legList,
            "",
            address
        );
        returnList.add(flightData);
      }
    }
    return returnList;
  }

  private String getFakeFilteringCarrier(String addressRoot)
  {
    if (addressRoot.equals("com"))
    {
      return "All Airlines";
    }
    else if (addressRoot.equals("de"))
    {
      return "Alle Fluggesellschaften";
    }
    else if (addressRoot.equals("es"))
    {
      return "Todas las compañías";
    }
    else if (addressRoot.equals("it"))
    {
      return "Tutte le compagnie";
    }
    return null;
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

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new EdreamsFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      obtainer.search(null, "com", "VCE", "BKK", formatter.parse("07.10.2013"), "BKK", "VCE", formatter.parse("21.10.2013"), "VCE", "NYC", formatter.parse("31.10.2013"), 1, false);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
