package si.nerve.flightchecker;

import java.util.Date;

/**
 * Created: 10.8.13 20:31
 */
public class MultiCityFlightData
{
  private int price;
  private String from1;
  private String to1;
  private Date date1;
  private String from2;
  private String to2;
  private Date date2;

  public MultiCityFlightData(int price, String from1, String to1, Date date1, String from2, String to2, Date date2)
  {
    this.price = price;
    this.from1 = from1;
    this.to1 = to1;
    this.date1 = date1;
    this.from2 = from2;
    this.to2 = to2;
    this.date2 = date2;
  }

  public int getPrice()
  {
    return price;
  }

  public String getFrom1()
  {
    return from1;
  }

  public String getTo1()
  {
    return to1;
  }

  public Date getDate1()
  {
    return date1;
  }

  public String getFrom2()
  {
    return from2;
  }

  public String getTo2()
  {
    return to2;
  }

  public Date getDate2()
  {
    return date2;
  }
}
