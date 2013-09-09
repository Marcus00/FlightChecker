package si.nerve.flightchecker;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;

import com.csvreader.CsvReader;
import com.toedter.calendar.JDateChooser;
import si.nerve.flightchecker.components.MultiCityFlightTable;
import si.nerve.flightchecker.components.MultiCityFlightTableModel;
import si.nerve.flightchecker.components.SearchBoxModel;
import si.nerve.flightchecker.data.AirportData;
import si.nerve.flightchecker.data.MultiCityFlightData;
import si.nerve.flightchecker.process.ComboDocument;
import si.nerve.flightchecker.process.SelectFocusAdapter;

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
  private JLabel m_statusLabel;
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
    m_statusLabel = new JLabel();

    m_combined.addActionListener(this);
    m_combined.setActionCommand(COMBO_CHANGED);
    m_search.addActionListener(this);
    m_search.setActionCommand(SEARCH_CMD);

    m_airportMap = readAirportCsv();
    m_fromAP1.setEditable(true);
    m_toAP1.setEditable(true);
    m_fromAP2.setEditable(true);
    m_toAP2.setEditable(true);

    SearchBoxModel sbm1 = new SearchBoxModel(m_fromAP1, m_airportMap);
    //DocumentFilter filter = new UppercaseDocumentFilter();
    JTextComponent fromComboxTF1 = (JTextComponent)m_fromAP1.getEditor().getEditorComponent();
    fromComboxTF1.setDocument(new ComboDocument());
    fromComboxTF1.addFocusListener(new SelectFocusAdapter(fromComboxTF1));
    //((AbstractDocument)fromComboxTF1.getDocument()).setDocumentFilter(filter);
    m_fromAP1.setModel(sbm1);
    m_fromAP1.addItemListener(sbm1);
    SearchBoxModel sbm2 = new SearchBoxModel(m_toAP1, m_airportMap);
    JTextComponent toComboxTF1 = (JTextComponent)m_toAP1.getEditor().getEditorComponent();
    toComboxTF1.setDocument(new ComboDocument());
    toComboxTF1.addFocusListener(new SelectFocusAdapter(toComboxTF1));
    //((AbstractDocument)toComboxTF1.getDocument()).setDocumentFilter(filter);
    m_toAP1.setModel(sbm2);
    m_toAP1.addItemListener(sbm2);
    SearchBoxModel sbm3 = new SearchBoxModel(m_fromAP2, m_airportMap);
    JTextComponent fromComboxTF2 = (JTextComponent)m_fromAP2.getEditor().getEditorComponent();
    fromComboxTF2.setDocument(new ComboDocument());
    fromComboxTF2.addFocusListener(new SelectFocusAdapter(fromComboxTF2));
    //((AbstractDocument)fromComboxTF2.getDocument()).setDocumentFilter(filter);
    m_fromAP2.setModel(sbm3);
    m_fromAP2.addItemListener(sbm3);
    SearchBoxModel sbm4 = new SearchBoxModel(m_toAP2, m_airportMap);
    JTextComponent toCombocTF2 = (JTextComponent)m_toAP2.getEditor().getEditorComponent();
    toCombocTF2.setDocument(new ComboDocument());
    toCombocTF2.addFocusListener(new SelectFocusAdapter(toCombocTF2));
    //((AbstractDocument)toCombocTF2.getDocument()).setDocumentFilter(filter);
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

    JPanel statusPanel = new JPanel(new GridBagLayout());
    statusPanel.add(m_statusLabel, new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.BOTH, new Insets(0, 0, 3, 0), 0, 0));

    panel.add(commandPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.add(m_scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.add(statusPanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
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
            m_fromAP1.getItemAt(m_fromAP1.getSelectedIndex()).toString(),
            m_toAP1.getItemAt(m_toAP1.getSelectedIndex()).toString(),
            m_fromDateChooser.getDate(),
            m_fromAP2.getItemAt(m_fromAP2.getSelectedIndex()).toString(),
            m_toAP2.getItemAt(m_toAP2.getSelectedIndex()).toString(),
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
    final String toStatic = ((AirportData)m_toAP1.getItemAt(m_toAP1.getSelectedIndex())).getIataCode();
    final String fromStatic = ((AirportData)m_fromAP2.getItemAt(m_fromAP2.getSelectedIndex())).getIataCode();
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

  public JLabel getStatusLabel()
  {
    return m_statusLabel;
  }

  protected void refreshTableModel()
  {
    ArrayList<MultiCityFlightData> multiCityFlightDatas = new ArrayList<MultiCityFlightData>();
    multiCityFlightDatas.addAll(m_flightSet);

    ((MultiCityFlightTableModel)m_mainTable.getModel()).setEntityList(multiCityFlightDatas);

    m_sorter.sort();
  }

  private Map<String, AirportData> readAirportCsv()
  {
    Map<String, AirportData> airports;
    try
    {
      InputStream resourceAsStream = ClassLoader.class.getResourceAsStream("/resources/airports.csv");
      CsvReader reader = new CsvReader(new InputStreamReader(resourceAsStream));
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
