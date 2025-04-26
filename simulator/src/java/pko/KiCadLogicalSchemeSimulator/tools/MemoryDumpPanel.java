package pko.KiCadLogicalSchemeSimulator.tools;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import static pko.KiCadLogicalSchemeSimulator.tools.UiTools.loadBase64Image;
import static pko.KiCadLogicalSchemeSimulator.tools.UiTools.refreshIconBase64;

public class MemoryDumpPanel extends JPanel {
    private static final int COLS = 16;
    private static final int CELL_WIDTH = 20;
    private static final int CELL_HEIGHT = 15;
    private static final int ADDR_WIDTH = 35;
    private final int[] words;

    public MemoryDumpPanel(int[] words) {
        super(new BorderLayout());
        this.words = words;
        // Get screen height and calculate max panel height (allowing space for taskbar)
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        int maxPanelHeight = screenHeight - 150;
        // Create table with an extra column for address labels
        String[] columnNames = new String[COLS + 1]; // Extra column for address
        columnNames[0] = "Address";  // Address column header
        for (int i = 0; i < COLS; i++) {
            columnNames[i + 1] = String.format("%X", i);  // Column headers: 0x0, 0x1, ..., 0xF
        }
        // Calculate number of rows
        int rows = (words.length + COLS - 1) / COLS;
        Object[][] data = new Object[rows][COLS + 1]; // Extra space for address column
        // Initialize data for table, including the address
        for (int i = 0; i < rows; i++) {
            data[i][0] = String.format("%04X", i * COLS);  // Address column
            for (int j = 0; j < COLS; j++) {
                int index = i * COLS + j;
                if (index < words.length) {
                    data[i][j + 1] = String.format("%02X", words[index]);
                } else {
                    data[i][j + 1] = "";  // Empty for non-existent memory addresses
                }
            }
        }
        // Set up table model and table
        DefaultTableModel model = new DefaultTableModel(data, columnNames);
        JTable table = getJTable(model, rows);
        // Use JScrollPane for scrolling
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(ADDR_WIDTH + COLS * (CELL_WIDTH + 3), Math.min(maxPanelHeight, 200 + table.getRowCount() * CELL_HEIGHT)));
        add(scrollPane, BorderLayout.CENTER);
        // ** Refresh Button **
        JButton refresh = new JButton();
        refresh.setIcon(loadBase64Image(refreshIconBase64()));
        add(refresh, BorderLayout.SOUTH);
        // Refresh button action to update data (simulate a memory dump update)
        refresh.addActionListener(e -> updateMemoryDump(model));
    }

    private static JTable getJTable(DefaultTableModel model, int rows) {
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;  // Make cells non-editable
            }
        };
        // Set table properties
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.setRowHeight(CELL_HEIGHT);
        table.setPreferredScrollableViewportSize(new Dimension(ADDR_WIDTH + (COLS + 1) * CELL_WIDTH, rows * CELL_HEIGHT));
        table.setFillsViewportHeight(true);
        // Set the first column (address column) to have a fixed width
        table.getColumnModel().getColumn(0).setPreferredWidth(ADDR_WIDTH); // Set preferred width
        table.getColumnModel().getColumn(0).setMinWidth(ADDR_WIDTH); // Set minimum width to ensure enough space
        table.getColumnModel().getColumn(0).setMaxWidth(ADDR_WIDTH); // Optionally, you can set a max width to prevent resizing
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    private void updateMemoryDump(DefaultTableModel model) {
        // Simulate an update: Here you can adjust the words as needed.
        // For example, you can modify the memory dump or simply refresh it.
        int rows = (words.length + COLS - 1) / COLS;
        Object[][] data = new Object[rows][COLS + 1]; // Extra space for address column
        for (int i = 0; i < rows; i++) {
            data[i][0] = String.format("%04X", i * COLS);  // Address column
            for (int j = 0; j < COLS; j++) {
                int index = i * COLS + j;
                if (index < words.length) {
                    data[i][j + 1] = String.format("%02X", words[index]);
                } else {
                    data[i][j + 1] = "";
                }
            }
        }
        // Update the table data
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < COLS + 1; j++) {
                model.setValueAt(data[i][j], i, j);
            }
        }
    }
}
