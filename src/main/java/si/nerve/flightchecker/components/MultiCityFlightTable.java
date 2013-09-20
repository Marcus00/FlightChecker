package si.nerve.flightchecker.components;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.TableModel;

import si.nerve.flightchecker.data.MultiCityFlightData;

/**
 * @author bratwurzt
 */
public class MultiCityFlightTable extends JTable
{
  public JPopupMenu m_popupMenu;
  private static JEditorPane m_popupEditorPane;

  public MultiCityFlightTable(TableModel dm)
  {
    super(dm);
    this.addMouseMotionListener(new MouseMotionAdapter()
    {
      int lastRow = Integer.MAX_VALUE;
      @Override
      public void mouseMoved(MouseEvent e)
      {
        super.mouseMoved(e);
        if (e.getSource() instanceof MultiCityFlightTable)
        {
          Point point = e.getPoint();
          JTable table = (JTable)e.getSource();
          int column = table.columnAtPoint(point);
          int row = table.rowAtPoint(point);

          if (row > -1 && row != lastRow)
          {
            List<MultiCityFlightData> entityList = ((MultiCityFlightTableModel)table.getModel()).getEntityList();
            if (entityList.size() > 0)
            {
              row = table.convertRowIndexToModel(row);
              MultiCityFlightData data = entityList.get(row);
              if (column == MultiCityFlightTableModel.COL_ADDRESS)
              {
                String link = (String) getModel().getValueAt(row, column);
                String linkName = data.getPopupLinkName();
                m_popupEditorPane.setText("<html><a href=\"" + link + "\">" + linkName + "</a></html>");
                showPopup(e);
              }
              else
              {
                m_popupMenu.setVisible(false);
              }
              lastRow = row;
            }
          }
        }
      }

      private void showPopup(MouseEvent e)
      {
        Component component = e.getComponent();
        m_popupMenu.show(component, e.getX() - m_popupMenu.getSize().width, e.getY());
        m_popupMenu.setVisible(true);
      }
    });

    m_popupMenu = new JPopupMenu();
    m_popupEditorPane = new JEditorPane();
    HyperlinkListener listener = new HyperlinkListener()
    {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e)
      {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
          open(e.getURL());
        }
      }
    };
    m_popupEditorPane.setEditable(false);
    m_popupEditorPane.setContentType("text/html");
    m_popupEditorPane.addHyperlinkListener(listener);
    m_popupMenu.add(m_popupEditorPane);
  }

  private void open(URL url)
  {
    if (Desktop.isDesktopSupported())
    {
      try
      {
        URI uri = url.toURI();
        Desktop desktop = Desktop.getDesktop();
        desktop.browse(uri);
      }
      catch (URISyntaxException ignored)
      {
      }
      catch (IOException ignored)
      {
      }
    }
  }

  public void setColumnWidths(int[] nRelativeColumnWidths)
  {
    int nCount = columnModel.getColumnCount();
    int nRelativeColumnWidthsSum = 0;
    for (int i = 0; i < nCount; i++)
    {
      nRelativeColumnWidthsSum += nRelativeColumnWidths[i];
    }

    int nWidth = getWidth();
    for (int i = 0; i < nCount; i++)
    {
      int nColumnWidth = nWidth * nRelativeColumnWidths[i] / nRelativeColumnWidthsSum;
      columnModel.getColumn(i).setPreferredWidth(nColumnWidth);
      nWidth -= nColumnWidth;
      nRelativeColumnWidthsSum -= nRelativeColumnWidths[i];
    }

    sizeColumnsToFit(-1);
  }


}
