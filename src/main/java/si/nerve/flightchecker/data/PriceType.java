package si.nerve.flightchecker.data;

/**
 * @author bratwurzt
 */
public enum PriceType
{
  EURO,
  DOLLAR,
  POUND;

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
}
