package si.nerve.flightchecker.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import si.nerve.flightchecker.data.ProxyData;
import si.nerve.flightchecker.pages.obtainers.EbookersFlightObtainer;
import si.nerve.flightchecker.pages.obtainers.EdreamsFlightObtainer;
import si.nerve.flightchecker.pages.obtainers.ExpediaFlightObtainer;
import si.nerve.flightchecker.pages.obtainers.KayakFlightObtainer;

/**
 * @author bratwurzt
 */
public class Helper
{
  private static Map<Class, ConcurrentLinkedQueue<ProxyData>> m_proxyQueueMap = new HashMap<Class, ConcurrentLinkedQueue<ProxyData>>();
  private static Map<Class, ConcurrentLinkedQueue<ProxyData>> m_bannedProxyQueueMap = new HashMap<Class, ConcurrentLinkedQueue<ProxyData>>();

  public static String readResponse(InputStream ins, String charset) throws IOException
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
    return bos.toString(charset);
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

  public synchronized static Proxy peekFreeProxy(Class sourceClass, boolean changeProxy)
  {
    if (m_proxyQueueMap.size() == 0)
    {
      final ConcurrentLinkedQueue<ProxyData> value = readProxiesFromTheWebs(null);
      m_proxyQueueMap.put(EdreamsFlightObtainer.class, value);
      m_proxyQueueMap.put(EbookersFlightObtainer.class, value);
      m_proxyQueueMap.put(ExpediaFlightObtainer.class, value);
      m_proxyQueueMap.put(KayakFlightObtainer.class, value);
    }

    if (m_proxyQueueMap.size() == 0)
    {
      m_proxyQueueMap.put(EdreamsFlightObtainer.class, new ConcurrentLinkedQueue<ProxyData>());
      m_proxyQueueMap.put(EbookersFlightObtainer.class, new ConcurrentLinkedQueue<ProxyData>());
      m_proxyQueueMap.put(ExpediaFlightObtainer.class, new ConcurrentLinkedQueue<ProxyData>());
      m_proxyQueueMap.put(KayakFlightObtainer.class, new ConcurrentLinkedQueue<ProxyData>());
    }

    if (changeProxy)
    {
      ConcurrentLinkedQueue<ProxyData> proxyQueue = m_proxyQueueMap.get(sourceClass);
      m_bannedProxyQueueMap.get(sourceClass).add(proxyQueue.poll());
      if (proxyQueue.size() == 0)
      {
        proxyQueue.addAll(readProxiesFromTheWebs(sourceClass));
      }
    }

    return m_proxyQueueMap.get(sourceClass).peek().getProxy();
  }

  private static ConcurrentLinkedQueue<ProxyData> readProxiesFromTheWebs(Class sourceClass)
  {
    String urlString = "http://www.cool-proxy.net/proxies/http_proxy_list/country_code:/port:/anonymous:1/sort:score/direction:desc";
    try
    {
      ConcurrentLinkedQueue<ProxyData> returnQueue = new ConcurrentLinkedQueue<ProxyData>();
      String charset = "UTF-8";

      URL url = new URL(urlString);
      URLConnection urlConnection = url.openConnection();
      String response = Helper.readResponse(urlConnection.getInputStream(), charset);
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
            if (sourceClass == null || !m_bannedProxyQueueMap.get(sourceClass).contains(proxyData))
            {
              returnQueue.add(proxyData);
            }
          }
          catch (Exception ignored)
          {
          }
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
}
