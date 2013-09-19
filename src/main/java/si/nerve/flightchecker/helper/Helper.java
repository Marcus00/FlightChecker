package si.nerve.flightchecker.helper;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

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

  public static int getSelectedButtonText(ButtonGroup buttonGroup)
  {
    for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements(); )
    {
      AbstractButton button = buttons.nextElement();

      if (button.isSelected())
      {
        String text = button.getText();
        if ("N".equals(text))
        {
          return 0;
        }
        else if ("Rošada".equals(text))
        {
          return 1;
        }
        else if ("1x".equals(text))
        {
          return 2;
        }
        else // 3x
        {
          return 3;
        }
      }
    }

    return -1;
  }
}
