package org.benasin;


import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class MyTableModel extends AbstractTableModel
{
    private final ArrayList<EmptyStringResult> log;

    public MyTableModel()
    {
        this.log = new ArrayList<>();
    }

    @Override
    public synchronized int getRowCount()
    {
        return log.size();
    }

    @Override
    public int getColumnCount()
    {
        return 5;
    }

    @Override
    public String getColumnName(int column)
    {
        return switch (column)
                {
                    case 0 -> "#";
                    case 1 -> "URL";
                    case 2 -> "Empty String Type";
                    case 3 -> "Potential Hidden Param";
                    case 4 -> "Reflected?";
                    default -> "";
                };
    }

    @Override
    public synchronized Object getValueAt(int rowIndex, int columnIndex)
    {
        EmptyStringResult emptyStringResult = log.get(rowIndex);

        return switch (columnIndex)
                {
                    case 0 -> emptyStringResult.getId();
                    case 1 -> emptyStringResult.getBaseRequestResponse().url();
                    case 2 -> emptyStringResult.getType();
                    case 3 -> emptyStringResult.getHiddenParam();
                    case 4 -> emptyStringResult.isReflected() ? "Yes" : "";
                    default -> "";
                };
    }

    public synchronized void add(EmptyStringResult emptyStringResult)
    {
        int index = log.size();
        log.add(emptyStringResult);
        fireTableRowsInserted(index, index);
    }

    public synchronized EmptyStringResult get(int rowIndex)
    {
        return log.get(rowIndex);
    }
    public synchronized void remove(ArrayList<Integer> selectedIds)
    {
        for(int i : selectedIds) {
            log.removeIf(o -> o.getId() == i);
        }
        fireTableDataChanged();
    }
}
