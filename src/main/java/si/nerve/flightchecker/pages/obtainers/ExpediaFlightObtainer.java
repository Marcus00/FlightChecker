package si.nerve.flightchecker.pages.obtainers;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
import si.nerve.flightchecker.pages.MultiCityFlightObtainer;

/**
 * Created: 10.8.13 20:39
 */
public class ExpediaFlightObtainer implements MultiCityFlightObtainer
{
  private SimpleDateFormat m_formatter;
  private static final Logger LOG = Logger.getLogger(ExpediaFlightObtainer.class);

  @Override
  public void search(
      final FlightsGui flightGui, String addressRoot, String from1, String to1, Date date1,
      String from2, String to2, Date date2, String from3, String to3, Date date3, Integer numOfPersons, boolean changeProxy) throws Exception
  {
    if ("com".equals(addressRoot))
    {
      m_formatter = new SimpleDateFormat("MM/dd/yyyy");
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

    String hostAddress = "http://www.expedia." + addressRoot;
    final String logName = "[" + hostAddress + "] ";
    String address = hostAddress + "/Flight-SearchResults?trip=multi" +
        "&leg1=" + URLEncoder.encode("from:" + from1 + ",frCode:undefined,to:" + to1 + ",toCode:undefined,departure:" + m_formatter.format(date1) + "TANYT", "UTF-8") +
        "&leg2=" + URLEncoder.encode("from:" + from2 + ",frCode:undefined,to:" + to2 + ",toCode:undefined,departure:" + m_formatter.format(date2) + "TANYT", "UTF-8");
    if (from3 != null && to3 != null && date3 != null)
    {
      address += "&leg3=" + URLEncoder.encode("from:" + from3 + ",frCode:undefined,to:" + to3 + ",toCode:undefined,departure:" + m_formatter.format(date3) + "TANYT", "UTF-8");
    }
    address += "&passengers=" + URLEncoder.encode("children:0,adults:" + numOfPersons + ",seniors:0,infantinlap:Y", "UTF-8") +
        "&options=" + URLEncoder.encode("cabinclass:economy,nopenalty:N,sortby:price", "UTF-8") +
        "&mode=search";

    URL url = new URL(address);
    URLConnection connection = Helper.createHttpConnection(url);
    InputStream ins = connection.getInputStream();
    String encoding = connection.getHeaderField("Content-Encoding");
    if (encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }
    String response = Helper.readResponse(ins, connection);
    int streamingStartLocation = response.indexOf("Flight-SearchResults");
    int streamingEndLocation = response.indexOf("';", streamingStartLocation);
    try
    {
      address = hostAddress + "/" + response.substring(streamingStartLocation, streamingEndLocation);
    }
    catch (Exception e)
    {
      LOG.error(logName + "Flight-SearchResults not found!", e);
      return;
    }
    url = new URL(address);
    StringBuilder sbuf = new StringBuilder();

    if (Thread.currentThread().isInterrupted())
    {
      throw new InterruptedException();
    }

    for (String cookie : connection.getHeaderFields().get("Set-Cookie"))
    {
      if (sbuf.length() > 0)
      {
        sbuf.append(";");
      }
      sbuf.append(cookie);
    }
    String cookies = sbuf.toString();
    connection = Helper.createHttpConnection(url);
    connection.addRequestProperty("Cookie", cookies);
    ins = connection.getInputStream();
    encoding = connection.getHeaderField("Content-Encoding");
    if (encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }
    response = Helper.readResponse(ins, connection);
    String startJsonString = "<script id='jsonData' type=\"text/x-jquery-tmpl\">";
    int startIndex = response.indexOf(startJsonString);
    if (startIndex == -1)
    {
      debugResponse(addressRoot, logName, response);
      return;
    }
    streamingStartLocation = startIndex + startJsonString.length();
    streamingEndLocation = response.indexOf("</script>", streamingStartLocation);
    try
    {
      String jsonString = response.substring(streamingStartLocation, streamingEndLocation);
      jsonString = jsonString.substring(jsonString.indexOf('['), jsonString.length());
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      SimpleDateFormat localFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

      Object obj = JSONValue.parse(jsonString);
      JSONArray array = (JSONArray)obj;
      List<MultiCityFlightData> returnList = new ArrayList<MultiCityFlightData>();
      for (Object anArray : array)
      {
        JSONObject flight = (JSONObject)anArray;
        String localeTotalPrice = (String)flight.get("localeTotalPrice");
        JSONArray legs = (JSONArray)flight.get("legs");
        LinkedList<FlightLeg> legList = new LinkedList<FlightLeg>();
        for (Object leg1 : legs)
        {
          JSONObject leg = (JSONObject)leg1;
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
              address
          );
          returnList.add(flightData);
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
    }
    catch (Exception e)
    {
      LOG.error("ExpediaFlightObtainer: JSON decoding failed!", e);
    }
  }

  private void debugResponse(String addressRoot, String logName, String response)
  {
    if (addressRoot.equals("dk"))
    {
      if (response.contains("Vi kunne ikke finde lufthavne, der passede til din søgning efter"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (Vi kunne ikke finde lufthavne, der passede til din søgning efter...)");
      }
      else if (response.contains("Der blev ikke fundet nogen fly, der stemte overens med søgekriterierne"))
      {
        LOG.debug(logName + "No results available. (Der blev ikke fundet nogen fly, der stemte overens med søgekriterierne)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("de"))
    {
      if (response.contains("Es wurden keine Flüge gefunden, die Ihren Suchkriterien entsprechen"))
      {
        LOG.debug(logName + "No results available. (Es wurden keine Flüge gefunden, die Ihren Suchkriterien entsprechen)");
      }
      else if (response.contains("Es wurde kein Flughafen gefunden, der Ihrer Suche nach"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (Es wurde kein Flughafen gefunden, der Ihrer Suche nach...)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("com"))
    {
      if (response.contains("We could not find any airports that match your search for"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (We could not find any airports that match your search for...)");
      }
      else if (response.contains("No flights were found that matched your search"))
      {
        LOG.debug(logName + "No results available. (No flights were found that matched your request)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("at"))
    {
      if (response.contains("Es wurden keine Flüge gefunden, die Ihren Suchkriterien entsprechen"))
      {
        LOG.debug(logName + "No results available. (Es wurden keine Flüge gefunden, die Ihren Suchkriterien entsprechen)");
      }
      else if (response.contains("Es wurde kein Flughafen gefunden, der Ihrer Suche nach"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (Es wurde kein Flughafen gefunden, der Ihrer Suche nach...)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("nl"))
    {
      if (response.contains("We kunnen geen luchthavens vinden die overeenkomen met de zoekopdracht naar"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (We kunnen geen luchthavens vinden die overeenkomen met de zoekopdracht naar...)");
      }
      else if (response.contains("Er zijn geen vluchten gevonden die aan je verzoek voldoen"))
      {
        LOG.debug(logName + "No results available. (Er zijn geen vluchten gevonden die aan je verzoek voldoen)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("it"))
    {
      if (response.contains("Impossibile trovare aeroporti corrispondenti alla ricerca di"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (Impossibile trovare aeroporti corrispondenti alla ricerca di...)");
      }
      else if (response.contains("Non è stato trovato alcun volo che soddisfi i criteri"))
      {
        LOG.debug(logName + "No results available. (Non è stato trovato alcun volo che soddisfi i criteri)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("co.uk"))
    {
      if (response.contains("We could not find any airports that match your search for"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (We could not find any airports that match your search for)");
      }
      else if (response.contains("No flights were found that matched your request"))
      {
        LOG.debug(logName + "No results available. (No flights were found that matched your request)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("es"))
    {
      if (response.contains("No hemos encontrado aeropuertos que coincidan con tu búsqueda de"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (No hemos encontrado aeropuertos que coincidan con tu búsqueda de)");
      }
      else if (response.contains("No se han encontrado vuelos que coincidan con tu solicitud"))
      {
        LOG.debug(logName + "No results available. (No se han encontrado vuelos que coincidan con tu solicitud)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("fr"))
    {
      if (response.contains("Aucun aéroport ne correspond à votre recherche pour"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (Aucun aéroport ne correspond à votre recherche pour...)");
      }
      else if (response.contains("Aucun vol ne correspond à vos critères"))
      {
        LOG.debug(logName + "No results available. (Aucun vol ne correspond à vos critères)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("ca"))
    {
      if (response.contains("No flights were found that matched your request"))
      {
        LOG.debug(logName + "No results available. (No flights were found that matched your request)");
      }
      else if (response.contains("We could not find any airports that match your search for"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (We could not find any airports that match your search for...)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("ie"))
    {
      if (response.contains("No flights were found that matched your request"))
      {
        LOG.debug(logName + "No results available. (No flights were found that matched your request)");
      }
      else if (response.contains("We could not find any airports that match your search for"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (We could not find any airports that match your search for...)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("be"))
    {
      if (response.contains("We kunnen geen luchthavens vinden die overeenkomen met de zoekopdracht naar"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (We kunnen geen luchthavens vinden die overeenkomen met de zoekopdracht naar...)");
      }
      else if (response.contains("Er zijn geen vluchten gevonden die aan je verzoek voldoen"))
      {
        LOG.debug(logName + "No results available. (Er zijn geen vluchten gevonden die aan je verzoek voldoen)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else if (addressRoot.equals("se"))
    {
      if (response.contains("Det går inte att hitta några flygplatser som matchar din sökning efter"))
      {
        LOG.debug(logName + "We cannot find the cities you selected. (Det går inte att hitta några flygplatser som matchar din sökning efter...)");
      }
      else if (response.contains("Inga flyg stämde med sökvillkoren"))
      {
        LOG.debug(logName + "No results available. (Inga flyg stämde med sökvillkoren)");
      }
      else
      {
        LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
      }
    }
    else
    {
      LOG.error(logName + "JSON decoding failed for www.expedia." + addressRoot);
    }
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

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new ExpediaFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      obtainer.search(null, "de", "LJU", "NYC", formatter.parse("18.12.2013"), "NYC", "VIE", formatter.parse("07.01.2014"), "NYC", "VIE", formatter.parse("07.01.2014"), 1, false);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
