package si.nerve.flightchecker;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.table.TableRowSorter;

import com.csvreader.CsvReader;
import com.toedter.calendar.JDateChooser;
import si.nerve.flightchecker.components.MultiCityFlightTable;
import si.nerve.flightchecker.components.MultiCityFlightTableModel;
import si.nerve.flightchecker.components.SearchBoxModel;
import si.nerve.flightchecker.data.AirportData;
import si.nerve.flightchecker.data.MultiCityFlightData;

/**
 * @author bratwurzt
 */
public class FlightsGui extends JFrame implements ActionListener
{
  public static final String COMBO_CHANGED = "COMBO_CHANGED";
  public static final String SEARCH_CMD = "SEARCH";
  private MultiCityFlightTable m_mainTable;
  public TableRowSorter<MultiCityFlightTableModel> m_sorter;
  private JScrollPane m_scrollPane;
  private JComboBox m_fromAP1, m_toAP1, m_fromAP2, m_toAP2;
  private JDateChooser m_fromDateChooser, m_toDateChooser;
  private JButton m_search;
  private JCheckBox m_combined;
  public static int[] c_columnWidths = {5, 5, 5, 5, 7, 10, 5, 5, 5, 5, 7, 10, 10, 2};
  private Set<MultiCityFlightData> m_flightSet;
  private MultiCityFlightObtainer m_cityFlightObtainer;
  private Map<String, AirportData> m_airportMap;
  private ScheduledExecutorService m_executorService;

  public FlightsGui() throws HeadlessException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException
  {
    super("Flights");
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    m_cityFlightObtainer = new KayakFlightObtainer();
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    m_mainTable = new MultiCityFlightTable(new MultiCityFlightTableModel(new ArrayList<MultiCityFlightData>()));
    m_mainTable.setPreferredScrollableViewportSize(new Dimension(1200, 800));
    m_mainTable.setFillsViewportHeight(true);
    m_sorter = new TableRowSorter<MultiCityFlightTableModel>((MultiCityFlightTableModel)m_mainTable.getModel());
    m_sorter.toggleSortOrder(MultiCityFlightTableModel.COL_PRICE);
    m_mainTable.setRowSorter(m_sorter);
    m_scrollPane = new JScrollPane(m_mainTable);

    m_fromAP1 = new JComboBox();
    m_toAP1 = new JComboBox();
    m_fromAP2 = new JComboBox();
    m_toAP2 = new JComboBox();
    m_fromDateChooser = new JDateChooser();
    m_fromDateChooser.setDateFormatString("dd.MM.yyyy");
    m_toDateChooser = new JDateChooser();
    m_toDateChooser.setDateFormatString("dd.MM.yyyy");
    m_combined = new JCheckBox("Rošada");
    m_search = new JButton("Išči");

    m_combined.addActionListener(this);
    m_combined.setActionCommand(COMBO_CHANGED);
    m_search.addActionListener(this);
    m_search.setActionCommand(SEARCH_CMD);

    m_airportMap = readAirportCsv();
    m_fromAP1.setEditable(true);
    m_toAP1.setEditable(true);
    m_fromAP2.setEditable(true);
    m_toAP2.setEditable(true);
    ArrayList<String> airportCodes = new ArrayList<String>(m_airportMap.keySet());
    SearchBoxModel sbm1 = new SearchBoxModel(m_fromAP1, airportCodes);
    m_fromAP1.setModel(sbm1);
    m_fromAP1.addItemListener(sbm1);
    SearchBoxModel sbm2 = new SearchBoxModel(m_toAP1, airportCodes);
    m_toAP1.setModel(sbm2);
    m_toAP1.addItemListener(sbm2);
    SearchBoxModel sbm3 = new SearchBoxModel(m_fromAP2, airportCodes);
    m_fromAP2.setModel(sbm3);
    m_fromAP2.addItemListener(sbm3);
    SearchBoxModel sbm4 = new SearchBoxModel(m_toAP2, airportCodes);
    m_toAP2.setModel(sbm4);
    m_toAP2.addItemListener(sbm4);

    JPanel commandPanel = new JPanel(new GridBagLayout());
    commandPanel.add(m_fromAP1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_toAP1, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_fromAP2, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_toAP2, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_fromDateChooser, new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_toDateChooser, new GridBagConstraints(5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_combined, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_search, new GridBagConstraints(7, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    JPanel panel = new JPanel(new GridBagLayout());

    panel.add(commandPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.add(m_scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.setOpaque(true);
    add(panel, BorderLayout.CENTER);

    pack();
    setVisible(true);
    m_mainTable.setColumnWidths(c_columnWidths);
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (SEARCH_CMD.equals(e.getActionCommand()))
    {
      search(m_combined.isSelected());
    }
    else if (e.getSource().equals(m_fromDateChooser))
    {
      m_toDateChooser.setDate(m_fromDateChooser.getDate());
    }
  }

  private void search(boolean combined)
  {
    if (combined)
    {
      searchCombined();
    }
    else
    {
      try
      {
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        m_flightSet = m_cityFlightObtainer.get("de",
            (String)m_fromAP1.getItemAt(m_fromAP1.getSelectedIndex()),
            (String)m_toAP1.getItemAt(m_toAP1.getSelectedIndex()),
            m_fromDateChooser.getDate(),
            (String)m_fromAP2.getItemAt(m_fromAP2.getSelectedIndex()),
            (String)m_toAP2.getItemAt(m_toAP2.getSelectedIndex()),
            m_toDateChooser.getDate());
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      finally
      {
        refreshTableModel();
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }

  private void searchCombined()
  {
    String[] codes = {
        "VIE", "BRU", "CRL", "ZAG", "MRS", "NCE", "ORY", "CDG", "FRA", "MUC", "BUD", "BLQ", "LIN", "MXP", "FCO", "CIA", "TSF",
        "VCE", "LJU", "BCN", "MAD", "VLC", "BRN", "GVA", "LUG", "ZRH", "EDI", "MAN", "LHR"};

    final String from = m_fromAP1.getSelectedIndex() > 0 ? (String)m_fromAP1.getItemAt(m_fromAP1.getSelectedIndex()) : null;
    final String to = m_toAP2.getSelectedIndex() > 0 ? (String)m_toAP2.getItemAt(m_toAP2.getSelectedIndex()) : null;

    //m_executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    m_executorService = Executors.newScheduledThreadPool(1);
    final String toStatic = (String)m_toAP1.getItemAt(m_toAP1.getSelectedIndex());
    final String fromStatic = (String)m_fromAP2.getItemAt(m_fromAP2.getSelectedIndex());
    final Date fromDate = m_fromDateChooser.getDate();
    final Date toDate = m_toDateChooser.getDate();

    m_flightSet = new HashSet<MultiCityFlightData>();
    if (from != null && from.length() == 3 && to != null && to.length() == 3)
    {
      m_executorService.execute(new SearchAndRefresh(this, from, to, toStatic, fromStatic, fromDate, toDate));
    }

    long delay = (long)(300 + Math.random() * 1000);
    for (final String codeFrom : codes)
    {
      for (final String codeTo : codes)
      {
        if (!codeFrom.equals(from) && !codeTo.equals(to))
        {
          m_executorService.schedule(new SearchAndRefresh(this, codeFrom, codeTo, toStatic, fromStatic, fromDate, toDate), delay, TimeUnit.MILLISECONDS);
          delay += (long)(300 + Math.random() * 1000);
        }
      }
    }
  }

  public TableRowSorter<MultiCityFlightTableModel> getSorter()
  {
    return m_sorter;
  }

  public MultiCityFlightTable getMainTable()
  {
    return m_mainTable;
  }

  public Set<MultiCityFlightData> getFlightSet()
  {
    return m_flightSet;
  }

  public MultiCityFlightObtainer getCityFlightObtainer()
  {
    return m_cityFlightObtainer;
  }

  protected void refreshTableModel()
  {
    ArrayList<MultiCityFlightData> multiCityFlightDatas = new ArrayList<MultiCityFlightData>();
    multiCityFlightDatas.addAll(m_flightSet);

    ((MultiCityFlightTableModel)m_mainTable.getModel()).setEntityList(multiCityFlightDatas);

    //MultiCityFlightTableModel model = new MultiCityFlightTableModel(multiCityFlightDatas);
    //m_sorter.setModel(model);
    //m_sorter.toggleSortOrder(MultiCityFlightTableModel.COL_PRICE);
    m_sorter.sort();
    //m_mainTable.setModel(model);
    //m_mainTable.setRowSorter(m_sorter);
    //m_mainTable.setColumnWidths(c_columnWidths);
    //this.pack();
  }

  private Map<String, AirportData> readAirportCsv()
  {
    Map<String, AirportData> airports;
    try
    {
      CsvReader reader = new CsvReader(new InputStreamReader(new FileInputStream(new File("airports.csv"))));
      reader.setDelimiter(',');
      reader.readHeaders();
      try
      {
        airports = new HashMap<String, AirportData>();
        while (reader.readRecord())
        {
          String key = reader.get(4);
          int i;
          if (key != null && !key.isEmpty())
          {
            i = 0;
            airports.put(key, new AirportData(
                Integer.parseInt(reader.get(i++)),
                reader.get(i++),
                reader.get(i++),
                reader.get(i++),
                reader.get(i++),
                reader.get(i++),
                Float.valueOf(reader.get(++i)) / 0.3048f,
                reader.get(++i)
            ));
          }
        }
        return airports;
      }
      finally
      {
        reader.close();
      }
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return null;
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
