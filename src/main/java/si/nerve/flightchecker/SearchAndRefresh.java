package si.nerve.flightchecker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

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
  private String m_root;
  private boolean m_combined;
  private String[] m_codes;


  public SearchAndRefresh(String root, FlightsGui flightsGui, String codeFrom, String codeTo,
                          String codeToStatic, String codeFromStatic, Date fromDate, Date toDate, boolean combined, String[] codes)
  {
    m_root = root;
    m_flightsGui = flightsGui;
    m_codeFrom = codeFrom;
    m_codeTo = codeTo;
    m_codeFromStatic = codeFromStatic;
    m_codeToStatic = codeToStatic;
    m_fromDate = fromDate;
    m_toDate = toDate;
    m_combined = combined;
    m_codes = codes;
  }

  @Override
  public void run()
  {
    try
    {
      KayakFlightObtainer kayakFlightObtainer = new KayakFlightObtainer();
      kayakFlightObtainer.search(m_flightsGui, m_root, m_codeFrom, m_codeToStatic, m_fromDate, m_codeFromStatic, m_codeTo, m_toDate);

      if (m_combined)
      {
        for (final String codeFrom : m_codes)
        {
          for (final String codeTo : m_codes)
          {
            if (!codeFrom.equals(m_codeFrom) && !codeTo.equals(m_codeTo))
            {
              try
              {
                Thread.sleep(500 + (int)(Math.random() * 100));
              }
              catch (InterruptedException e)
              {
                e.printStackTrace();
              }
              kayakFlightObtainer.search(m_flightsGui, m_root, codeFrom, m_codeToStatic, m_fromDate, m_codeFromStatic, codeTo, m_toDate);
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
