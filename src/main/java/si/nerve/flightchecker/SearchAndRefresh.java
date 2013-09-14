package si.nerve.flightchecker;

import java.awt.Color;
import java.util.Date;
import javax.swing.JLabel;
import javax.swing.JToggleButton;

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
  private JLabel m_kayakStatusLabel;

  public SearchAndRefresh(
      String root, FlightsGui flightsGui, JLabel kayakStatusLabel, String codeFrom, String codeTo,
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
    m_kayakStatusLabel = kayakStatusLabel;
  }

  @Override
  public void run()
  {
    try
    {
      KayakFlightObtainer kayakFlightObtainer = new KayakFlightObtainer();
      try
      {
        if (m_codeFrom != null && m_codeTo != null && m_codeFrom.length() == 3 && m_codeTo.length() == 3)
        {
          m_kayakStatusLabel.setText(m_codeFrom + "-" + m_codeToStatic + " | " + m_codeFromStatic + "-" + m_codeTo);
          kayakFlightObtainer.search(m_flightsGui, m_kayakStatusLabel, m_root, m_codeFrom, m_codeToStatic, m_fromDate, m_codeFromStatic, m_codeTo, m_toDate);
        }
      }
      catch (InterruptedException e)
      {
        m_kayakStatusLabel.setForeground(Color.DARK_GRAY);
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
              JToggleButton toggleButtonFrom, toggleButtonTo;
              synchronized (m_flightsGui.getAirportGroupBtnMap())
              {
                toggleButtonFrom = m_flightsGui.getAirportGroupBtnMap().get(codeFrom);
                toggleButtonTo = m_flightsGui.getAirportGroupBtnMap().get(codeTo);
              }

              if (toggleButtonFrom.isSelected() && toggleButtonTo.isSelected())
              {
                try
                {
                  Thread.sleep(1300 + (int)(Math.random() * 400));
                }
                catch (InterruptedException e)
                {
                  return;
                }

                kayakFlightObtainer.search(m_flightsGui, m_kayakStatusLabel, m_root, codeFrom, m_codeToStatic, m_fromDate, m_codeFromStatic, codeTo, m_toDate);

                m_kayakStatusLabel.setText(codeFrom + "-" + m_codeToStatic + " | " + m_codeFromStatic + "-" + codeTo);
              }
            }
          }
        }
      }
    }
    catch (InterruptedException e)
    {
      m_kayakStatusLabel.setForeground(Color.DARK_GRAY);
      return;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      m_kayakStatusLabel.setForeground(Color.RED);
    }
    m_kayakStatusLabel.setForeground(Color.GREEN);
  }
}
