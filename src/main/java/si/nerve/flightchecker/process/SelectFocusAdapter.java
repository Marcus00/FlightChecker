package si.nerve.flightchecker.process;

import java.awt.event.FocusAdapter;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * @author bratwurzt
 */
public class SelectFocusAdapter extends FocusAdapter
{
  private JTextComponent m_component;

  public SelectFocusAdapter(JTextComponent component)
  {
    m_component = component;
  }

  public void focusGained(java.awt.event.FocusEvent evt)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        m_component.selectAll();
      }
    });
  }
}
