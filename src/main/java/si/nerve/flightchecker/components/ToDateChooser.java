package si.nerve.flightchecker.components;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.Date;

/**
 * @author bratwurzt
 */
public class ToDateChooser extends JDateChooser
{
  public ToDateChooser()
  {
    super();
    setDateFormatString("dd.MM.yyyy");
  }

  public JPopupMenu getPopup()
  {
    return popup;
  }

  public void actionPerformed(ActionEvent e)
  {
    int x = -getCalendarButton().getX();
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
