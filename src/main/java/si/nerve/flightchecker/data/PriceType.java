package si.nerve.flightchecker.data;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import si.nerve.flightchecker.helper.Helper;

/**
 * @author bratwurzt
 */
public enum PriceType
{
  EURO,
  DOLLAR,
  POUND;

  private static Long m_lastUpdated = Long.MAX_VALUE;
  private double m_rate = 0;

  public static PriceType getInstance(String price)
  {
    return price.contains("€") ? PriceType.EURO : price.contains("£") ? PriceType.POUND : PriceType.DOLLAR;
  }

  public String getMonSign()
  {
    switch (this)
    {
      case EURO:
        return "€";
      case DOLLAR:
        return "$";
      case POUND:
        return "£";
      default:
        return "?";
    }
  }

  public double getConversionToEuro(int priceAmount)
  {
    long currentTime = System.currentTimeMillis();
    try
    {
      if (currentTime - m_lastUpdated > 21600000 || Long.MAX_VALUE == m_lastUpdated)  // 6h
      {
        m_rate = getRateToEuros(this);
        m_lastUpdated = currentTime;
      }
    }
    catch (IOException ignored)
    {
    }
    return m_rate == 0 ? (this.equals(PriceType.DOLLAR) ? 0.749288176 : this.equals(PriceType.POUND) ? 1.19646336 : 1.0) * priceAmount : m_rate * priceAmount;
  }

  private double getRateToEuros(PriceType priceTypeFrom) throws IOException
  {
    String google = "http://www.google.com/ig/calculator?hl=en&q=1";
    String baseCurrency = get3LetterCode(priceTypeFrom);

    URL url = new URL(google + baseCurrency + "=EUR");
    URLConnection urlConnection = url.openConnection();
    String jsonString = Helper.readResponse(urlConnection.getInputStream(), urlConnection);
    String firstAnchor = ",rhs: \"";
    int startIndex = jsonString.indexOf(firstAnchor);
    return Double.parseDouble(jsonString.substring(startIndex + firstAnchor.length(), jsonString.indexOf(" Euros", startIndex + firstAnchor.length())));
  }

  private String get3LetterCode(PriceType priceType)
  {
    switch (priceType)
    {
      case EURO:
        return "EUR";
      case DOLLAR:
        return "USD";
      case POUND:
        return "GBP";
      default:
        return "EUR";
    }
  }
}
