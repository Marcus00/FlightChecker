package si.nerve.flightchecker;

import java.io.IOException;
import java.util.Date;
import java.util.Set;

import si.nerve.flightchecker.data.MultiCityFlightData;

/**
 * Created: 10.8.13 20:27
 */
public interface MultiCityFlightObtainer
{
  public Set<MultiCityFlightData> get(String from1, String to1, Date date1, String from2, String to2, Date date2) throws IOException;
}
