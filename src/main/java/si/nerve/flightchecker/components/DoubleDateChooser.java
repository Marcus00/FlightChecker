package si.nerve.flightchecker.components;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.*;

/**
 * @author bratwurzt
 */
public class DoubleDateChooser extends JPanel
{
  private FromDateChooser m_fromDateChooser;
  private ToDateChooser m_toDateChooser;

  public DoubleDateChooser()
  {
    super(new GridBagLayout());
    m_toDateChooser = new ToDateChooser();
    m_fromDateChooser = new FromDateChooser(m_toDateChooser);

    add(m_fromDateChooser, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    add(m_toDateChooser, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
  }

  public JDateChooser getFromDateChooser()
  {
    return m_fromDateChooser;
  }

  public JDateChooser getToDateChooser()
  {
    return m_toDateChooser;
  }
}