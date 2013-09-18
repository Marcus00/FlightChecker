package si.nerve.flightchecker.pages;

import si.nerve.flightchecker.FlightsGui;

import javax.swing.*;
import java.util.Date;

/**
 * Created: 10.8.13 20:27
 */
public interface MultiCityFlightObtainer
{
  public void search(FlightsGui flightGui, JLabel statusLabel, String addressRoot, String from1, String to1, Date date1, String from2, String to2, Date date2)
      throws Exception;
}
