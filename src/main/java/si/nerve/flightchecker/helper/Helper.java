package si.nerve.flightchecker.helper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPInputStream;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import si.nerve.flightchecker.data.ProxyData;
import si.nerve.flightchecker.pages.MultiCityFlightObtainer;
import si.nerve.flightchecker.pages.obtainers.EdreamsFlightObtainer;
import si.nerve.flightchecker.pages.obtainers.KayakFlightObtainer;

/**
 * @author bratwurzt
 */
public class Helper
{
  private static Map<String, ConcurrentLinkedQueue<ProxyData>> m_proxyQueueMap = new HashMap<String, ConcurrentLinkedQueue<ProxyData>>();
  private static Map<String, ConcurrentLinkedQueue<ProxyData>> m_bannedProxyQueueMap = new HashMap<String, ConcurrentLinkedQueue<ProxyData>>();

  public static String readResponse(InputStream ins, URLConnection connection) throws IOException
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
    return bos.toString(getCharSetFromConnection(connection));
  }

  public static int getSelectedButtonText(ButtonGroup buttonGroup)
  {
    for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements(); )
    {
      AbstractButton button = buttons.nextElement();

      if (button.isSelected())
      {
        String text = button.getText();
        if ("Normal".equals(text))
        {
          return 0;
        }
        else if ("Mix".equals(text))
        {
          return 1;
        }
        else if ("1x".equals(text))
        {
          return 2;
        }
        else if ("3x".equals(text))// 3x
        {
          return 3;
        }
      }
    }

    return -1;
  }

  public synchronized static Proxy peekFreeProxy(String sourceObtainer, boolean changeProxy)
  {
    if (!m_proxyQueueMap.containsKey(sourceObtainer))
    {
      final ConcurrentLinkedQueue<ProxyData> value = readProxiesFromTheWebs(null);
      m_proxyQueueMap.put(sourceObtainer, value);
    }

    if (!m_bannedProxyQueueMap.containsKey(sourceObtainer))
    {
      m_bannedProxyQueueMap.put(sourceObtainer, new ConcurrentLinkedQueue<ProxyData>());
    }

    if (changeProxy)
    {
      ConcurrentLinkedQueue<ProxyData> proxyQueue = m_proxyQueueMap.get(sourceObtainer);
      m_bannedProxyQueueMap.get(sourceObtainer).add(proxyQueue.poll());
      if (proxyQueue.size() == 0)
      {
        proxyQueue.addAll(readProxiesFromTheWebs(sourceObtainer));
      }
    }

    return m_proxyQueueMap.get(sourceObtainer).peek().getProxy();
  }

  private static ConcurrentLinkedQueue<ProxyData> readProxiesFromTheWebs(String sourceObtainer)
  {
    String urlString = "http://www.cool-proxy.net/proxies/http_proxy_list/country_code:/port:/anonymous:1/sort:score/direction:desc";
    try
    {
      ConcurrentLinkedQueue<ProxyData> returnQueue = new ConcurrentLinkedQueue<ProxyData>();
      URL url = new URL(urlString);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      String response = Helper.readResponse(connection.getInputStream(), connection);
      Document parse = Jsoup.parse(response);
      for (Element element : parse.select("table > tbody > tr"))
      {
        Elements script = element.select("script");
        String portNum = element.select("td:has(script) + td").text();
        if (script.size() > 0)
        {
          try
          {
            String javascript = script.toString();
            String startString = "Base64.decode(\"";
            int beginIndex = javascript.indexOf(startString) + startString.length();
            String ipString = new String(Base64.decode(javascript.substring(beginIndex, javascript.indexOf("\")", beginIndex))));
            ProxyData proxyData = new ProxyData(ipString, Integer.parseInt(portNum));
            if (sourceObtainer == null || !m_bannedProxyQueueMap.get(sourceObtainer).contains(proxyData))
            {
              returnQueue.add(proxyData);
            }
          }
          catch (Exception ignored)
          {
          }
        }
      }

      final String hostAddress = "http://hidemyass.com";
      urlString = hostAddress + "/proxy-list/";

      url = new URL(urlString);
      connection = createHttpConnection(url);
      //InputStream ins = connection.getInputStream();
      //encoding = connection.getHeaderField("Content-Encoding");
      //if (encoding.equals("gzip"))
      //{
      //  ins = new GZIPInputStream(ins);
      //}
      //response = readResponse(ins, connection);
      StringBuilder sbuf = new StringBuilder();

      List<String> cookieList = connection.getHeaderFields().get("Set-Cookie");
      String cookies = null;
      if (cookieList != null)
      {
        for (String cookie : cookieList)
        {
          if (sbuf.length() > 0)
          {
            sbuf.append(";");
          }
          sbuf.append(cookie);
        }
        cookies = sbuf.toString();
        cookies = cookies.substring(0, cookies.indexOf("; path=")).trim();
      }
      String postString = "c%5B%5D=United+States&c%5B%5D=Indonesia&c%5B%5D=Venezuela&c%5B%5D=Brazil&c%5B%5D=Thailand&c%5B%5D=Poland&c%5B%5D=Russian+Federation&c%5B%5D=Bulgaria&c%5B%5D=Iran&c%5B%5D=India&c%5B%5D=Colombia&c%5B%5D=Moldova%2C+Republic+of&c%5B%5D=Ecuador&c%5B%5D=Ukraine&c%5B%5D=Czech+Republic&c%5B%5D=United+Kingdom&c%5B%5D=Canada&c%5B%5D=Turkey&c%5B%5D=Taiwan&c%5B%5D=Peru&c%5B%5D=Hong+Kong&c%5B%5D=Italy&c%5B%5D=Sweden&c%5B%5D=Netherlands&c%5B%5D=Argentina&c%5B%5D=Taiwan&c%5B%5D=Serbia&c%5B%5D=Pakistan&c%5B%5D=Germany&c%5B%5D=United+Arab+Emirates&c%5B%5D=Bangladesh&c%5B%5D=Hungary&c%5B%5D=Chile&c%5B%5D=Egypt&c%5B%5D=Nigeria&c%5B%5D=South+Africa&c%5B%5D=Cambodia&c%5B%5D=Iraq&c%5B%5D=Albania&c%5B%5D=Bosnia+and+Herzegovina&c%5B%5D=Latvia&c%5B%5D=Kazakhstan&c%5B%5D=Ghana&c%5B%5D=Korea%2C+Republic+of&c%5B%5D=Kenya&c%5B%5D=Romania&c%5B%5D=Viet+Nam&c%5B%5D=Japan&c%5B%5D=Mexico&c%5B%5D=Austria&c%5B%5D=Zaire&c%5B%5D=Gabon&c%5B%5D=Europe&c%5B%5D=El+Salvador&c%5B%5D=Georgia&c%5B%5D=Belarus&c%5B%5D=Sudan&c%5B%5D=Cote+D%27Ivoire&c%5B%5D=Bahrain&c%5B%5D=Costa+Rica&c%5B%5D=Macedonia&c%5B%5D=Honduras&c%5B%5D=Yemen&c%5B%5D=France&c%5B%5D=Malaysia&c%5B%5D=Sri+Lanka&c%5B%5D=Spain&c%5B%5D=Dominican+Republic&c%5B%5D=Slovakia&c%5B%5D=Nepal&c%5B%5D=Philippines&c%5B%5D=Israel&c%5B%5D=Australia&c%5B%5D=Singapore&c%5B%5D=Nicaragua&c%5B%5D=Croatia&c%5B%5D=Denmark&c%5B%5D=Saudi+Arabia&c%5B%5D=Zimbabwe&c%5B%5D=Finland&p=&pr%5B%5D=0&a%5B%5D=0&a%5B%5D=1&a%5B%5D=2&a%5B%5D=3&a%5B%5D=4&pl=on&sp%5B%5D=2&sp%5B%5D=3&ct%5B%5D=2&ct%5B%5D=3&s=1&o=0&pp=3&sortBy=response_time";
      connection = createHttpConnection(url);
      connection.addRequestProperty("Cookie", cookies);
      connection.addRequestProperty("Cache-Control", "max-age=0");
      connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      connection.addRequestProperty("Origin", hostAddress);
      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestMethod("POST");
      final String postStringLength = String.valueOf(postString.length());
      connection.addRequestProperty("Content-Length", postStringLength);
      DataOutputStream out = new DataOutputStream(connection.getOutputStream());
      out.writeBytes(postString);
      out.flush();
      out.close();

      String location = connection.getHeaderField("Location");
      url = new URL(hostAddress + location);
      connection = createHttpConnection(url);
      connection.addRequestProperty("Cache-Control", "max-age=0");
      connection.addRequestProperty("Cookie", cookies);
      connection.addRequestProperty("Referer", urlString);
      InputStream ins = connection.getInputStream();
      String encoding = connection.getHeaderField("Content-Encoding");
      if (encoding != null && encoding.equals("gzip"))
      {
        ins = new GZIPInputStream(ins);
      }

      response = Helper.readResponse(ins, connection);
      parse = Jsoup.parse(response);
      StringBuilder builder;
      for (Element element : parse.select("table#listtable > tbody >tr"))
      {
        builder = new StringBuilder();
        List<Node> ipPartsObscured = element.select("td").get(1).child(0).childNodes();
        Node cssClassesFirst = ipPartsObscured.get(0);
        String validCssClassesDataNode = ((Element)cssClassesFirst).dataNodes().get(0).getWholeData();
        Map<String, Boolean> map = new HashMap<String, Boolean>();
        for (String css : validCssClassesDataNode.split("\n"))
        {
          if (css.length() > 0)
          {
            map.put(css.substring(1, css.indexOf("{")), !css.contains("display:none"));
          }
        }

        for (Node ipPart : ipPartsObscured)
        {
          if (!ipPart.equals(cssClassesFirst))
          {
            if (ipPart instanceof TextNode)
            {
              builder.append(((TextNode)ipPart).text());
            }
            else
            {
              String ipPartString = ipPart.toString();
              boolean visible = false;
              for (String cssClass : map.keySet())
              {
                if (!ipPartString.contains("display:none"))
                {
                  if (ipPartString.contains(cssClass) && map.get(cssClass) || ipPartString.contains("inline\"") || ipPart.attr("class").matches("\\d+"))
                  {
                    //builder.append(ipPart);
                    visible = true;
                    break;
                  }
                }
              }
              if (visible)
              {
                builder.append(((Element)ipPart).text());
              }
            }
          }
        }

        String ipAddress = builder.toString();
        String portNum = element.select("td").get(2).text();
        ProxyData proxyData = new ProxyData(ipAddress, Integer.parseInt(portNum));
        if (sourceObtainer == null || !m_bannedProxyQueueMap.get(sourceObtainer).contains(proxyData))
        {
          returnQueue.add(proxyData);
        }
      }

      return returnQueue;
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  private static String getCharSetFromConnection(URLConnection connection)
  {
    String contentType = connection.getHeaderField("Content-Type");

    String charset = null;
    if (contentType != null)
    {
      String[] values = contentType.split(";");

      for (String value : values)
      {
        value = value.trim();

        if (value.toLowerCase().startsWith("charset="))
        {
          charset = value.substring("charset=".length());
        }
      }
    }

    if (charset == null)
    {
      charset = "UTF-8";
    }
    return charset;
  }

  public static HttpURLConnection createHttpConnection(URL url) throws IOException
  {
    URLConnection connection = url.openConnection();
    connection.addRequestProperty("Connection", "keep-alive");
    //connection.addRequestProperty("Cache-Control", "max-age");
    connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.76 Safari/537.36");
    connection.addRequestProperty("Host", url.getHost());
    connection.addRequestProperty("Referer", url.toString());
    connection.addRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
    connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
    return (HttpURLConnection)connection;
  }

  public static HttpURLConnection createHttpProxyConnection(String address, Proxy proxy) throws Exception
  {
    URL url = new URL(address);
    HttpURLConnection conn = proxy == null ? (HttpURLConnection)url.openConnection() : (HttpURLConnection)url.openConnection(proxy);
    conn.addRequestProperty("Connection", "keep-alive");
    conn.addRequestProperty("Cache-Control", "max-age=0");
    conn.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.76 Safari/537.36");
    conn.addRequestProperty("Host", url.getHost());
    conn.addRequestProperty("Referer", url.toString());
    conn.addRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
    conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
    return conn;
  }

  public static int getSleepMillis(MultiCityFlightObtainer obtainer)
  {
    if (obtainer instanceof KayakFlightObtainer)
    {
      return 700;
    }
    else if (obtainer instanceof EdreamsFlightObtainer)
    {
      return 300;
    }
    return 100;
  }

  public static void main(String[] args)
  {
    Helper.readProxiesFromTheWebs(null);
  }
}
