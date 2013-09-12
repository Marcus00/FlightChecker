package si.nerve.flightchecker.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import com.toedter.calendar.JDateChooser;

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