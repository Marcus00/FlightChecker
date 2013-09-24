package si.nerve.flightchecker.helper;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import si.nerve.flightchecker.data.ProxyData;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author bratwurzt
 */
public class Helper
{
  private static Proxy c_currentProxy = null;
  private static ConcurrentLinkedQueue<ProxyData> m_proxyQueue = null;

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

  public synchronized static Proxy pullFreeProxy(boolean changeProxy)
  {
    if (m_proxyQueue == null)
    {
      m_proxyQueue = readProxiesFromTheWebs();
    }

    if (c_currentProxy == null || changeProxy)
    {
      c_currentProxy = m_proxyQueue.poll().getProxy();
    }
    return c_currentProxy;
  }

  private static ConcurrentLinkedQueue<ProxyData> readProxiesFromTheWebs()
  {
    String urlString = "http://www.cool-proxy.net/proxies/http_proxy_list/sort:score/direction:desc";
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
            returnQueue.add(new ProxyData(ipString, Integer.parseInt(portNum)));
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
