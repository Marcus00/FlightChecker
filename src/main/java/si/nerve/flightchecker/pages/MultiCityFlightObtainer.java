package si.nerve.flightchecker.pages;

import java.util.Date;

import javax.swing.JLabel;

import si.nerve.flightchecker.FlightsGui;

/**
 * Created: 10.8.13 20:27
 */
public interface MultiCityFlightObtainer
{
  public void search(
      FlightsGui flightGui,
      JLabel kayakComStatusLabel, String addressRoot,
      String from1,
      String to1,
      Date date1,
      String from2,
      String to2,
      Date date2) throws Exception;
}
