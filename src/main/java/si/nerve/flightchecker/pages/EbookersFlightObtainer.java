package si.nerve.flightchecker.pages;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
import javax.swing.JLabel;

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

/**
 * Created: 10.8.13 20:39
 */
public class EbookersFlightObtainer implements MultiCityFlightObtainer
{
  private SimpleDateFormat m_formatter;
  private SimpleDateFormat m_dateFormatter = new SimpleDateFormat("dd.MM.yyyy");
  private static final Logger LOG = Logger.getLogger(EbookersFlightObtainer.class);
  private Pattern m_pattern = Pattern.compile("\\d+");

  @Override
  public void search(final FlightsGui flightGui, JLabel statusLabel, String addressRoot, String from1, String to1,
      Date date1, String from2, String to2, Date date2, String from3, String to3, Date date3, Integer numOfPersons) throws Exception
  {
    if ("com".equals(addressRoot))
    {
      m_formatter = new SimpleDateFormat("dd/MM/yy");
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
        || "ca".equals(addressRoot) || "ie".equals(addressRoot) || "be".equals(addressRoot))
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
    String hostAddress = "http://www.ebookers." + addressRoot;
    StringBuilder builder = new StringBuilder(hostAddress).append("/shop/airsearch?type=air&ar.type=multiCity");

    builder
        .append("&ar.mc.slc").append(URLEncoder.encode("[0]", "UTF-8")).append(".orig.key=").append(URLEncoder.encode(from1, "UTF-8"))
        .append("&ar.mc.slc").append(URLEncoder.encode("[0]", "UTF-8")).append(".dest.key=").append(URLEncoder.encode(to1, "UTF-8"))
        .append("&ar.mc.slc").append(URLEncoder.encode("[0]", "UTF-8")).append(".date=").append(URLEncoder.encode(m_formatter.format(date1), "UTF-8"))
        .append("&ar.mc.slc").append(URLEncoder.encode("[0]", "UTF-8")).append(".time=Anytime")
        .append("&ar.mc.slc").append(URLEncoder.encode("[1]", "UTF-8")).append(".orig.key=").append(URLEncoder.encode(from2, "UTF-8"))
        .append("&ar.mc.slc").append(URLEncoder.encode("[1]", "UTF-8")).append(".dest.key=").append(URLEncoder.encode(to2, "UTF-8"))
        .append("&ar.mc.slc").append(URLEncoder.encode("[1]", "UTF-8")).append(".date=").append(URLEncoder.encode(m_formatter.format(date2), "UTF-8"))
        .append("&ar.mc.slc").append(URLEncoder.encode("[1]", "UTF-8")).append(".time=Anytime")
        .append("&ar.mc.slc").append(URLEncoder.encode("[2]", "UTF-8")).append(".orig.key=").append(URLEncoder.encode(from3, "UTF-8"))
        .append("&ar.mc.slc").append(URLEncoder.encode("[2]", "UTF-8")).append(".dest.key=").append(URLEncoder.encode(to3, "UTF-8"))
        .append("&ar.mc.slc").append(URLEncoder.encode("[2]", "UTF-8")).append(".date=").append(URLEncoder.encode(m_formatter.format(date3), "UTF-8"))
        .append("&ar.mc.slc").append(URLEncoder.encode("[2]", "UTF-8")).append(".time=Anytime")
        .append("&ar.mc.slc").append(URLEncoder.encode("[3]", "UTF-8")).append(".orig.key=")
        .append("&ar.mc.slc").append(URLEncoder.encode("[3]", "UTF-8")).append(".dest.key=")
        .append("&ar.mc.slc").append(URLEncoder.encode("[3]", "UTF-8")).append(".date=")
        .append("&ar.mc.slc").append(URLEncoder.encode("[3]", "UTF-8")).append(".time=Anytime")
        .append("&ar.mc.numAdult=").append(numOfPersons).append("&ar.mc.numSenior=0&ar.mc.numChild=0&ar.mc.child").append(URLEncoder.encode("[0]", "UTF-8"))
        .append("=&ar.mc.child").append(URLEncoder.encode("[1]", "UTF-8"))
        .append("=&ar.mc.child").append(URLEncoder.encode("[2]", "UTF-8"))
        .append("=&ar.mc.child").append(URLEncoder.encode("[3]", "UTF-8"))
        .append("=&ar.mc.child").append(URLEncoder.encode("[4]", "UTF-8"))
        .append("=&ar.mc.child").append(URLEncoder.encode("[5]", "UTF-8"))
        .append("=&ar.mc.child").append(URLEncoder.encode("[6]", "UTF-8"))
        .append("=&ar.mc.child").append(URLEncoder.encode("[7]", "UTF-8"))
        .append("=&_ar.mc.nonStop=0&_ar.mc.narrowSel=0&ar.mc.narrow=airlines&ar.mc.carriers").append(URLEncoder.encode("[0]", "UTF-8"))
        .append("=&ar.mc.carriers").append(URLEncoder.encode("[1]", "UTF-8"))
        .append("=&ar.mc.carriers").append(URLEncoder.encode("[2]", "UTF-8"))
        .append("=&ar.mc.cabin=C&search=").append(URLEncoder.encode("Search Flights", "UTF-8"));

    String address = builder.toString();

    URL url = new URL(address);
    URLConnection connection = createHttpConnection(url);
    InputStream ins = connection.getInputStream();
    String encoding = connection.getHeaderField("Content-Encoding");
    if (encoding.equals("gzip"))
    {
      ins = new GZIPInputStream(ins);
    }
    String response = Helper.readResponse(ins, getCharSetFromConnection(connection));
    try
    {
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

        for (Element leg : link.select("div[class^=airSlice slice]"))
        {
          Element departureInfo = leg.select("div[class=info departure]").first();
          Element fromAirport = departureInfo.select("span.airportCode > abbr").first();
          String fromAirportName = fromAirport.attr("title");
          String fromAirportCode = fromAirport.text();
          String fromLocalTime = m_dateFormatter.format(date1) + " " + departureInfo.select("span.heading").text() + ":00";

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
          calendar.setTime(date1);
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
              hostAddress
          );

          statusLabel.setForeground(statusLabel.getForeground().equals(Color.BLACK) ? Color.DARK_GRAY : Color.BLACK);

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
      LOG.error("EbookersFlightObtainer: Jsoup parsing failed!", e);
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
    //    connection.addRequestProperty("Cache-Control", "max-age");
    connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36");
    connection.addRequestProperty("Host", url.getHost());
    connection.addRequestProperty("Referer", url.toString());
    connection.addRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
    connection.addRequestProperty("Accept-Language", "sl-SI,sl;q=0.8,en-GB;q=0.6,en;q=0.4,en-US,en;q=0.8");
    return (HttpURLConnection)connection;
  }

  public static void main(String[] args)
  {
    MultiCityFlightObtainer obtainer = new EbookersFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      obtainer.search(null, null, "com", "vce", "bkk", formatter.parse("20.12.2013"), "bkk", "vce", formatter.parse("07.01.2014"), "bkk", "vce", formatter.parse("07.01.2014"), 1);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
