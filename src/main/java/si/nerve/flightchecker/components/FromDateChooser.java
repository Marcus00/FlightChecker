package si.nerve.flightchecker.components;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;

/**
 * @author bratwurzt
 */
public class FromDateChooser extends JDateChooser
{
  private ToDateChooser m_toDateChooser;

  public FromDateChooser(ToDateChooser toDateChooser)
  {
    super();
    m_toDateChooser = toDateChooser;
    setDateFormatString("dd.MM.yyyy");
    getDateEditor().addPropertyChangeListener(new PropertyChangeListener()
    {
      @Override
      public void propertyChange(PropertyChangeEvent e)
      {
        if ("date".equals(e.getPropertyName())/* && e.getOldValue() != null*/)
        {
          m_toDateChooser.setDate(getDate());
          SwingUtilities.invokeLater(new Runnable()
          {
            @Override
            public void run()
            {
              m_toDateChooser.getCalendarButton().doClick();
              m_toDateChooser.requestFocus();
            }
          });
        }
      }
    });
  }

  public JPopupMenu getPopup()
  {
    return popup;
  }

  public void actionPerformed(ActionEvent e)
  {
    int x = calendarButton.getWidth() - (int) popup.getPreferredSize().getWidth();
    int y = calendarButton.getY() + calendarButton.getHeight();

    Calendar calendar = Calendar.getInstance();
    Date date = dateEditor.getDate();
    if (date != null)
    {
      calendar.setTime(date);
    }
    jcalendar.setCalendar(calendar);
    popup.show(calendarButton, x, y);
    dateSelected = false;
  }
}
