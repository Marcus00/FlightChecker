package si.nerve.flightchecker.components;

import si.nerve.flightchecker.data.AirportData;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author bratwurzt
 */
public class SearchBoxModel extends AbstractListModel implements ComboBoxModel, KeyListener, ItemListener
{
  private static final long serialVersionUID = 9050594122953046884L;
  private Map<String, AirportData> db;
  private List<String> data;
  private AirportData selection;
  private JComboBox cb;
  private ComboBoxEditor cbe;

  public SearchBoxModel(JComboBox jcb, Map<String, AirportData> airportDataMap)
  {
    cb = jcb;
    cbe = jcb.getEditor();
    //here we add the key listener to the text field that the combobox is wrapped around
    cbe.getEditorComponent().addKeyListener(this);

    //set up our "database" of items - in practice you will usuallu have a proper db.
    db = airportDataMap;
    data = new ArrayList<String>();
  }

  public void updateModel(String in)
  {
    data.clear();
    //lets find any items which start with the string the user typed, and add it to the popup list
    //here you would usually get your items from a database, or some other storage...
    for (Map.Entry<String, AirportData> s : db.entrySet())
    {
      String key = s.getKey();
      if (key.startsWith(in))
      {
        data.add(key);
      }
    }

    super.fireContentsChanged(this, 0, data.size());

    //this is a hack to search around redraw problems when changing the list length of the displayed popups
    cb.hidePopup();
    cb.showPopup();
    if (data.size() != 0)
    {
      cb.setSelectedIndex(0);
    }
  }

  public int getSize()
  {
    return data.size();
  }

  public Object getElementAt(int index)
  {
    return db.get(data.get(index));
  }

  public void setSelectedItem(Object anItem)
  {
    if (anItem instanceof AirportData)
    {
      selection = (AirportData) anItem;
    }
  }

  public Object getSelectedItem()
  {
    return selection;
  }

  public void keyTyped(KeyEvent e)
  {
  }

  public void keyPressed(KeyEvent e)
  {
  }

  public void keyReleased(KeyEvent e)
  {
    String str = cbe.getItem().toString();
    JTextField jtf = (JTextField) cbe.getEditorComponent();
    int currPos = jtf.getCaretPosition();

    if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED)
    {
      if (e.getKeyCode() != KeyEvent.VK_ENTER)
      {
        cbe.setItem(str);
        //jtf.setCaretPosition(currPos);
      }
    }
    else if (e.getKeyCode() == KeyEvent.VK_ENTER)
    {
      cb.setSelectedIndex(cb.getSelectedIndex());
      cbe.setItem(((AirportData) cb.getSelectedItem()).getIataCode());
    }
    else
    {
      updateModel(cb.getEditor().getItem().toString());
      cbe.setItem(str);
      //jtf.setCaretPosition(currPos);
    }
  }

  public void itemStateChanged(ItemEvent e)
  {
    cbe.setItem(e.getItem().toString());
    cb.setSelectedItem(e.getItem());
  }
}