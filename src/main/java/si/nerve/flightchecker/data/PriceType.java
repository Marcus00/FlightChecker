package si.nerve.flightchecker.data;

/**
 * @author bratwurzt
 */
public enum PriceType
{
  EURO,
  DOLLAR,
  POUND
  ;

  public static PriceType getInstance(char monetarySign)
  {
    if ('$' == monetarySign)
    {
      return DOLLAR;
    }
    else if ('€' == monetarySign)
    {
      return EURO;
    }
    else if ('£' == monetarySign)
    {
      return POUND;
    }
    return null;
  }
}
