package si.nerve.flightchecker;

import com.csvreader.CsvReader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import si.nerve.flightchecker.components.*;
import si.nerve.flightchecker.data.AirportData;
import si.nerve.flightchecker.data.MultiCityFlightData;
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
  public static final int QUEUE_SIZE = 50;
  private MultiCityFlightTable m_mainTable;
  public TableRowSorter<MultiCityFlightTableModel> m_sorter;
  private JComboBox m_fromAP1, m_toAP1, m_fromAP2, m_toAP2;
  private JButton m_searchButton;
  private DoubleDateChooser m_dateChooser;
  private JCheckBox m_combined;
  private JCheckBox m_kayakNl, m_kayakCom, m_kayakDe, m_kayakIt, m_kayakCoUk, m_kayakEs, m_kayakFr, m_kayakPl,
      m_expediaCom, m_expediaDe, m_expediaEs, m_expediaFr, m_expediaIt;
  private JLabel m_kayakNlStatusLabel, m_kayakComStatusLabel, m_kayakDeStatusLabel, m_kayakItStatusLabel, m_kayakCoUkStatusLabel, m_kayakEsStatusLabel, m_kayakFrStatusLabel, m_kayakPlStatusLabel,
      m_expediaComStatusLabel, m_expediaDeStatusLabel, m_expediaEsStatusLabel, m_expediaFrStatusLabel, m_expediaItStatusLabel;
  private JCheckBox m_showInEuro;
  public static int[] c_columnWidths = {4, 10, 4, 10, 5, 10, 4, 10, 4, 10, 5, 10, 6, 2, 10};
  private String[] m_codes = {
      "VIE", "BRU", "CRL", "ZAG", "MRS", "NCE", "ORY", "CDG", "FRA", "MUC", "BUD", "BLQ", "LIN", "MXP", "FCO", "CIA", "TSF",
      "VCE", "LJU", "BCN", "MAD", "VLC", "BRN", "GVA", "LUG", "ZRH", "EDI", "MAN", "LHR"};
  private Map<String, JToggleButton> m_airportGroupBtnMap;
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
    m_mainTable.setPreferredScrollableViewportSize(new Dimension(1300, 800));
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

    m_searchButton = new JButton("Išči");

    m_kayakNlStatusLabel = new JLabel();
    m_kayakComStatusLabel = new JLabel();
    m_kayakDeStatusLabel = new JLabel();
    m_kayakItStatusLabel = new JLabel();
    m_kayakCoUkStatusLabel = new JLabel();
    m_kayakEsStatusLabel = new JLabel();
    m_kayakFrStatusLabel = new JLabel();
    m_kayakPlStatusLabel = new JLabel();
    m_expediaComStatusLabel = new JLabel();
    m_expediaDeStatusLabel = new JLabel();
    m_expediaFrStatusLabel = new JLabel();
    m_expediaEsStatusLabel = new JLabel();
    m_expediaItStatusLabel = new JLabel();

    m_kayakPl = new JCheckBox("www.kayak.pl");
    m_kayakNl = new JCheckBox("www.kayak.nl");
    m_kayakCom = new JCheckBox("www.kayak.com");
    m_kayakDe = new JCheckBox("www.kayak.de");
    m_kayakIt = new JCheckBox("www.kayak.it");
    m_kayakCoUk = new JCheckBox("www.kayak.co.uk");
    m_kayakEs = new JCheckBox("www.kayak.es");
    m_kayakFr = new JCheckBox("www.kayak.fr");
    m_expediaCom = new JCheckBox("www.expedia.com");
    m_expediaDe = new JCheckBox("www.expedia.de");
    m_expediaEs = new JCheckBox("www.expedia.es");
    m_expediaFr = new JCheckBox("www.expedia.fr");
    m_expediaIt = new JCheckBox("www.expedia.it");

    Border lineBorder = BorderFactory.createLineBorder(Color.BLACK);
    m_kayakNlStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_kayakNl.getText()));
    m_kayakComStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_kayakCom.getText()));
    m_kayakDeStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_kayakDe.getText()));
    m_kayakItStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_kayakIt.getText()));
    m_kayakCoUkStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_kayakCoUk.getText()));
    m_kayakEsStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_kayakEs.getText()));
    m_kayakFrStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_kayakFr.getText()));
    m_kayakPlStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_kayakPl.getText()));
    m_expediaComStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_expediaCom.getText()));
    m_expediaDeStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_expediaCom.getText()));
    m_expediaFrStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_expediaCom.getText()));
    m_expediaEsStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_expediaCom.getText()));
    m_expediaItStatusLabel.setBorder(BorderFactory.createTitledBorder(lineBorder, m_expediaCom.getText()));

    m_combined = new JCheckBox("Rošada");
    m_showInEuro = new JCheckBox("€");

    m_kayakPl.setActionCommand(CHECKBOX_CHANGED);
    m_kayakNl.setActionCommand(CHECKBOX_CHANGED);
    m_kayakCom.setActionCommand(CHECKBOX_CHANGED);
    m_kayakDe.setActionCommand(CHECKBOX_CHANGED);
    m_kayakIt.setActionCommand(CHECKBOX_CHANGED);
    m_kayakCoUk.setActionCommand(CHECKBOX_CHANGED);
    m_kayakEs.setActionCommand(CHECKBOX_CHANGED);
    m_kayakFr.setActionCommand(CHECKBOX_CHANGED);
    m_expediaCom.setActionCommand(CHECKBOX_CHANGED);
    m_expediaDe.setActionCommand(CHECKBOX_CHANGED);
    m_expediaEs.setActionCommand(CHECKBOX_CHANGED);
    m_expediaFr.setActionCommand(CHECKBOX_CHANGED);
    m_expediaIt.setActionCommand(CHECKBOX_CHANGED);

    m_kayakPl.setSelected(false);
    m_kayakNl.setSelected(false);
    m_kayakCom.setSelected(false);
    m_kayakDe.setSelected(false);
    m_kayakIt.setSelected(false);
    m_kayakCoUk.setSelected(false);
    m_kayakEs.setSelected(false);
    m_kayakFr.setSelected(false);
    m_expediaCom.setSelected(true);
    m_expediaDe.setSelected(true);
    m_expediaEs.setSelected(true);
    m_expediaFr.setSelected(true);
    m_expediaIt.setSelected(true);

    m_searchButton.addActionListener(this);
    m_searchButton.setActionCommand(SEARCH);

    m_showInEuro.addActionListener(this);
    m_showInEuro.setActionCommand(CHECKBOX_CHANGED);
    m_showInEuro.setSelected(true);

    m_airportGroupBtnMap = new HashMap<String, JToggleButton>();
    for (String code : m_codes)
    {
      m_airportGroupBtnMap.put(code, new JToggleButton(code));
    }

    Map<String, AirportData> airportMap = readAirportCsv();
    m_fromAP1.setEditable(true);
    m_toAP1.setEditable(true);
    m_fromAP2.setEditable(true);
    m_toAP2.setEditable(true);
    m_dateChooser = new DoubleDateChooser();

    SearchBoxModel sbm1 = new SearchBoxModel(m_fromAP1, airportMap);
    JTextComponent fromComboxTF1 = (JTextComponent) m_fromAP1.getEditor().getEditorComponent();
    fromComboxTF1.setDocument(new ComboDocument());
    fromComboxTF1.addFocusListener(new SelectFocusAdapter(fromComboxTF1));
    m_fromAP1.setModel(sbm1);
    m_fromAP1.addItemListener(sbm1);
    SearchBoxModel sbm2 = new SearchBoxModel(m_toAP1, airportMap);
    JTextComponent toComboxTF1 = (JTextComponent) m_toAP1.getEditor().getEditorComponent();
    toComboxTF1.setDocument(new ComboDocument());
    toComboxTF1.addFocusListener(new SelectFocusAdapter(toComboxTF1));
    m_toAP1.setModel(sbm2);
    m_toAP1.addItemListener(sbm2);
    SearchBoxModel sbm3 = new SearchBoxModel(m_fromAP2, airportMap);
    JTextComponent fromComboxTF2 = (JTextComponent) m_fromAP2.getEditor().getEditorComponent();
    fromComboxTF2.setDocument(new ComboDocument());
    fromComboxTF2.addFocusListener(new SelectFocusAdapter(fromComboxTF2));
    m_fromAP2.setModel(sbm3);
    m_fromAP2.addItemListener(sbm3);
    SearchBoxModel sbm4 = new SearchBoxModel(m_toAP2, airportMap);
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

    JPanel groupPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    groupPanel.setBorder(BorderFactory.createTitledBorder("Strani"));
    groupPanel.add(m_kayakDe);
    groupPanel.add(m_kayakIt);
    groupPanel.add(m_kayakCom);
    groupPanel.add(m_kayakCoUk);
    groupPanel.add(m_kayakEs);
    groupPanel.add(m_kayakFr);
    groupPanel.add(m_kayakNl);
    groupPanel.add(m_kayakPl);
    groupPanel.add(m_expediaCom);
    groupPanel.add(m_expediaDe);
    groupPanel.add(m_expediaEs);
    groupPanel.add(m_expediaFr);
    groupPanel.add(m_expediaIt);

    JPanel groupCodesPanel = new JPanel(new WrapLayout(FlowLayout.LEADING));
    groupCodesPanel.setBorder(BorderFactory.createTitledBorder("Letališča za rošado"));

    for (JToggleButton button : m_airportGroupBtnMap.values())
    {
      button.addActionListener(this);
      groupCodesPanel.add(button);
    }

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

    JPanel groupStatusPanel = new JPanel(new GridBagLayout());
    groupStatusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
    groupStatusPanel.add(m_kayakDeStatusLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_kayakItStatusLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_kayakComStatusLabel, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_kayakCoUkStatusLabel, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_kayakEsStatusLabel, new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_kayakFrStatusLabel, new GridBagConstraints(5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_kayakNlStatusLabel, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_kayakPlStatusLabel, new GridBagConstraints(7, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_expediaComStatusLabel, new GridBagConstraints(8, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_expediaDeStatusLabel, new GridBagConstraints(9, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_expediaEsStatusLabel, new GridBagConstraints(10, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_expediaFrStatusLabel, new GridBagConstraints(11, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    groupStatusPanel.add(m_expediaItStatusLabel, new GridBagConstraints(12, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

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
      m_searchButton.setText("Stop");
      m_searchButton.setActionCommand(STOP_SEARCH);
      search(m_combined.isSelected());
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
            m_searchButton.setText("Išči");
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
      LOG.error("FlightsGui: error reading from file!", e);
    }
  }

  private PriorityQueue<MultiCityFlightData> readFlightsFile(String absolutePath)
  {
    return new PriorityQueue<MultiCityFlightData>(QUEUE_SIZE, m_comparator);
  }

  private void search(boolean combined)
  {
    if (nothingSelected())
    {
      return;
    }

    final String from = m_fromAP1.getSelectedIndex() >= 0 ? ((AirportData) m_fromAP1.getItemAt(m_fromAP1.getSelectedIndex())).getIataCode() : null;
    final String to = m_toAP2.getSelectedIndex() >= 0 ? ((AirportData) m_toAP2.getItemAt(m_toAP2.getSelectedIndex())).getIataCode() : null;

    m_executorService = Executors.newFixedThreadPool(7);
    final String toStatic = ((AirportData) m_toAP1.getItemAt(m_toAP1.getSelectedIndex())).getIataCode();
    final String fromStatic = ((AirportData) m_fromAP2.getItemAt(m_fromAP2.getSelectedIndex())).getIataCode();
    final Date fromDate = m_dateChooser.getFromDateChooser().getDate();
    final Date toDate = m_dateChooser.getToDateChooser().getDate();

    m_flightQueue = new PriorityQueue<MultiCityFlightData>(QUEUE_SIZE, m_comparator);
    if (fromStatic != null && fromStatic.length() == 3 && toStatic != null && toStatic.length() == 3)
    {
      if (m_kayakCom.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new KayakFlightObtainer(), "com", this, m_kayakComStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_kayakComStatusLabel.setForeground(Color.GRAY);
      }
      if (m_kayakDe.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new KayakFlightObtainer(), "de", this, m_kayakDeStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_kayakDeStatusLabel.setForeground(Color.GRAY);
      }
      if (m_kayakIt.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new KayakFlightObtainer(), "it", this, m_kayakItStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_kayakItStatusLabel.setForeground(Color.GRAY);
      }
      if (m_kayakCoUk.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new KayakFlightObtainer(), "co.uk", this, m_kayakCoUkStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_kayakCoUkStatusLabel.setForeground(Color.GRAY);
      }
      if (m_kayakEs.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new KayakFlightObtainer(), "es", this, m_kayakEsStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_kayakEsStatusLabel.setForeground(Color.GRAY);
      }
      if (m_kayakFr.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new KayakFlightObtainer(), "fr", this, m_kayakFrStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_kayakFrStatusLabel.setForeground(Color.GRAY);
      }
      if (m_kayakNl.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new KayakFlightObtainer(), "nl", this, m_kayakNlStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_kayakNlStatusLabel.setForeground(Color.GRAY);
      }
      if (m_kayakPl.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new KayakFlightObtainer(), "pl", this, m_kayakPlStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_kayakPlStatusLabel.setForeground(Color.GRAY);
      }
      if (m_expediaCom.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new ExpediaFlightObtainer(), "com", this, m_expediaComStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_expediaComStatusLabel.setForeground(Color.GRAY);
      }
      if (m_expediaDe.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new ExpediaFlightObtainer(), "de", this, m_expediaComStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_expediaDeStatusLabel.setForeground(Color.GRAY);
      }
      if (m_expediaEs.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new ExpediaFlightObtainer(), "es", this, m_expediaComStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_expediaEsStatusLabel.setForeground(Color.GRAY);
      }
      if (m_expediaFr.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new ExpediaFlightObtainer(), "fr", this, m_expediaComStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_expediaFrStatusLabel.setForeground(Color.GRAY);
      }
      if (m_expediaIt.isSelected())
      {
        m_executorService.execute(new SearchAndRefresh(new ExpediaFlightObtainer(), "it", this, m_expediaComStatusLabel, from, to, toStatic, fromStatic, fromDate, toDate, combined, m_codes));
      }
      else
      {
        m_expediaItStatusLabel.setForeground(Color.GRAY);
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
            m_searchButton.setText("Išči");
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

  private boolean nothingSelected()
  {
    return !m_kayakCom.isSelected() && !m_kayakCoUk.isSelected() && !m_kayakDe.isSelected() && !m_kayakEs.isSelected()
        && !m_kayakFr.isSelected() && !m_kayakIt.isSelected() && !m_kayakNl.isSelected() && !m_kayakPl.isSelected()
        && !m_expediaCom.isSelected() && !m_expediaDe.isSelected() && !m_expediaEs.isSelected() && !m_expediaFr.isSelected() && !m_expediaIt.isSelected();
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
    LOG.info("Flights Logger configured.");
  }
}
