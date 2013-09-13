package si.nerve.flightchecker;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;

import com.csvreader.CsvReader;
import si.nerve.flightchecker.components.DoubleDateChooser;
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
  public static final String SEARCH = "SEARCH";
  public static final String STOP_SEARCH = "STOP_SEARCH";
  public static final String CHECKBOX_CHANGED = "CHECKBOX_CHANGED";
  public static final int QUEUE_SIZE = 50;
  private MultiCityFlightTable m_mainTable;
  public TableRowSorter<MultiCityFlightTableModel> m_sorter;
  private JScrollPane m_scrollPane;
  private JComboBox m_fromAP1, m_toAP1, m_fromAP2, m_toAP2;
  private JButton m_searchButton;
  private DoubleDateChooser m_dateChooser;
  private JLabel m_statusLabel;
  private JCheckBox m_combined;
  private JCheckBox m_showInEuro;
  public static int[] c_columnWidths = {5, 5, 5, 5, 7, 10, 5, 5, 5, 5, 7, 10, 10, 3, 15};
  private PriorityQueue<MultiCityFlightData> m_flightQueue;
  private Comparator<MultiCityFlightData> m_comparator;
  private Map<String, AirportData> m_airportMap;
  private ExecutorService m_executorService;

  private JMenuBar m_menuBar;
  private JMenu m_mainMenu;
  private JFileChooser m_fileChooser;
  private JMenuItem m_menuItem;

  public FlightsGui() throws HeadlessException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, ParseException
  {
    super("Flights");
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    m_mainTable = new MultiCityFlightTable(new MultiCityFlightTableModel(new ArrayList<MultiCityFlightData>()));
    m_mainTable.setPreferredScrollableViewportSize(new Dimension(1300, 800));
    m_mainTable.setFillsViewportHeight(true);
    m_sorter = new TableRowSorter<MultiCityFlightTableModel>((MultiCityFlightTableModel) m_mainTable.getModel());
    m_sorter.toggleSortOrder(MultiCityFlightTableModel.COL_PRICE);
    m_mainTable.setRowSorter(m_sorter);
    m_sorter.setSortsOnUpdates(true);
    m_scrollPane = new JScrollPane(m_mainTable);

    m_fromAP1 = new JComboBox();
    m_toAP1 = new JComboBox();
    m_fromAP2 = new JComboBox();
    m_toAP2 = new JComboBox();

    m_searchButton = new JButton("Išči");
    m_statusLabel = new JLabel();
    m_combined = new JCheckBox("Rošada");
    m_showInEuro = new JCheckBox("€");

    m_searchButton.addActionListener(this);
    m_searchButton.setActionCommand(SEARCH);

    m_showInEuro.addActionListener(this);
    m_showInEuro.setActionCommand(CHECKBOX_CHANGED);
    m_showInEuro.setSelected(true);

    m_airportMap = readAirportCsv();
    m_fromAP1.setEditable(true);
    m_toAP1.setEditable(true);
    m_fromAP2.setEditable(true);
    m_toAP2.setEditable(true);
    m_dateChooser = new DoubleDateChooser();

    SearchBoxModel sbm1 = new SearchBoxModel(m_fromAP1, m_airportMap);
    JTextComponent fromComboxTF1 = (JTextComponent) m_fromAP1.getEditor().getEditorComponent();
    fromComboxTF1.setDocument(new ComboDocument());
    fromComboxTF1.addFocusListener(new SelectFocusAdapter(fromComboxTF1));
    m_fromAP1.setModel(sbm1);
    m_fromAP1.addItemListener(sbm1);
    SearchBoxModel sbm2 = new SearchBoxModel(m_toAP1, m_airportMap);
    JTextComponent toComboxTF1 = (JTextComponent) m_toAP1.getEditor().getEditorComponent();
    toComboxTF1.setDocument(new ComboDocument());
    toComboxTF1.addFocusListener(new SelectFocusAdapter(toComboxTF1));
    m_toAP1.setModel(sbm2);
    m_toAP1.addItemListener(sbm2);
    SearchBoxModel sbm3 = new SearchBoxModel(m_fromAP2, m_airportMap);
    JTextComponent fromComboxTF2 = (JTextComponent) m_fromAP2.getEditor().getEditorComponent();
    fromComboxTF2.setDocument(new ComboDocument());
    fromComboxTF2.addFocusListener(new SelectFocusAdapter(fromComboxTF2));
    m_fromAP2.setModel(sbm3);
    m_fromAP2.addItemListener(sbm3);
    SearchBoxModel sbm4 = new SearchBoxModel(m_toAP2, m_airportMap);
    JTextComponent toCombocTF2 = (JTextComponent) m_toAP2.getEditor().getEditorComponent();
    toCombocTF2.setDocument(new ComboDocument());
    toCombocTF2.addFocusListener(new SelectFocusAdapter(toCombocTF2));
    m_toAP2.setModel(sbm4);
    m_toAP2.addItemListener(sbm4);

    JPanel commandPanel = new JPanel(new GridBagLayout());
    commandPanel.add(m_fromAP1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_toAP1, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_fromAP2, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_toAP2, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_dateChooser, new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_combined, new GridBagConstraints(5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_showInEuro, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_searchButton, new GridBagConstraints(7, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    JPanel panel = new JPanel(new GridBagLayout());

    JPanel statusPanel = new JPanel(new GridBagLayout());
    statusPanel.add(m_statusLabel, new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.BOTH, new Insets(0, 3, 3, 0), 0, 0));

    panel.add(commandPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.add(m_scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.add(statusPanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.setOpaque(true);
    add(panel, BorderLayout.CENTER);

    pack();
    setVisible(true);
    m_mainTable.setColumnWidths(c_columnWidths);

    m_comparator = new Comparator<MultiCityFlightData>()
    {
      @Override
      public int compare(MultiCityFlightData o1, MultiCityFlightData o2)
      {
        return o1.getPriceAmount(true) > o2.getPriceAmount(true) ? -1 : o1.getPriceAmount(true) < o2.getPriceAmount(true) ? 1 : 0;
      }
    };

    buildMenus();
  }

  private void buildMenus()
  {
    m_fileChooser = new JFileChooser();
    m_fileChooser.addChoosableFileFilter(new FileFilter()
    {
      @Override
      public boolean accept(File file)
      {
        return file.getName().toLowerCase().endsWith(".csv");
      }

      @Override
      public String getDescription()
      {
        return null;
      }
    });

    m_menuBar = new JMenuBar();
    m_mainMenu = new JMenu("File");
    m_mainMenu.setMnemonic(KeyEvent.VK_F);
    m_menuBar.add(m_mainMenu);
    m_menuItem = new JMenuItem("Open...", KeyEvent.VK_O);
    m_menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_MASK));
    m_menuItem.addActionListener(this);
    m_menuItem.setActionCommand("MENU.OPEN");
    m_mainMenu.add(m_menuItem);
    m_menuItem = new JMenuItem("Save...", KeyEvent.VK_S);
    m_menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_MASK));
    m_menuItem.addActionListener(this);
    m_menuItem.setActionCommand("MENU.SAVE");
    m_mainMenu.add(m_menuItem);
    m_menuItem = new JMenuItem("Close", KeyEvent.VK_C);
    m_menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
    m_menuItem.setActionCommand("MENU.CLOSE");
    m_menuItem.addActionListener(this);
    m_mainMenu.add(m_menuItem);
    //    setJMenuBar(m_menuBar);
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    String action = e.getActionCommand();
    if (SEARCH.equals(action))
    {
      setHoldCursor();
      search(m_combined.isSelected());
      m_searchButton.setText("Stop");
      m_searchButton.setActionCommand(STOP_SEARCH);
      pack();
    }
    else if (STOP_SEARCH.equals(action))
    {
      m_searchButton.setText("Išči");
      m_searchButton.setActionCommand(SEARCH);
      pack();
      m_executorService.shutdownNow();
      resetCursor();
    }
    else if ("MENU.OPEN".equals(action))
    {
      int returnVal = m_fileChooser.showOpenDialog(FlightsGui.this);

      if (returnVal == JFileChooser.APPROVE_OPTION)
      {
        File file = m_fileChooser.getSelectedFile();
        if (file.exists())
        {
          setHoldCursor();
          try
          {
            m_flightQueue = readFlightsFile(file.getAbsolutePath());
          }
          finally
          {
            resetCursor();
          }
        }
        refreshTableModel();
      }
    }
    else if ("MENU.SAVE".equals(action))
    {
      int returnVal = m_fileChooser.showSaveDialog(FlightsGui.this);
      if (returnVal == JFileChooser.APPROVE_OPTION)
      {
        writeFlightsFile(m_flightQueue, m_fileChooser.getSelectedFile().getAbsolutePath());
      }
    }
    else if ("MENU.CLOSE".equals(action))
    {
      m_flightQueue = new PriorityQueue<MultiCityFlightData>(QUEUE_SIZE, m_comparator);
      refreshTableModel();
    }
    else if (CHECKBOX_CHANGED.equals(action))
    {
      MultiCityFlightTableModel tableModel = (MultiCityFlightTableModel) m_mainTable.getModel();
      tableModel.setConvertToEuro(m_showInEuro.isSelected());
      tableModel.fireTableDataChanged();
    }
  }

  private void writeFlightsFile(PriorityQueue<MultiCityFlightData> flightSet, String absolutePath)
  {
    try
    {
      FileWriter writer = new FileWriter(absolutePath);
      writer.append("AP-From1-IATA,").append("Date-From1,").append("AP-To1-IATA,").append("Date-To1,").append("Duration1,").append("Layovers1,");
      writer.append("AP-From2-IATA,").append("Date-From2,").append("AP-To2-IATA,").append("Date-To2,").append("Duration2,").append("Layovers2,");
      writer.append("Price,");
      writer.append("PriceType");
      writer.append('\n');
      for (MultiCityFlightData flightData : flightSet)
      {
        writer.append(flightData.getFlightLegs().get(0).getFromAirportCode()).append(',');
        writer.append(flightData.getFlightLegs().get(0).getDepartureLocalTime()).append(',');
        writer.append(flightData.getFlightLegs().get(0).getToAirportCode()).append(',');
        writer.append(flightData.getFlightLegs().get(0).getArrivalLocalTime()).append(',');
        writer.append(flightData.getFlightLegs().get(0).getDuration()).append(',');
        writer.append(flightData.getFlightLegs().get(0).getLayovers()).append(',');
        writer.append(flightData.getFlightLegs().get(1).getFromAirportCode()).append(',');
        writer.append(flightData.getFlightLegs().get(1).getDepartureLocalTime()).append(',');
        writer.append(flightData.getFlightLegs().get(1).getToAirportCode()).append(',');
        writer.append(flightData.getFlightLegs().get(1).getArrivalLocalTime()).append(',');
        writer.append(flightData.getFlightLegs().get(1).getDuration()).append(',');
        writer.append(String.valueOf(flightData.getPriceAmount(false))).append(',');
        writer.append(flightData.getPriceType().getMonSign());
        writer.append('\n');
      }
      writer.flush();
      writer.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  private PriorityQueue<MultiCityFlightData> readFlightsFile(String absolutePath)
  {
    return new PriorityQueue<MultiCityFlightData>(QUEUE_SIZE, m_comparator);
  }

  private void search(boolean combined)
  {
    String[] codes = {
        "VIE", "BRU", "CRL", "ZAG", "MRS", "NCE", "ORY", "CDG", "FRA", "MUC", "BUD", "BLQ", "LIN", "MXP", "FCO", "CIA", "TSF",
        "VCE", "LJU", "BCN", "MAD", "VLC", "BRN", "GVA", "LUG", "ZRH", "EDI", "MAN", "LHR"};

    final String from = m_fromAP1.getSelectedIndex() >= 0 ? ((AirportData)m_fromAP1.getItemAt(m_fromAP1.getSelectedIndex())).getIataCode() : null;
    final String to = m_toAP2.getSelectedIndex() >= 0 ? ((AirportData)m_toAP2.getItemAt(m_toAP2.getSelectedIndex())).getIataCode() : null;

    m_executorService = Executors.newFixedThreadPool(7);
    final String toStatic = ((AirportData) m_toAP1.getItemAt(m_toAP1.getSelectedIndex())).getIataCode();
    final String fromStatic = ((AirportData) m_fromAP2.getItemAt(m_fromAP2.getSelectedIndex())).getIataCode();
    final Date fromDate = m_dateChooser.getFromDateChooser().getDate();
    final Date toDate = m_dateChooser.getToDateChooser().getDate();

    m_flightQueue = new PriorityQueue<MultiCityFlightData>(QUEUE_SIZE, m_comparator);
    if (from != null && from.length() == 3 && to != null && to.length() == 3)
    {
      m_executorService.execute(new SearchAndRefresh("com", this, from, to, toStatic, fromStatic, fromDate, toDate, combined, codes));
      m_executorService.execute(new SearchAndRefresh("de", this, from, to, toStatic, fromStatic, fromDate, toDate, combined, codes));
      m_executorService.execute(new SearchAndRefresh("it", this, from, to, toStatic, fromStatic, fromDate, toDate, combined, codes));
      m_executorService.execute(new SearchAndRefresh("co.uk", this, from, to, toStatic, fromStatic, fromDate, toDate, combined, codes));
      m_executorService.execute(new SearchAndRefresh("es", this, from, to, toStatic, fromStatic, fromDate, toDate, combined, codes));
      m_executorService.execute(new SearchAndRefresh("fr", this, from, to, toStatic, fromStatic, fromDate, toDate, combined, codes));
      m_executorService.execute(new SearchAndRefresh("nl", this, from, to, toStatic, fromStatic, fromDate, toDate, combined, codes));
      m_executorService.execute(new SearchAndRefresh("pl", this, from, to, toStatic, fromStatic, fromDate, toDate, combined, codes));
    }
  }

  public MultiCityFlightTable getMainTable()
  {
    return m_mainTable;
  }

  public PriorityQueue<MultiCityFlightData> getFlightQueue()
  {
    return m_flightQueue;
  }

  public JLabel getStatusLabel()
  {
    return m_statusLabel;
  }

  protected void refreshTableModel()
  {
    ArrayList<MultiCityFlightData> multiCityFlightDatas = new ArrayList<MultiCityFlightData>();
    multiCityFlightDatas.addAll(m_flightQueue);

    MultiCityFlightTableModel tableModel = (MultiCityFlightTableModel) m_mainTable.getModel();
    tableModel.setEntityList(multiCityFlightDatas);
    tableModel.fireTableDataChanged();
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

  public void setHoldCursor()
  {
    setCursorOnActiveWindow(this, Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  public void resetCursor()
  {
    setCursorOnActiveWindow(this, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  private Cursor setCursorOnActiveWindow(Window window, Cursor cursor)
  {
    if (window.getFocusOwner() != null)
    {
      Cursor oldCursor = window.getCursor();
      window.setCursor(cursor);
      return oldCursor;
    }
    else if (window.getOwnedWindows().length > 0)
    {
      Window[] temp = window.getOwnedWindows();
      Cursor tmpCursor = null;
      for (int i = 0; i < temp.length && tmpCursor == null; i++)
      {
        tmpCursor = setCursorOnActiveWindow(temp[i], cursor);
      }
      return tmpCursor;
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
    catch (ParseException e)
    {
      e.printStackTrace();
    }
  }
}
