package si.nerve.flightchecker;

import java.util.Date;

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
      try
      {
        kayakFlightObtainer.search(m_flightsGui, m_root, m_codeFrom, m_codeToStatic, m_fromDate, m_codeFromStatic, m_codeTo, m_toDate);
      }
      catch (InterruptedException e)
      {
        return;
      }

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
                Thread.sleep(1300 + (int)(Math.random() * 400));
              }
              catch (InterruptedException e)
              {
                return;
              }
              kayakFlightObtainer.search(m_flightsGui, m_root, codeFrom, m_codeToStatic, m_fromDate, m_codeFromStatic, codeTo, m_toDate);
              synchronized (m_flightsGui.getFlightQueue())
              {
                String text = "Prikaz: " + String.valueOf(m_flightsGui.getFlightQueue().size())
                    + " cen. Trenutno iščem: " + codeFrom + "-" + m_codeToStatic + " | " + m_codeFromStatic + "-" + codeTo;
                if (!text.equals(m_flightsGui.getStatusLabel().getText()))
                {
                  m_flightsGui.getStatusLabel().setText(text);
                }
              }
            }
          }
        }
      }
    }
    catch (InterruptedException ignored)
    {
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
