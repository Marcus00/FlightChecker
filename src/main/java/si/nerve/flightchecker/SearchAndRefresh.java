package si.nerve.flightchecker;

import si.nerve.flightchecker.pages.EbookersFlightObtainer;
import si.nerve.flightchecker.pages.ExpediaFlightObtainer;
import si.nerve.flightchecker.pages.KayakFlightObtainer;
import si.nerve.flightchecker.pages.MultiCityFlightObtainer;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author bratwurzt
 */
public class SearchAndRefresh implements Runnable
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
  private JLabel m_statusLabel;
  private Integer m_numOfPersons;
  private MultiCityFlightObtainer m_multiCityFlightObtainer;
  private List<String[]> m_a1xCombinations;
  private List<String[]> m_a3xCombinations;

  public SearchAndRefresh(MultiCityFlightObtainer multiCityFlightObtainer, String root, FlightsGui flightsGui, JLabel statusLabel, String codeFrom1,
      String codeTo2, String codeTo1, String codeFrom2, Date date1, Date date2, Date from1xDate, Integer numOfPersons, int selectedRadio,
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
    m_selectedRadio = selectedRadio;
    m_codes = codes;
    m_statusLabel = statusLabel;
    m_multiCityFlightObtainer = multiCityFlightObtainer;
    m_a1xCombinations = a1xCombinations;
    m_a3xCombinations = a3xCombinations;
  }

  @Override
  public void run()
  {
    try
    {
      switch (m_selectedRadio)
      {
        case 0:
        case 1:
          if (m_codeFrom1 != null && m_codeTo2 != null && m_codeFrom1.length() == 3 && m_codeTo2.length() == 3)
          {
            m_statusLabel.setText(m_codeFrom1 + "-" + m_codeTo1 + " | " + m_codeFrom2 + "-" + m_codeTo2);
            m_multiCityFlightObtainer
                .search(m_flightsGui, m_statusLabel, m_root, m_codeFrom1, m_codeTo1, m_date1, m_codeFrom2, m_codeTo2, m_date2, null, null, null, m_numOfPersons);
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
                    Thread.sleep(getSleepMillis() + (int)(Math.random() * 100));

                    m_multiCityFlightObtainer
                        .search(m_flightsGui, m_statusLabel, m_root, codeFrom, m_codeTo1, m_date1, m_codeFrom2, codeTo, m_date2, null, null, null, m_numOfPersons);

                    m_statusLabel.setText(codeFrom + "-" + m_codeTo1 + " | " + m_codeFrom2 + "-" + codeTo);
                  }
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
              m_statusLabel.setText(from3 + "-" + to3 + " | " + m_codeFrom1 + "-" + m_codeTo1 + " | " + m_codeFrom2 + "-" + m_codeTo2);
              m_multiCityFlightObtainer.search(
                  m_flightsGui,
                  m_statusLabel,
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
                  m_numOfPersons
              );
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
              m_statusLabel.setText(from3 + "-" + to3 + " | " + m_codeFrom1 + "-" + m_codeTo1 + " | " + m_codeFrom2 + "-" + m_codeTo2);
              m_multiCityFlightObtainer.search(
                  m_flightsGui,
                  m_statusLabel,
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
                  m_numOfPersons
              );
            }
          }
          break;
      }
      m_statusLabel.setForeground(Color.GREEN);
    }
    catch (InterruptedException e)
    {
      m_statusLabel.setForeground(Color.DARK_GRAY);
    }
    catch (Exception e)
    {
      m_statusLabel.setForeground(Color.RED);
    }
  }

  public int getSleepMillis()
  {
    if (m_multiCityFlightObtainer instanceof KayakFlightObtainer)
    {
      return 600;
    }
    return 100;
  }
}
