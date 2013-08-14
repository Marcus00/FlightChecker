package si.nerve.flightchecker.data;

/**
 * @author bratwurzt
 */
public class FlightLeg
{
  private String m_fromAirportName;
  private String m_fromAirportCode;
  private String m_departureLocalTime;
  private String m_toAirportName;
  private String m_toAirportCode;
  private String m_arrivalLocalTime;
  private String m_duration;
  private String m_layovers;

  public FlightLeg(
      String fromAirportName, String fromAirportCode, String departureLocalTime, String toAirportName, String toAirportCode, String arrivalLocalTime,
      String duration, String layovers)
  {
    m_fromAirportName = fromAirportName;
    m_fromAirportCode = fromAirportCode;
    m_departureLocalTime = departureLocalTime;
    m_toAirportName = toAirportName;
    m_toAirportCode = toAirportCode;
    m_arrivalLocalTime = arrivalLocalTime;
    m_duration = duration;
    m_layovers = layovers;
  }

  public String getFromAirportName()
  {
    return m_fromAirportName;
  }

  public void setFromAirportName(String fromAirportName)
  {
    m_fromAirportName = fromAirportName;
  }

  public String getFromAirportCode()
  {
    return m_fromAirportCode;
  }

  public void setFromAirportCode(String fromAirportCode)
  {
    m_fromAirportCode = fromAirportCode;
  }

  public String getDepartureLocalTime()
  {
    return m_departureLocalTime;
  }

  public void setDepartureLocalTime(String departureLocalTime)
  {
    m_departureLocalTime = departureLocalTime;
  }

  public String getToAirportName()
  {
    return m_toAirportName;
  }

  public void setToAirportName(String toAirportName)
  {
    m_toAirportName = toAirportName;
  }

  public String getToAirportCode()
  {
    return m_toAirportCode;
  }

  public void setToAirportCode(String toAirportCode)
  {
    m_toAirportCode = toAirportCode;
  }

  public String getArrivalLocalTime()
  {
    return m_arrivalLocalTime;
  }

  public void setArrivalLocalTime(String arrivalLocalTime)
  {
    m_arrivalLocalTime = arrivalLocalTime;
  }

  public String getDuration()
  {
    return m_duration;
  }

  public void setDuration(String duration)
  {
    m_duration = duration;
  }

  public String getLayovers()
  {
    return m_layovers;
  }

  public void setLayovers(String layovers)
  {
    m_layovers = layovers;
  }
}
