package si.nerve.flightchecker.data;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * @author bratwurzt
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

  @Override
  public int hashCode()
  {
    return m_proxy.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (!(obj instanceof ProxyData))
    {
      return false;
    }

    ProxyData other = (ProxyData) obj;
    return m_proxy.address().equals(other.getProxy().address());
  }
}
