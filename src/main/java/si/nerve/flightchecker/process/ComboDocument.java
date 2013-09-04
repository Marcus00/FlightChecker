package si.nerve.flightchecker.process;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**
 * @author bratwurzt
 */
public class ComboDocument extends PlainDocument
{
  @Override
  public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException
  {
    if (str == null)
    {
      return;
    }

    int length = getLength() + str.length();
    if (length <= 3)
    {
      super.insertString(offset, str.toUpperCase(), attr);
    }
  }
}
