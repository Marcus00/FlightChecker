package si.nerve.flightchecker.data;

/**
 * @author bratwurzt
 */
public class AirportData
{
  private int m_id;
  private String m_name;
  private String m_city;
  private String m_country;
  private String m_iataCode;
  private String m_icaoCode;
  private float m_altitude;
  private String m_dst;

  public AirportData(int id, String name, String city, String country, String iataCode, String icaoCode, float altitude, String dst)
  {
    m_id = id;
    m_name = name;
    m_city = city;
    m_country = country;
    m_iataCode = iataCode;
    m_icaoCode = icaoCode;
    m_altitude = altitude;
    m_dst = dst;
  }

  public int getId()
  {
    return m_id;
  }

  public String getName()
  {
    return m_name;
  }

  public String getCity()
  {
    return m_city;
  }

  public String getCountry()
  {
    return m_country;
  }

  public String getIataCode()
  {
    return m_iataCode;
  }

  public String getIcaoCode()
  {
    return m_icaoCode;
  }

  public float getAltitude()
  {
    return m_altitude;
  }

  public String getDst()
  {
    return m_dst;
  }
}
