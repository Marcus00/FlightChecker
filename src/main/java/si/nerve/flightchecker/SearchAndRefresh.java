package si.nerve.flightchecker;

import java.awt.Cursor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import si.nerve.flightchecker.components.MultiCityFlightTableModel;
import si.nerve.flightchecker.data.MultiCityFlightData;

/**
 * @author bratwurzt
 */
public class SearchAndRefresh implements Runnable
{
  private FlightsGui m_flightsGui;
  private String m_codeFrom;
  private String m_codeTo;
  private String m_codeFromStatic;
  private String m_codeToStatic;
  private Date m_fromDate;
  private Date m_toDate;

  public SearchAndRefresh(FlightsGui flightsGui, String codeFrom, String codeTo, String codeToStatic, String codeFromStatic, Date fromDate, Date toDate)
  {
    m_flightsGui = flightsGui;
    m_codeFrom = codeFrom;
    m_codeTo = codeTo;
    m_codeFromStatic = codeFromStatic;
    m_codeToStatic = codeToStatic;
    m_fromDate = fromDate;
    m_toDate = toDate;
  }

  @Override
  public void run()
  {
    try
    {
      m_flightsGui.setCursor(new Cursor(Cursor.WAIT_CURSOR));
      m_flightsGui.getFlightSet().addAll(
          m_flightsGui.getCityFlightObtainer().get(
              "de",
              m_codeFrom,
              m_codeToStatic,
              m_fromDate,
              m_codeFromStatic,
              m_codeTo,
              m_toDate)
      );
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    finally
    {
      ArrayList<MultiCityFlightData> multiCityFlightDatas = new ArrayList<MultiCityFlightData>();
      multiCityFlightDatas.addAll(m_flightsGui.getFlightSet());
      MultiCityFlightTableModel model = (MultiCityFlightTableModel)m_flightsGui.getMainTable().getModel();
      model.setEntityList(multiCityFlightDatas);
      //MultiCityFlightTableModel model = new MultiCityFlightTableModel(multiCityFlightDatas);
      m_flightsGui.getMainTable().setModel(model);
      m_flightsGui.getSorter().setModel(model);
      m_flightsGui.getMainTable().setRowSorter(m_flightsGui.getSorter());
      m_flightsGui.getSorter().sort();
      m_flightsGui.getSorter().toggleSortOrder(MultiCityFlightTableModel.COL_PRICE);
      //m_flightsGui.pack();
      m_flightsGui.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
  }
}
