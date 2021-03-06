package si.nerve.flightchecker;

import java.awt.Color;
import java.util.Date;
import java.util.List;
import javax.swing.JProgressBar;
import javax.swing.JToggleButton;

import si.nerve.flightchecker.helper.Helper;
import si.nerve.flightchecker.pages.MultiCityFlightObtainer;

/**
 * @author bratwurzt
 */
public class SearchAndRefreshRunnable implements Runnable
{
  private FlightsGui m_flightsGui;
  private String m_codeFrom1;
  private String m_codeTo2;
  private String m_codeFrom2;
  private String m_codeTo1;
  private Date m_date1;
  private Date m_date2;
  private Date m_date3;
  private String m_root;
  private int m_selectedRadio;
  private String[] m_codes;
  private Integer m_numOfPersons, m_numOfDays;
  private MultiCityFlightObtainer m_multiCityFlightObtainer;
  private List<String[]> m_a1xCombinations;
  private List<String[]> m_a3xCombinations;
  private JProgressBar m_progressBar;

  public SearchAndRefreshRunnable(
      MultiCityFlightObtainer multiCityFlightObtainer, String root, FlightsGui flightsGui, JProgressBar progressBar, String codeFrom1,
      String codeTo2, String codeTo1, String codeFrom2, Date date1, Date date2, Date from1xDate, Integer numOfPersons, Integer numOfDays, int selectedRadio,
      String[] codes, List<String[]> a1xCombinations, List<String[]> a3xCombinations)
  {
    m_root = root;
    m_flightsGui = flightsGui;
    m_codeFrom1 = codeFrom1;
    m_codeTo2 = codeTo2;
    m_codeFrom2 = codeFrom2;
    m_codeTo1 = codeTo1;
    m_date1 = date1;
    m_date2 = date2;
    m_date3 = from1xDate;
    m_numOfPersons = numOfPersons;
    m_numOfDays = numOfDays;
    m_selectedRadio = selectedRadio;
    m_codes = codes;
    m_progressBar = progressBar;
    m_multiCityFlightObtainer = multiCityFlightObtainer;
    m_a1xCombinations = a1xCombinations;
    m_a3xCombinations = a3xCombinations;
    m_progressBar.setMinimum(0);
    m_progressBar.setMaximum(getMaximum());
  }

  private int getMaximum()
  {
    switch (m_selectedRadio)
    {
      case 0:
        return 1;
      case 1:
        return m_codes.length * m_codes.length;
      case 2:
        return m_a1xCombinations.size();
      case 3:
        return m_a3xCombinations.size();
      case 4:
        return 1;
      default:
        return 0;
    }
  }

  @Override
  public void run()
  {
    try
    {
      m_progressBar.setForeground(Color.BLACK);
      m_progressBar.setValue(0);
      switch (m_selectedRadio)
      {
        case 0:
        case 1:
          if (m_codeFrom1 != null && m_codeTo2 != null && m_codeFrom1.length() == 3 && m_codeTo2.length() == 3)
          {
            m_progressBar.setString(m_codeFrom1 + "-" + m_codeTo1 + " | " + m_codeFrom2 + "-" + m_codeTo2);
            m_multiCityFlightObtainer.search(m_flightsGui, m_root, m_codeFrom1, m_codeTo1, m_date1, m_codeFrom2, m_codeTo2, m_date2, null, null, null, m_numOfPersons, false);
            m_progressBar.setValue(1);
          }

          if (m_selectedRadio == 1) //todo randomize combination call order
          {
            for (final String codeFrom : m_codes)
            {
              for (final String codeTo : m_codes)
              {
                if (!codeFrom.equals(m_codeFrom1) && !codeTo.equals(m_codeTo2))
                {
                  JToggleButton toggleButtonFrom, toggleButtonTo;
                  synchronized (m_flightsGui.getAirportGroupBtnMap())
                  {
                    toggleButtonFrom = m_flightsGui.getAirportGroupBtnMap().get(codeFrom);
                    toggleButtonTo = m_flightsGui.getAirportGroupBtnMap().get(codeTo);
                  }

                  if (toggleButtonFrom.isSelected() && toggleButtonTo.isSelected())
                  {
                    Thread.sleep(Helper.getSleepMillis(m_multiCityFlightObtainer) + (int)(Math.random() * 100));

                    m_multiCityFlightObtainer.search(m_flightsGui, m_root, codeFrom, m_codeTo1, m_date1, m_codeFrom2, codeTo, m_date2, null, null, null, m_numOfPersons, false);

                    m_progressBar.setString(codeFrom + "-" + m_codeTo1 + " | " + m_codeFrom2 + "-" + codeTo);
                  }
                  m_progressBar.setValue(m_progressBar.getValue() + 1);
                }
              }
            }
          }
          break;
        case 2: // 1x
          for (final String[] codePairs : m_a1xCombinations)
          {
            String from3 = codePairs[0].toUpperCase();
            String to3 = codePairs[1].toUpperCase();
            if (m_codeFrom1 != null && m_codeTo2 != null && from3 != null && to3 != null
                && m_codeFrom1.length() == 3 && m_codeTo2.length() == 3 && from3.length() == 3 && to3.length() == 3)
            {
              m_progressBar.setString(from3 + "-" + to3 + " | " + m_codeFrom1 + "-" + m_codeTo1 + " | " + m_codeFrom2 + "-" + m_codeTo2);

              if (Thread.currentThread().isInterrupted())
              {
                m_progressBar.setForeground(Color.BLUE);
                return;
              }

              m_multiCityFlightObtainer.search(
                  m_flightsGui,
                  m_root,
                  from3,
                  to3,
                  m_date3,
                  m_codeFrom1,
                  m_codeTo1,
                  m_date1,
                  m_codeFrom2,
                  m_codeTo2,
                  m_date2,
                  m_numOfPersons,
                  false
              );
              m_progressBar.setValue(m_progressBar.getValue() + 1);
            }
          }
          break;
        case 3: // 3x
          for (final String[] codePairs : m_a3xCombinations)
          {
            String from3 = codePairs[0].toUpperCase();
            String to3 = codePairs[1].toUpperCase();
            if (m_codeFrom1 != null && m_codeTo2 != null && from3 != null && to3 != null
                && m_codeFrom1.length() == 3 && m_codeTo2.length() == 3 && from3.length() == 3 && to3.length() == 3)
            {
              m_progressBar.setString(from3 + "-" + to3 + " | " + m_codeFrom1 + "-" + m_codeTo1 + " | " + m_codeFrom2 + "-" + m_codeTo2);
              if (Thread.currentThread().isInterrupted())
              {
                m_progressBar.setForeground(Color.BLUE);
                return;
              }
              m_multiCityFlightObtainer.search(
                  m_flightsGui,
                  m_root,
                  m_codeFrom1,
                  m_codeTo1,
                  m_date1,
                  m_codeFrom2,
                  m_codeTo2,
                  m_date2,
                  from3,
                  to3,
                  m_date3,
                  m_numOfPersons,
                  false
              );
              m_progressBar.setValue(m_progressBar.getValue() + 1);
            }
          }
          break;
      }
      m_progressBar.setForeground(Color.GREEN);
    }
    catch (InterruptedException e)
    {
      m_progressBar.setForeground(Color.BLUE);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      m_progressBar.setForeground(Color.RED);
    }
  }
}
