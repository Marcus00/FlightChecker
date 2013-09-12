package si.nerve.flightchecker.components;

import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import si.nerve.flightchecker.data.MultiCityFlightData;
import si.nerve.flightchecker.data.PriceType;

/**
 * @author bratwurzt
 */
public class MultiCityFlightTableModel extends DefaultTableModel
{
  private static final long serialVersionUID = -4509000599023221046L;
  public static final int COL_LEG1_FROM_AIRPORT = 0;
  public static final int COL_LEG1_FROM_TIME = 1;
  public static final int COL_LEG1_TO_AIRPORT = 2;
  public static final int COL_LEG1_TO_TIME = 3;
  public static final int COL_LEG1_DURATION = 4;
  public static final int COL_LEG1_STOPS = 5;
  public static final int COL_LEG2_FROM_AIRPORT = 6;
  public static final int COL_LEG2_FROM_TIME = 7;
  public static final int COL_LEG2_TO_AIRPORT = 8;
  public static final int COL_LEG2_TO_TIME = 9;
  public static final int COL_LEG2_DURATION = 10;
  public static final int COL_LEG2_STOPS = 11;
  public static final int COL_PRICE = 12;
  public static final int COL_MON_ID = 13;
  public static final int COL_HOST_ADDRESS = 14;
  protected static final int COLUMN_COUNT = 15;

  protected List<MultiCityFlightData> m_entityList;
  private boolean m_convertToEuro;
  public MultiCityFlightTableModel(List<MultiCityFlightData> entityList)
  {
    m_entityList = entityList;
    m_convertToEuro = true;
  }

  @Override
  public int getRowCount()
  {
    return m_entityList == null ? 0 : m_entityList.size();
  }

  @Override
  public int getColumnCount()
  {
    return COLUMN_COUNT;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex)
  {
    MultiCityFlightData flightData = m_entityList.get(rowIndex);
    switch (columnIndex)
    {
      case COL_LEG1_FROM_AIRPORT:
        return flightData.getFlightLegs().get(0).getFromAirportCode();

      case COL_LEG1_FROM_TIME:
        return flightData.getFlightLegs().get(0).getDepartureLocalTime();

      case COL_LEG1_TO_AIRPORT:
        return flightData.getFlightLegs().get(0).getToAirportCode();

      case COL_LEG1_TO_TIME:
        return flightData.getFlightLegs().get(0).getArrivalLocalTime();

      case COL_LEG1_DURATION:
        return flightData.getFlightLegs().get(0).getDuration();

      case COL_LEG1_STOPS:
        return flightData.getFlightLegs().get(0).getLayovers();

      case COL_LEG2_FROM_AIRPORT:
        return flightData.getFlightLegs().get(1).getFromAirportCode();

      case COL_LEG2_FROM_TIME:
        return flightData.getFlightLegs().get(1).getDepartureLocalTime();

      case COL_LEG2_TO_AIRPORT:
        return flightData.getFlightLegs().get(1).getToAirportCode();

      case COL_LEG2_TO_TIME:
        return flightData.getFlightLegs().get(1).getArrivalLocalTime();

      case COL_LEG2_DURATION:
        return flightData.getFlightLegs().get(1).getDuration();

      case COL_LEG2_STOPS:
        return flightData.getFlightLegs().get(1).getLayovers();

      case COL_PRICE:
        return flightData.getPriceAmount(m_convertToEuro);

      case COL_MON_ID:
        return m_convertToEuro ? PriceType.EURO.getMonSign() : flightData.getPriceType().getMonSign();

      case COL_HOST_ADDRESS:
        return flightData.getHostAddress();

      default:
        return "damn you, sky!";
    }
  }

  public String getColumnName(int columnIndex)
  {
    switch (columnIndex)
    {
      case COL_LEG1_FROM_AIRPORT:
      case COL_LEG2_FROM_AIRPORT:
        return "Let od";

      case COL_LEG1_FROM_TIME:
      case COL_LEG1_TO_TIME:
      case COL_LEG2_FROM_TIME:
      case COL_LEG2_TO_TIME:
        return "Čas";

      case COL_LEG1_TO_AIRPORT:
      case COL_LEG2_TO_AIRPORT:
        return "Let do";

      case COL_LEG1_DURATION:
      case COL_LEG2_DURATION:
        return "Trajanje";

      case COL_LEG1_STOPS:
      case COL_LEG2_STOPS:
        return "Layover";

      case COL_PRICE:
        return "Cena";
      case COL_MON_ID:
        return "Enota";

      case COL_HOST_ADDRESS:
        return "Vir";

      default:
        return "?";
    }
  }

  public Class<?> getColumnClass(int columnIndex)
  {
    switch (columnIndex)
    {
      case COL_LEG1_FROM_AIRPORT:
      case COL_LEG1_FROM_TIME:
      case COL_LEG1_TO_AIRPORT:
      case COL_LEG1_TO_TIME:
      case COL_LEG1_DURATION:
      case COL_LEG1_STOPS:
      case COL_LEG2_FROM_AIRPORT:
      case COL_LEG2_FROM_TIME:
      case COL_LEG2_TO_AIRPORT:
      case COL_LEG2_TO_TIME:
      case COL_LEG2_DURATION:
      case COL_LEG2_STOPS:
      case COL_HOST_ADDRESS:
        return String.class;

      case COL_PRICE:
      case COL_MON_ID:
        return Integer.class;

      default:
        return String.class;
    }
  }

  public List<MultiCityFlightData> getEntityList()
  {
    return m_entityList;
  }

  public void setEntityList(List<MultiCityFlightData> entityList)
  {
    m_entityList = entityList;
  }

  public void setConvertToEuro(boolean convertToEuro)
  {
    m_convertToEuro = convertToEuro;
  }
}
