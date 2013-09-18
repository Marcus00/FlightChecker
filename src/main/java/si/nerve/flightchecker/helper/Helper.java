package si.nerve.flightchecker.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author bratwurzt
 */
public class Helper
{
  public static String readResponse(InputStream ins, String charset) throws IOException
  {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int cnt;
    while ((cnt = ins.read(buffer)) >= 0)
    {
      if (cnt > 0)
      {
        bos.write(buffer, 0, cnt);
      }
    }
    return bos.toString(charset);
  }
}
