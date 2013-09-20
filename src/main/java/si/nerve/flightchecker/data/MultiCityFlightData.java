package si.nerve.flightchecker.data;

import java.util.LinkedList;

/**
 * @author bratwurzt
 */
public class MultiCityFlightData
{
  private int m_index;
  private String m_dataResultId;
  private int m_priceAmount;
  private PriceType m_priceType;
  private LinkedList<FlightLeg> m_flightLegs;
  private String m_seatsPromo;
  private String m_hostAddress;

  public MultiCityFlightData(int index, String dataResultId, int priceAmount, PriceType priceType,
                             LinkedList<FlightLeg> flightLegs, String seatsPromo, String hostAddress)
  {
    m_index = index;
    m_dataResultId = dataResultId;
    m_priceAmount = priceAmount;
    m_priceType = priceType;
    m_flightLegs = flightLegs;
    m_seatsPromo = seatsPromo;
    m_hostAddress = hostAddress;
  }

  public int getIndex()
  {
    return m_index;
  }

  public String getDataResultId()
  {
    return m_dataResultId;
  }

  public int getPriceAmount(boolean convertToEuro)
  {
    if (convertToEuro)
    {
      switch (m_priceType)
      {
        case DOLLAR:
          return (int) m_priceType.getConversionToEuro(m_priceAmount);
        case EURO:
          return m_priceAmount;
        case POUND:
          return (int) m_priceType.getConversionToEuro(m_priceAmount);
      }
    }
    return m_priceAmount;
  }

  public PriceType getPriceType()
  {
    return m_priceType;
  }

  public LinkedList<FlightLeg> getFlightLegs()
  {
    return m_flightLegs;
  }

  public String getSeatsPromo()
  {
    return m_seatsPromo;
  }

  public String getAddress()
  {
    return m_hostAddress;
  }

  public String getPopupLinkName()
  {
    StringBuilder builder = new StringBuilder();
    for (FlightLeg leg : getFlightLegs())
    {
      if (builder.length() > 0)
      {
        builder.append(" | ");
      }
      else
      {
        builder.append(getPriceAmount(true)).append(" ").append(getPriceType().getMonSign()).append("   ");
      }
      builder.append(leg.getFromAirportCode()).append(" - ").append(leg.getToAirportCode());
    }
    return builder.toString();
  }

  @Override
  public int hashCode()
  {
    FlightLeg flightLeg1 = getFlightLegs().get(0);
    FlightLeg flightLeg2 = getFlightLegs().get(1);
    return (flightLeg1.getFromAirportCode()
        + flightLeg1.getDepartureLocalTime()
        + flightLeg1.getToAirportCode()
        + flightLeg1.getArrivalLocalTime()
        + flightLeg1.getDuration()
        + flightLeg1.getLayovers()
        + flightLeg2.getFromAirportCode().hashCode()
        + flightLeg2.getDepartureLocalTime()
        + flightLeg2.getToAirportCode()
        + flightLeg2.getArrivalLocalTime()
        + flightLeg2.getDuration()
        + flightLeg2.getLayovers()
        + getAddress()).hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (!(obj instanceof MultiCityFlightData))
    {
      return false;
    }

    MultiCityFlightData other = (MultiCityFlightData) obj;
    FlightLeg otherFlightLeg1 = other.getFlightLegs().get(0);
    FlightLeg otherFlightLeg2 = other.getFlightLegs().get(1);
    FlightLeg flightLeg1 = getFlightLegs().get(0);
    FlightLeg flightLeg2 = getFlightLegs().get(1);
    return flightLeg1.getFromAirportCode().equals(otherFlightLeg1.getFromAirportCode())
        && flightLeg1.getDepartureLocalTime().equals(otherFlightLeg1.getDepartureLocalTime())
        && flightLeg1.getToAirportCode().equals(otherFlightLeg1.getToAirportCode())
        && flightLeg1.getArrivalLocalTime().equals(otherFlightLeg1.getArrivalLocalTime())
        && flightLeg1.getDuration().equals(otherFlightLeg1.getDuration())
        && flightLeg1.getLayovers().equals(otherFlightLeg1.getLayovers())
        && flightLeg2.getFromAirportCode().equals(otherFlightLeg2.getFromAirportCode())
        && flightLeg2.getDepartureLocalTime().equals(otherFlightLeg2.getDepartureLocalTime())
        && flightLeg2.getToAirportCode().equals(otherFlightLeg2.getToAirportCode())
        && flightLeg2.getArrivalLocalTime().equals(otherFlightLeg2.getArrivalLocalTime())
        && flightLeg2.getDuration().equals(otherFlightLeg2.getDuration())
        && flightLeg2.getLayovers().equals(otherFlightLeg2.getLayovers())
        && getAddress().equals(other.getAddress());
  }
}
