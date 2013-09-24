package si.nerve.flightchecker.data;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * @author DusanM
 */
public class ProxyData
{
  private Proxy m_proxy;

  public ProxyData(String ipString, int port)
  {
    m_proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ipString, port));
  }

  public Proxy getProxy()
  {
    return m_proxy;
  }
}
