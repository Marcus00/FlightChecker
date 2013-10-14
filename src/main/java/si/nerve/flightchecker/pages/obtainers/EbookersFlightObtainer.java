package si.nerve.flightchecker.pages.obtainers;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

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

/**
 * Created: 10.8.13 20:39
 */
public class EbookersFlightObtainer implements MultiCityFlightObtainer
{
  private SimpleDateFormat m_dateFormatter = new SimpleDateFormat("dd.MM.yyyy");
  private static final Logger LOG = Logger.getLogger(EbookersFlightObtainer.class);
  private Pattern m_pattern = Pattern.compile("\\d+");

  @Override
  public void search(
      final FlightsGui flightGui, String addressRoot, String from1, String to1,
      Date date1, String from2, String to2, Date date2, String from3, String to3, Date date3, Integer numOfPersons, boolean changeProxy) throws Exception
  {
    SimpleDateFormat formatter;
    if ("com".equals(addressRoot))
    {
      formatter = new SimpleDateFormat("dd/MM/yy");
    }
    else if ("de".equals(addressRoot) || "dk".equals(addressRoot) || "at".equals(addressRoot))
    {
      formatter = new SimpleDateFormat("dd.MM.yyyy");
    }
    else if ("nl".equals(addressRoot))
    {
      formatter = new SimpleDateFormat("dd-MM-yyyy");
    }
    else if ("it".equals(addressRoot) || "co.uk".equals(addressRoot) || "es".equals(addressRoot) || "fr".equals(addressRoot) || "pl".equals(addressRoot)
        || "ca".equals(addressRoot) || "ie".equals(addressRoot) || "be".equals(addressRoot))
    {
      formatter = new SimpleDateFormat("dd/MM/yyyy");
    }
    else if ("se".equals(addressRoot))
    {
      formatter = new SimpleDateFormat("yyyy-MM-dd");
    }
    else
    {
      formatter = new SimpleDateFormat("MM/dd/yyyy");
    }
    String hostAddress = "http://www.ebookers." + addressRoot;
    String logName = "[" + hostAddress + "] ";
    StringBuilder builder = new StringBuilder(hostAddress).append("/shop/airsearch?type=air&ar.type=multiCity");
    String encode0 = URLEncoder.encode("[0]", "UTF-8");
    String encode1 = URLEncoder.encode("[1]", "UTF-8");
    String encode2 = URLEncoder.encode("[2]", "UTF-8");
    String encode3 = URLEncoder.encode("[3]", "UTF-8");
    builder
        .append("&ar.mc.slc").append(encode0).append(".orig.key=").append(URLEncoder.encode(from1, "UTF-8"))
        .append("&ar.mc.slc").append(encode0).append(".dest.key=").append(URLEncoder.encode(to1, "UTF-8"))
        .append("&ar.mc.slc").append(encode0).append(".date=").append(URLEncoder.encode(formatter.format(date1), "UTF-8"))
        .append("&ar.mc.slc").append(encode0).append(".time=Anytime")
        .append("&ar.mc.slc").append(encode1).append(".orig.key=").append(URLEncoder.encode(from2, "UTF-8"))
        .append("&ar.mc.slc").append(encode1).append(".dest.key=").append(URLEncoder.encode(to2, "UTF-8"))
        .append("&ar.mc.slc").append(encode1).append(".date=").append(URLEncoder.encode(formatter.format(date2), "UTF-8"))
        .append("&ar.mc.slc").append(encode1).append(".time=Anytime")
        .append("&ar.mc.slc").append(encode2).append(".orig.key=");
    if (from3 != null)
    {
      builder.append(URLEncoder.encode(from3, "UTF-8"));
    }
    builder.append("&ar.mc.slc").append(encode2).append(".dest.key=");
    if (to3 != null)
    {
      builder.append(URLEncoder.encode(to3, "UTF-8"));
    }
    builder.append("&ar.mc.slc").append(encode2).append(".date=");
    if (date3 != null)
    {
      builder.append(URLEncoder.encode(formatter.format(date3), "UTF-8"));
    }
    builder.append("&ar.mc.slc").append(encode2).append(".time=Anytime")
        .append("&ar.mc.slc").append(encode3).append(".orig.key=")
        .append("&ar.mc.slc").append(encode3).append(".dest.key=")
        .append("&ar.mc.slc").append(encode3).append(".date=")
        .append("&ar.mc.slc").append(encode3).append(".time=Anytime")
        .append("&ar.mc.numAdult=").append(numOfPersons).append("&ar.mc.numSenior=0&ar.mc.numChild=0&ar.mc.child").append(encode0)
        .append("=&ar.mc.child").append(encode1)
        .append("=&ar.mc.child").append(encode2)
        .append("=&ar.mc.child").append(encode3)
        .append("=&ar.mc.child").append(URLEncoder.encode("[4]", "UTF-8"))
        .append("=&ar.mc.child").append(URLEncoder.encode("[5]", "UTF-8"))
        .append("=&ar.mc.child").append(URLEncoder.encode("[6]", "UTF-8"))
        .append("=&ar.mc.child").append(URLEncoder.encode("[7]", "UTF-8"))
        .append("=&_ar.mc.nonStop=0&_ar.mc.narrowSel=0&ar.mc.narrow=airlines&ar.mc.carriers").append(encode0)
        .append("=&ar.mc.carriers").append(encode1)
        .append("=&ar.mc.carriers").append(encode2)
        .append("=&ar.mc.cabin=C&search=").append(URLEncoder.encode("Search Flights", "UTF-8"));

    String address = builder.toString();

    URL url = new URL(address);
    URLConnection connection = Helper.createHttpConnection(url);
    InputStream ins;
    try
    {
      try
      {
        ins = connection.getInputStream();
      }
      catch (Exception e)
      {
        if (e instanceof IOException && e.getLocalizedMessage().contains(" 502 "))
        {
          LOG.debug(logName + "Last page.");
        }
        else if (e instanceof ConnectException)
        {
          this.search(flightGui, addressRoot, from1, to1, date1, from2, to2, date2, from3, to3, date3, numOfPersons, true);
        }
        else
        {
          LOG.error(logName, e);
        }
        return;
      }
      String encoding = connection.getHeaderField("Content-Encoding");
      if (encoding.equals("gzip"))
      {
        ins = new GZIPInputStream(ins);
      }
      String response = Helper.readResponse(ins, connection);

      Document doc = Jsoup.parse(response);
      Elements links = doc.select("div[class^=airResultsCard withSelectSliceCheckbox optimizedCard]");
      for (Element link : links)
      {
        Element priceElement = link.select("span.price > span").first();
        String monSign = "";
        int priceAmount = -1;

        for (Node node : priceElement.childNodes())
        {
          if (node instanceof TextNode)
          {
            String priceStringWithCurrency = ((TextNode)node).text();
            priceAmount = Integer.parseInt(priceStringWithCurrency.replaceAll("[\\D]", "").replace("&nbsp;", ""));

            Elements moneySymbol = priceElement.select("span.money-symbol");
            if (moneySymbol != null && moneySymbol.size() > 0)
            {
              monSign = moneySymbol.first().text();
            }
            else
            {
              monSign = PriceType.getInstance(priceStringWithCurrency).getMonSign();
            }
          }
        }

        LinkedList<FlightLeg> legs = new LinkedList<FlightLeg>();

        int legNum = 1;
        for (Element leg : link.select("div[class^=airSlice slice]"))
        {
          Element departureInfo = leg.select("div[class=info departure]").first();
          Element fromAirport = departureInfo.select("span.airportCode > abbr").first();
          String fromAirportName = fromAirport.attr("title");
          String fromAirportCode = fromAirport.text();
          Date fromDate = legNum == 1 ? date1 : legNum == 2 ? date2 : date3;
          String fromLocalTime = m_dateFormatter.format(fromDate) + " " + departureInfo.select("span.heading").text() + ":00";

          Element arrivalInfo = leg.select("div[class=info infoArrival]").first();
          Element toAirport = arrivalInfo.select("span.airportCode > abbr").first();
          String toAirportName = toAirport.attr("title");
          String toAirportCode = toAirport.text();
          String alerts = leg.select("div.messages > p.alert").text();
          int days = 0;
          if (alerts != null && alerts.contains("later"))
          {
            Matcher matcher = m_pattern.matcher(alerts);
            if (matcher.find())
            {
              days = Integer.parseInt(matcher.group());
            }
          }
          Calendar calendar = Calendar.getInstance();
          calendar.setTime(fromDate);
          calendar.add(Calendar.DAY_OF_YEAR, days);
          String toLocalTime = m_dateFormatter.format(calendar.getTime()) + " " + arrivalInfo.select("span.heading").text() + ":00";

          Element stopsInfo = leg.select("div[class=info stops]").first();
          String duration = stopsInfo.select("span.duration").text();
          String layovers = stopsInfo.select("span.heading").text();
          legs.add(new FlightLeg(
              fromAirportName,
              fromAirportCode,
              fromLocalTime,
              toAirportName,
              toAirportCode,
              toLocalTime,
              duration,
              layovers
          ));
          legNum++;
        }

        if (legs.size() > 1)
        {
          Elements select = link.select("a.actFastAlert > em");
          String ticketsLeft = null;
          if (select != null && select.size() > 0)
          {
            ticketsLeft = select.first().text();
            if (ticketsLeft != null && ticketsLeft.length() > 0)
            {
              ticketsLeft += " left";
            }
          }

          MultiCityFlightData flightData = new MultiCityFlightData(
              0,
              hostAddress,
              priceAmount,
              PriceType.getInstance(monSign),
              legs,
              ticketsLeft,
              address
          );

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
      LOG.error(logName + "Jsoup parsing failed!", e);
    }
  }

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new EbookersFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      obtainer.search(null, "com", "vce", "bkk", formatter.parse("20.12.2013"), "bkk", "vce", formatter.parse("07.01.2014"), "bkk", "vce", formatter.parse("07.01.2014"), 1, false);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
