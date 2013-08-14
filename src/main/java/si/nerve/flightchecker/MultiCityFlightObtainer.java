package si.nerve.flightchecker;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Created: 10.8.13 20:27
 */
public interface MultiCityFlightObtainer
{
  public MultiCityFlightData get(String from1, String to1, Date date1, String from2, String to2, Date date2) throws IOException;
}
