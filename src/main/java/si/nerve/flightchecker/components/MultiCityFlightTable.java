package si.nerve.flightchecker.components;

import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * @author bratwurzt
 */
public class MultiCityFlightTable extends JTable
{
  public MultiCityFlightTable(TableModel dm)
  {
    super(dm);
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
