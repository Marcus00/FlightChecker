package si.nerve.flightchecker;

import com.csvreader.CsvReader;
import com.toedter.calendar.JDateChooser;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import si.nerve.flightchecker.components.*;
import si.nerve.flightchecker.data.AirportData;
import si.nerve.flightchecker.data.MultiCityFlightData;
import si.nerve.flightchecker.helper.Helper;
import si.nerve.flightchecker.pages.EbookersFlightObtainer;
import si.nerve.flightchecker.pages.EdreamsFlightObtainer;
import si.nerve.flightchecker.pages.ExpediaFlightObtainer;
import si.nerve.flightchecker.pages.KayakFlightObtainer;
import si.nerve.flightchecker.process.ComboDocument;
import si.nerve.flightchecker.process.SelectFocusAdapter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author bratwurzt
 */
public class FlightsGui extends JFrame implements ActionListener, WindowListener
{
  public static final String SEARCH = "SEARCH";
  public static final String STOP_SEARCH = "STOP_SEARCH";
  public static final String CHECKBOX_CHANGED = "CHECKBOX_CHANGED";
  public static final String RADIO_CLICKED = "RADIO_CLICKED";
  public static final int QUEUE_SIZE = 200;
  private MultiCityFlightTable m_mainTable;
  public TableRowSorter<MultiCityFlightTableModel> m_sorter;

  private JComboBox m_fromAP1, m_toAP1, m_fromAP2, m_toAP2, m_numOfPersons;
  private JButton m_searchButton;
  private DoubleDateChooser m_dateChooser;
  private JDateChooser m_dateXChooser;
  private JCheckBox m_showInEuro;

  private Map<String, JCheckBox> m_kayakCBMap, m_expediaCBMap, m_ebookersCBMap, m_edreamsCBMap;
  private Map<String, JLabel> m_kayakLabelMap, m_expediaLabelMap, m_ebookersLabelMap, m_edreamsLabelMap;
  private Map<String, JToggleButton> m_airportGroupBtnMap;

  private JRadioButton m_normalSearch, m_combinedSearch, m_1xSearch, m_3xSearch;
  private ButtonGroup m_radioGroup;
  private List<String[]> m_1xCombinations, m_3xCombinations;

  private List<String> m_kayakRoots = new ArrayList<String>(Arrays.asList("com", "de", "nl", "it", "co.uk", "es", "fr", "pl")),
      m_expediaRoots = new ArrayList<String>(Arrays.asList("com", "de", "dk", "at", "nl", "it", "co.uk", "es", "fr", "ca", "ie", "be", "se")),
      m_ebookersRoots = new ArrayList<String>(Arrays.asList("com", "de", "nl", "fr", "at", "ie", "be")),
      m_edreamsRoots = new ArrayList<String>(Arrays.asList("com"));
  public static int[] c_columnWidths = {4, 10, 4, 10, 5, 10, 4, 10, 4, 10, 5, 10, 6, 2, 10};
  private String[] m_roshadaCodes = {
      "VIE", "BRU", "CRL", "ZAG", "MRS", "NCE", "ORY", "CDG", "FRA", "MUC", "BUD", "BLQ", "LIN", "MXP", "FCO", "CIA", "TSF",
      "VCE", "LJU", "BCN", "MAD", "VLC", "BRN", "GVA", "LUG", "ZRH", "EDI", "MAN", "LHR", "TRS", "DUS", "DUB"};

  private PriorityQueue<MultiCityFlightData> m_flightQueue;
  private Comparator<MultiCityFlightData> m_comparator;
  private ExecutorService m_executorService;
  private static final Logger LOG = Logger.getLogger(FlightsGui.class);

  //menu
  private JMenuBar m_menuBar;
  private JMenu m_mainMenu;
  private JFileChooser m_fileChooser;
  private JMenuItem m_menuItem;

  public FlightsGui()
      throws HeadlessException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, ParseException, IOException
  {
    super("Flights");
    this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    this.addWindowListener(this);
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    initLogging();

    m_mainTable = new MultiCityFlightTable(new MultiCityFlightTableModel(new ArrayList<MultiCityFlightData>()));
    Dimension size = new Dimension(1000, 800);
    m_mainTable.setPreferredScrollableViewportSize(size);
    m_mainTable.setFillsViewportHeight(true);
    m_sorter = new TableRowSorter<MultiCityFlightTableModel>((MultiCityFlightTableModel) m_mainTable.getModel());
    m_sorter.toggleSortOrder(MultiCityFlightTableModel.COL_PRICE);
    m_mainTable.setRowSorter(m_sorter);
    m_sorter.setSortsOnUpdates(true);
    JScrollPane scrollPane = new JScrollPane(m_mainTable);
    m_fromAP1 = new JComboBox();
    m_toAP1 = new JComboBox();
    m_fromAP2 = new JComboBox();
    m_toAP2 = new JComboBox();

    m_normalSearch = new JRadioButton("Normal");
    m_combinedSearch = new JRadioButton("Mix");
    m_1xSearch = new JRadioButton("1x");
    m_3xSearch = new JRadioButton("3x");

    MouseAdapter doubleClickML = new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2)
        {
          if (m_1xSearch.equals(e.getSource()))
          {
            m_1xCombinations = loadCombinations();
          }
          else if (m_3xSearch.equals(e.getSource()))
          {
            m_3xCombinations = loadCombinations();
          }
        }
      }
    };
    m_1xSearch.addMouseListener(doubleClickML);
    m_3xSearch.addMouseListener(doubleClickML);
    m_radioGroup = new ButtonGroup();
    m_radioGroup.add(m_normalSearch);
    m_radioGroup.add(m_combinedSearch);
    m_radioGroup.add(m_1xSearch);
    m_radioGroup.add(m_3xSearch);
    m_normalSearch.addActionListener(this);
    m_combinedSearch.addActionListener(this);
    m_1xSearch.addActionListener(this);
    m_3xSearch.addActionListener(this);
    m_normalSearch.setActionCommand(RADIO_CLICKED);
    m_combinedSearch.setActionCommand(RADIO_CLICKED);
    m_1xSearch.setActionCommand(RADIO_CLICKED);
    m_3xSearch.setActionCommand(RADIO_CLICKED);
    m_normalSearch.setSelected(true);

    Integer[] numOfPersons = {1, 2};
    m_numOfPersons = new JComboBox(numOfPersons);
    m_searchButton = new JButton("Search");

    m_kayakLabelMap = new HashMap<String, JLabel>();
    m_expediaLabelMap = new HashMap<String, JLabel>();
    m_ebookersLabelMap = new HashMap<String, JLabel>();
    m_edreamsLabelMap = new HashMap<String, JLabel>();
    m_kayakCBMap = new HashMap<String, JCheckBox>();
    m_expediaCBMap = new HashMap<String, JCheckBox>();
    m_ebookersCBMap = new HashMap<String, JCheckBox>();
    m_edreamsCBMap = new HashMap<String, JCheckBox>();

    fillCheckBoxMaps();

    m_showInEuro = new JCheckBox("€");

    m_searchButton.addActionListener(this);
    m_searchButton.setActionCommand(SEARCH);

    m_showInEuro.addActionListener(this);
    m_showInEuro.setActionCommand(CHECKBOX_CHANGED);
    m_showInEuro.setSelected(true);

    m_airportGroupBtnMap = new HashMap<String, JToggleButton>();
    for (String code : m_roshadaCodes)
    {
      m_airportGroupBtnMap.put(code, new JToggleButton(code));
    }

    Map<String, AirportData> airportMap = readAirportCsv();
    m_fromAP1.setEditable(true);
    m_toAP1.setEditable(true);
    m_fromAP2.setEditable(true);
    m_toAP2.setEditable(true);
    m_dateChooser = new DoubleDateChooser();
    m_dateXChooser = new JDateChooser();
    m_dateXChooser.setEnabled(false);
    m_dateXChooser.setDateFormatString("dd.MM.yyyy");

    SearchBoxModel sbm4 = new SearchBoxModel(m_toAP2, airportMap);
    JTextComponent toCombocTF2 = (JTextComponent) m_toAP2.getEditor().getEditorComponent();
    toCombocTF2.setDocument(new ComboDocument(m_dateChooser));
    toCombocTF2.addFocusListener(new SelectFocusAdapter(toCombocTF2));
    m_toAP2.setModel(sbm4);
    m_toAP2.addItemListener(sbm4);

    SearchBoxModel sbm3 = new SearchBoxModel(m_fromAP2, airportMap);
    JTextComponent fromComboxTF2 = (JTextComponent) m_fromAP2.getEditor().getEditorComponent();
    fromComboxTF2.setDocument(new ComboDocument(toCombocTF2));
    fromComboxTF2.addFocusListener(new SelectFocusAdapter(fromComboxTF2));
    m_fromAP2.setModel(sbm3);
    m_fromAP2.addItemListener(sbm3);

    SearchBoxModel sbm2 = new SearchBoxModel(m_toAP1, airportMap);
    JTextComponent toComboxTF1 = (JTextComponent) m_toAP1.getEditor().getEditorComponent();
    toComboxTF1.setDocument(new ComboDocument(fromComboxTF2));
    toComboxTF1.addFocusListener(new SelectFocusAdapter(toComboxTF1));
    m_toAP1.setModel(sbm2);
    m_toAP1.addItemListener(sbm2);

    SearchBoxModel sbm1 = new SearchBoxModel(m_fromAP1, airportMap);
    JTextComponent fromComboxTF1 = (JTextComponent) m_fromAP1.getEditor().getEditorComponent();
    fromComboxTF1.setDocument(new ComboDocument(toComboxTF1));
    fromComboxTF1.addFocusListener(new SelectFocusAdapter(fromComboxTF1));
    m_fromAP1.setModel(sbm1);
    m_fromAP1.addItemListener(sbm1);




    JPanel commandPanel = new JPanel(new GridBagLayout());
    int i = 0;
    commandPanel.add(m_fromAP1, new GridBagConstraints(i++, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_toAP1, new GridBagConstraints(i++, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_fromAP2, new GridBagConstraints(i++, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_toAP2, new GridBagConstraints(i++, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_dateChooser, new GridBagConstraints(i++, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_dateXChooser, new GridBagConstraints(i++, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_numOfPersons, new GridBagConstraints(i++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    commandPanel.add(m_showInEuro, new GridBagConstraints(i++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
    commandPanel.add(m_normalSearch, new GridBagConstraints(i++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
    commandPanel.add(m_combinedSearch, new GridBagConstraints(i++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
    commandPanel.add(m_1xSearch, new GridBagConstraints(i++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
    commandPanel.add(m_3xSearch, new GridBagConstraints(i++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
    commandPanel.add(m_searchButton, new GridBagConstraints(i, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.BOTH, new Insets(0, 20, 0, 0), 0, 0));

    JPanel groupPanel = new JPanel(new WrapLayout(FlowLayout.LEADING));
    groupPanel.setBorder(BorderFactory.createTitledBorder("Strani"));
    for (String root : m_kayakRoots)
    {
      groupPanel.add(m_kayakCBMap.get(root));
    }
    for (String root : m_expediaRoots)
    {
      groupPanel.add(m_expediaCBMap.get(root));
    }

    for (String root : m_ebookersRoots)
    {
      groupPanel.add(m_ebookersCBMap.get(root));
    }

    for (String root : m_edreamsRoots)
    {
      groupPanel.add(m_edreamsCBMap.get(root));
    }

    JPanel groupCodesPanel = new JPanel(new WrapLayout(FlowLayout.LEADING));
    groupCodesPanel.setBorder(BorderFactory.createTitledBorder("Letališča za rošado"));

    for (JToggleButton button : m_airportGroupBtnMap.values())
    {
      button.addActionListener(this);
      groupCodesPanel.add(button);
    }

    JPanel groupStatusPanel = new JPanel(new WrapLayout(FlowLayout.LEADING));
    groupStatusPanel.setBorder(BorderFactory.createTitledBorder("Status"));

    for (String root : m_kayakRoots)
    {
      groupStatusPanel.add(m_kayakLabelMap.get(root));
    }

    for (String root : m_expediaRoots)
    {
      groupStatusPanel.add(m_expediaLabelMap.get(root));
    }

    for (String root : m_ebookersRoots)
    {
      groupStatusPanel.add(m_ebookersLabelMap.get(root));
    }

    for (String root : m_edreamsRoots)
    {
      groupStatusPanel.add(m_edreamsLabelMap.get(root));
    }

    loadLastSavedGroups();

    JPanel panel = new JPanel(new GridBagLayout());
    panel.add(commandPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.add(groupCodesPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.add(groupPanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.add(scrollPane, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    panel.add(groupStatusPanel, new GridBagConstraints(0, 4, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
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
    setPrefferedComponentSize();
  }

  private void search(int selectedRadio)
  {
    int numOfSelected = numOfSelected();
    if (numOfSelected == 0)
    {
      return;
    }

    m_executorService = Executors.newFixedThreadPool(numOfSelected);
    m_flightQueue = new PriorityQueue<MultiCityFlightData>(QUEUE_SIZE, m_comparator);

    final String from = m_fromAP1.getSelectedIndex() >= 0 ? ((AirportData) m_fromAP1.getItemAt(m_fromAP1.getSelectedIndex())).getIataCode() : null;
    final String to = m_toAP2.getSelectedIndex() >= 0 ? ((AirportData) m_toAP2.getItemAt(m_toAP2.getSelectedIndex())).getIataCode() : null;

    final String toStatic = ((AirportData) m_toAP1.getItemAt(m_toAP1.getSelectedIndex())).getIataCode();
    final String fromStatic = ((AirportData) m_fromAP2.getItemAt(m_fromAP2.getSelectedIndex())).getIataCode();
    final Date fromDate = m_dateChooser.getFromDateChooser().getDate();
    final Date toDate = m_dateChooser.getToDateChooser().getDate();
    final Date fromXDate = m_dateXChooser.getDate();
    final Integer numOfPersons = (Integer) m_numOfPersons.getSelectedItem();

    if (fromStatic != null && fromStatic.length() == 3 && toStatic != null && toStatic.length() == 3)
    {
      for (String root : m_kayakRoots)
      {
        JLabel label = m_kayakLabelMap.get(root);
        if (m_kayakCBMap.get(root).isSelected())
        {
          m_executorService.execute(new SearchAndRefresh(
              new KayakFlightObtainer(),
              root,
              this,
              label,
              from,
              to,
              toStatic,
              fromStatic,
              fromDate,
              toDate,
              fromXDate,
              numOfPersons,
              selectedRadio,
              m_roshadaCodes,
              m_1xCombinations,
              m_3xCombinations)
          );
        }
        else
        {
          label.setForeground(Color.GRAY);
        }
      }

      for (String root : m_expediaRoots)
      {
        JLabel label = m_expediaLabelMap.get(root);
        if (m_expediaCBMap.get(root).isSelected())
        {
          m_executorService.execute(new SearchAndRefresh(
              new ExpediaFlightObtainer(),
              root,
              this,
              label,
              from,
              to,
              toStatic,
              fromStatic,
              fromDate,
              toDate,
              fromXDate,
              numOfPersons,
              selectedRadio,
              m_roshadaCodes,
              m_1xCombinations,
              m_3xCombinations)
          );
        }
        else
        {
          label.setForeground(Color.GRAY);
        }
      }

      for (String root : m_ebookersRoots)
      {
        JLabel label = m_ebookersLabelMap.get(root);
        if (m_ebookersCBMap.get(root).isSelected())
        {
          m_executorService.execute(new SearchAndRefresh(
              new EbookersFlightObtainer(),
              root,
              this,
              label,
              from,
              to,
              toStatic,
              fromStatic,
              fromDate,
              toDate,
              fromXDate,
              numOfPersons,
              selectedRadio,
              m_roshadaCodes,
              m_1xCombinations,
              m_3xCombinations)
          );
        }
        else
        {
          label.setForeground(Color.GRAY);
        }
      }

      for (String root : m_edreamsRoots)
      {
        JLabel label = m_edreamsLabelMap.get(root);
        if (m_edreamsCBMap.get(root).isSelected())
        {
          m_executorService.execute(new SearchAndRefresh(
              new EdreamsFlightObtainer(),
              root,
              this,
              label,
              from,
              to,
              toStatic,
              fromStatic,
              fromDate,
              toDate,
              fromXDate,
              numOfPersons,
              selectedRadio,
              m_roshadaCodes,
              m_1xCombinations,
              m_3xCombinations)
          );
        }
        else
        {
          label.setForeground(Color.GRAY);
        }
      }

      m_executorService.shutdown();
      new SwingWorker()
      {
        @Override
        protected Object doInBackground() throws Exception
        {
          try
          {
            m_executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            m_searchButton.setText("Search");
            m_searchButton.setActionCommand(SEARCH);
            resetCursor();
          }
          catch (InterruptedException ignored)
          {
          }
          return null;
        }
      }.execute();
    }
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    String action = e.getActionCommand();
    if (SEARCH.equals(action))
    {
      setHoldCursor();
      m_searchButton.setText("Stop");
      m_searchButton.setActionCommand(STOP_SEARCH);
      search(Helper.getSelectedButtonText(m_radioGroup));
    }
    else if (STOP_SEARCH.equals(action))
    {
      m_executorService.shutdownNow();
      new SwingWorker()
      {
        @Override
        protected Object doInBackground() throws Exception
        {
          try
          {
            m_executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            m_searchButton.setText("Search");
            m_searchButton.setActionCommand(SEARCH);
            resetCursor();
          }
          catch (InterruptedException ignored)
          {
          }
          return null;
        }
      }.execute();
    }
    else if (RADIO_CLICKED.equals(action))
    {
      if (e.getSource().equals(m_1xSearch))
      {
        m_dateXChooser.setEnabled(true);
        if (m_1xCombinations == null)
        {
          m_1xCombinations = loadCombinations();
        }
      }
      else if (e.getSource().equals(m_3xSearch))
      {
        m_dateXChooser.setEnabled(true);
        if (m_3xCombinations == null)
        {
          m_3xCombinations = loadCombinations();
        }
      }
      else
      {
        m_dateXChooser.setEnabled(false);
      }
    }
    else if (CHECKBOX_CHANGED.equals(action))
    {
      MultiCityFlightTableModel tableModel = (MultiCityFlightTableModel) m_mainTable.getModel();
      tableModel.setConvertToEuro(m_showInEuro.isSelected());
      tableModel.fireTableDataChanged();
    }
  }

  private List<String[]> loadCombinations()
  {
    int returnVal = m_fileChooser.showOpenDialog(FlightsGui.this);

    if (returnVal == JFileChooser.APPROVE_OPTION)
    {
      List<String[]> returnList = new ArrayList<String[]>();
      File file = m_fileChooser.getSelectedFile();
      if (file.exists())
      {
        setHoldCursor();
        try
        {
          returnList = readCvsPairFile(file.getAbsolutePath());
        }
        finally
        {
          resetCursor();
        }
      }
      return returnList;
    }
    return null;
  }

  private void setPrefferedComponentSize()
  {
    Dimension preferredSize = new Dimension(30, m_fromAP1.getSize().height);
    m_fromAP1.setPreferredSize(preferredSize);
    m_fromAP1.setMaximumSize(preferredSize);
    m_toAP1.setPreferredSize(preferredSize);
    m_toAP1.setMaximumSize(preferredSize);
    m_fromAP2.setPreferredSize(preferredSize);
    m_fromAP2.setMaximumSize(preferredSize);
    m_toAP2.setPreferredSize(preferredSize);
    m_toAP2.setMaximumSize(preferredSize);
    m_searchButton.setMaximumSize(new Dimension(50, m_searchButton.getSize().height));
  }

  private void fillCheckBoxMaps()
  {
    Border lineBorder = BorderFactory.createLineBorder(Color.BLACK);
    for (String root : m_kayakRoots)
    {
      JLabel label = new JLabel();
      String cbText = "www.kayak." + root;
      label.setBorder(BorderFactory.createTitledBorder(lineBorder, cbText));
      m_kayakLabelMap.put(root, label);
      JCheckBox checkBox = new JCheckBox(cbText);
      checkBox.setActionCommand(CHECKBOX_CHANGED);
      checkBox.setSelected(false);
      m_kayakCBMap.put(root, checkBox);
    }

    for (String root : m_expediaRoots)
    {
      JLabel label = new JLabel();
      String cbText = "www.expedia." + root;
      label.setBorder(BorderFactory.createTitledBorder(lineBorder, cbText));
      m_expediaLabelMap.put(root, label);
      JCheckBox checkBox = new JCheckBox(cbText);
      checkBox.setActionCommand(CHECKBOX_CHANGED);
      checkBox.setSelected(false);
      m_expediaCBMap.put(root, checkBox);
    }

    for (String root : m_ebookersRoots)
    {
      JLabel label = new JLabel();
      String cbText = "www.ebookers." + root;
      label.setBorder(BorderFactory.createTitledBorder(lineBorder, cbText));
      m_ebookersLabelMap.put(root, label);
      JCheckBox checkBox = new JCheckBox(cbText);
      checkBox.setActionCommand(CHECKBOX_CHANGED);
      checkBox.setSelected(false);
      m_ebookersCBMap.put(root, checkBox);
    }

    for (String root : m_edreamsRoots)
    {
      JLabel label = new JLabel();
      String cbText = "www.edreams." + root;
      label.setBorder(BorderFactory.createTitledBorder(lineBorder, cbText));
      m_edreamsLabelMap.put(root, label);
      JCheckBox checkBox = new JCheckBox(cbText);
      checkBox.setActionCommand(CHECKBOX_CHANGED);
      checkBox.setSelected(true);
      m_edreamsCBMap.put(root, checkBox);
    }
  }

  private void loadLastSavedGroups() throws IOException
  {
    File settingsFile = getSettingsFile();
    if (settingsFile != null && settingsFile.exists() && settingsFile.isFile())
    {
      BufferedReader br = new BufferedReader(new FileReader(settingsFile));
      try
      {
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null)
        {
          sb.append(line);
          line = br.readLine();
        }
        String everything = sb.toString();
        for (int i = 0; i < everything.length(); i += 3)
        {
          try
          {
            m_airportGroupBtnMap.get(everything.substring(i, i + 3)).setSelected(true);
          }
          catch (Exception e)
          {
            LOG.error("FlightsGui: error substring!", e);
          }
        }
      }
      catch (IOException e)
      {
        LOG.error("FlightsGui: error reading from file!", e);
      }
      finally
      {
        br.close();
      }
    }
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

    m_menuItem = new JMenuItem("Close", KeyEvent.VK_C);
    m_menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
    m_menuItem.setActionCommand("MENU.CLOSE");
    m_menuItem.addActionListener(this);
    m_mainMenu.add(m_menuItem);
    //    setJMenuBar(m_menuBar);
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
      LOG.error("FlightsGui: error reading from file!", e);
    }
  }

  private List<String[]> readCvsPairFile(String absolutePath)
  {
    ArrayList<String[]> stringPairs = new ArrayList<String[]>();
    try
    {
      CsvReader reader = new CsvReader(new FileReader(absolutePath));
      reader.setDelimiter(',');
      try
      {
        while (reader.readRecord())
        {
          String first = reader.get(0);
          String second = reader.get(1);
          if (first.length() != 3 || second.length() != 3)
          {
            throw new IllegalStateException("Datoteka ni pravega formata!");
          }
          stringPairs.add(new String[]{first, second});
        }
      }
      finally
      {
        reader.close();
      }
    }
    catch (FileNotFoundException e)
    {
      LOG.error("FlightsGui: error reading from file!", e);
    }
    catch (IOException e)
    {
      LOG.error("FlightsGui: error reading from file!", e);
    }
    catch (Exception e)
    {
      LOG.error("FlightsGui: error reading from file!", e);
    }

    return stringPairs;
  }

  private int numOfSelected()
  {
    int count = 0;
    for (String root : m_kayakRoots)
    {
      if (m_kayakCBMap.get(root).isSelected())
      {
        count++;
      }
    }

    for (String root : m_expediaRoots)
    {
      if (m_expediaCBMap.get(root).isSelected())
      {
        count++;
      }
    }

    for (String root : m_ebookersRoots)
    {
      if (m_ebookersCBMap.get(root).isSelected())
      {
        count++;
      }
    }

    for (String root : m_edreamsRoots)
    {
      if (m_edreamsCBMap.get(root).isSelected())
      {
        count++;
      }
    }
    return count;
  }

  public MultiCityFlightTable getMainTable()
  {
    return m_mainTable;
  }

  public PriorityQueue<MultiCityFlightData> getFlightQueue()
  {
    return m_flightQueue;
  }

  public Map<String, JToggleButton> getAirportGroupBtnMap()
  {
    return m_airportGroupBtnMap;
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
      LOG.error("FlightsGui: error reading from file!", e);
    }
    catch (IOException e)
    {
      LOG.error("FlightsGui: error reading from file!", e);
    }
    catch (Exception e)
    {
      LOG.error("FlightsGui: error reading from file!", e);
    }
    return null;
  }

  public void setHoldCursor()
  {
    m_mainTable.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  public void resetCursor()
  {
    m_mainTable.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  @Override
  public void windowOpened(WindowEvent e)
  {
  }

  @Override
  public void windowClosing(WindowEvent e)
  {
    try
    {
      FileWriter fw = new FileWriter(getSettingsFile());
      try
      {
        BufferedWriter bw = new BufferedWriter(fw);
        try
        {
          for (JToggleButton button : m_airportGroupBtnMap.values())
          {
            if (button.isSelected())
            {
              bw.write(button.getText());
            }
          }
        }
        finally
        {
          bw.close();
        }
      }
      finally
      {
        fw.close();
      }
    }
    catch (IOException e1)
    {
      e1.printStackTrace();
    }
  }

  @Override
  public void windowClosed(WindowEvent e)
  {
  }

  @Override
  public void windowIconified(WindowEvent e)
  {
  }

  @Override
  public void windowDeiconified(WindowEvent e)
  {
  }

  @Override
  public void windowActivated(WindowEvent e)
  {
  }

  @Override
  public void windowDeactivated(WindowEvent e)
  {
  }

  public File getSettingsFile()
  {
    String userHome = System.getProperty("user.home");
    if (userHome == null)
    {
      throw new IllegalStateException("user.home==null");
    }
    File home = new File(userHome);
    File settingsDirectory = new File(home, ".flights");
    if (!settingsDirectory.exists())
    {
      if (!settingsDirectory.mkdir())
      {
        throw new IllegalStateException(settingsDirectory.toString());
      }
    }
    return new File(settingsDirectory, "settings.ini");
  }

  private void initLogging()
  {
    URL url = Thread.currentThread().getContextClassLoader().getResource("config/log4j.properties");
    PropertyConfigurator.configure(url);
  }
}
