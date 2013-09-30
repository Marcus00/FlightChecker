package si.nerve.flightchecker.pages;

/**
 * Created: 11.8.13 20:16
 */
public class KayakResult
{
  Long m_time;
  String m_response;

  public KayakResult(Long time, String response)
  {
    m_time = time;
    m_response = response;
  }

  public Long getTime()
  {
    return m_time;
  }

  public String getResponse()
  {
    return m_response;
  }
}
