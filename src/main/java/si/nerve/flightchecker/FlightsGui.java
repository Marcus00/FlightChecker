package si.nerve.flightchecker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.table.TableRowSorter;

import si.nerve.flightchecker.data.MultiCityFlightData;
import si.nerve.flightchecker.data.MultiCityFlightTable;
import si.nerve.flightchecker.data.MultiCityFlightTableModel;

/**
 * @author bratwurzt
 */
public class FlightsGui extends JFrame
{
  private MultiCityFlightTable m_mainTable;
  public TableRowSorter<MultiCityFlightTableModel> m_sorter;
  private JScrollPane m_scrollPane;
  public static int[] m_columnWidths = {5, 7, 5, 7, 10, 15, 5, 7, 5, 7, 10, 15, 10};
  private Set<MultiCityFlightData> m_flightSet;

  public FlightsGui() throws HeadlessException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException
  {
    super("BeeTee");
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    MultiCityFlightObtainer obtainer = new KayakFlightObtainer();
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
    try
    {
      m_flightSet = obtainer.get("LJU", "NYC", formatter.parse("18.8.2013"), "NYC", "VIE", formatter.parse("25.8.2013"));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    ArrayList<MultiCityFlightData> multiCityFlightDatas = new ArrayList<MultiCityFlightData>();
    multiCityFlightDatas.addAll(m_flightSet);
    m_mainTable = new MultiCityFlightTable(new MultiCityFlightTableModel(multiCityFlightDatas));

    m_mainTable.setPreferredScrollableViewportSize(new Dimension(1200, 800));
    m_mainTable.setFillsViewportHeight(true);
    m_sorter = new TableRowSorter<MultiCityFlightTableModel>((MultiCityFlightTableModel)m_mainTable.getModel());
    m_mainTable.setRowSorter(m_sorter);

    m_scrollPane = new JScrollPane(m_mainTable);
    JPanel panel = new JPanel(new GridBagLayout());

    panel.add(m_scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.setOpaque(true);
    add(panel, BorderLayout.CENTER);

    pack();
    setVisible(true);
    m_mainTable.setColumnWidths(m_columnWidths);
  }

  public static void main(String[] args)
  {
    try
    {
      new FlightsGui();
    }
    catch (ClassNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (UnsupportedLookAndFeelException e)
    {
      e.printStackTrace();
    }
    catch (InstantiationException e)
    {
      e.printStackTrace();
    }
    catch (IllegalAccessException e)
    {
      e.printStackTrace();
    }
  }
}
