package org.benasin;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static burp.api.montoya.ui.editor.EditorOptions.READ_ONLY;

public class EmptyStringScannerEntry implements BurpExtension
{
    private MontoyaApi api;
    private JTable table;
    private MyTableModel tableModel;
    private EmptyStringScanner emptyStringScanner;
    @Override
    public void initialize(MontoyaApi api)
    {
        this.api = api;
        api.extension().setName("Empty String Scanner");

        tableModel = new MyTableModel();
        api.userInterface().registerSuiteTab("Empty String Scanner", constructLoggerTab(tableModel));
        emptyStringScanner = new EmptyStringScanner(api,tableModel);
        api.scanner().registerScanCheck(emptyStringScanner);
        api.logging().logToOutput("Empty String Scanner by Benasin loaded.");
    }

    private Component constructLoggerTab(MyTableModel tableModel)
    {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create a toggle button
        JToggleButton toggleButton = new JToggleButton("In-Scope Requests Only");
        toggleButton.setSelected(false); // Set initial state
        toggleButton.addActionListener(l -> {
            emptyStringScanner.toggleLogInScope();
        });

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; // Makes the toggleButton expand horizontally

        topPanel.add(toggleButton, gbc);

        // Create a "Copy to Clipboard" button
        JButton copyToClipboardButton = new JButton("Copy Hidden Params to Clipboard");
        copyToClipboardButton.addActionListener(l -> {
            copyHiddenParamsToClipboard();
        });

        gbc.weightx = 0.0; // Reset the weight to avoid expansion
        gbc.gridy = 1; // Move to the second row
        topPanel.add(copyToClipboardButton, gbc);

        mainPanel.add(topPanel, BorderLayout.NORTH); // Add the button under the toggle button

        // main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // tabs with request/response viewers
        JTabbedPane tabs = new JTabbedPane();

        UserInterface userInterface = api.userInterface();

        HttpRequestEditor requestViewer = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor responseViewer = userInterface.createHttpResponseEditor(READ_ONLY);

        tabs.addTab("Request", requestViewer.uiComponent());
        tabs.addTab("Response", responseViewer.uiComponent());

        splitPane.setRightComponent(tabs);

        // table of log entries
        table = new JTable(tableModel)
        {
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend)
            {
                // show the log entry for the selected row

                EmptyStringResult emptyStringResult = tableModel.get(rowIndex);
                HttpRequestResponse baseRequestResponse = emptyStringResult.getBaseRequestResponse();

                requestViewer.setRequest(baseRequestResponse.request());

                String highlight;
                if (emptyStringResult.isReflected()) {
                    highlight = "reflectedxyz";
                } else {
                    highlight = findOriginalExpression(emptyStringResult.getExpression(), baseRequestResponse.response().bodyToString());
                }
                responseViewer.setSearchExpression(highlight);
                responseViewer.setResponse(baseRequestResponse.response());
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };

        // Log table popup menu
        JPopupMenu logPopupmenu = new JPopupMenu();

        // Send to Repeater
        JMenuItem sendToRepeaterItem = new JMenuItem("Send to Repeater");
        sendToRepeaterItem.addActionListener(l -> sendToRepeater());
        logPopupmenu.add(sendToRepeaterItem);

        // Remove url
        JMenuItem deleteItem = new JMenuItem("Delete items");
        deleteItem.addActionListener(l -> deleteItems());
        logPopupmenu.add(deleteItem);

        table.setComponentPopupMenu(logPopupmenu);

        JScrollPane scrollPane = new JScrollPane(table);

        splitPane.setLeftComponent(scrollPane);

        mainPanel.add(splitPane, BorderLayout.CENTER); // Add the main UI components

        return mainPanel;
    }

    void sendToRepeater() {
        int[] selectedRows = table.getSelectedRows();
        for(int i : selectedRows) {
            EmptyStringResult esr = tableModel.get(i);
            api.repeater().sendToRepeater(esr.getBaseRequestResponse().request());
        }
    }

    void deleteItems() {
        int[] selectedRows = table.getSelectedRows();
        ArrayList<Integer> selectedIds = new ArrayList<>();

        for(int i : selectedRows) {
            api.logging().logToOutput("" + i);
            EmptyStringResult esr = tableModel.get(i);
            selectedIds.add(esr.getId());
            emptyStringScanner.getHiddenParamList().remove(esr.getHiddenParam());
        }
        tableModel.remove(selectedIds);

        tableModel.fireTableDataChanged();
    }

    void copyHiddenParamsToClipboard() {
        String text = "";
        for (String param : emptyStringScanner.getHiddenParamList()) {
            text += param + "\n";
        }
        copyTextToClipboard(text);
    }

    void copyTextToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(text);
        clipboard.setContents(selection, null);
    }

    private String findOriginalExpression(String match, String response) {
        String[] parts = match.split(" ");
        String regex = Pattern.quote(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            regex += ".*";
            regex += Pattern.quote(parts[i]);
        }

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return "";
        }
    }
}
