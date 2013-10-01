package si.nerve.flightchecker.pages;

import si.nerve.flightchecker.FlightsGui;

import javax.swing.*;
import java.util.Date;

/**
 * Created: 10.8.13 20:27
 */
public interface MultiCityFlightObtainer
{
  public void search(FlightsGui flightGui, String addressRoot,
      String from1, String to1, Date date1, String from2, String to2, Date date2, String from3, String to3, Date date3, Integer numOfPersons, boolean changeProxy) throws Exception;
}
