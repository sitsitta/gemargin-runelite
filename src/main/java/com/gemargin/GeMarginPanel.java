package com.gemargin;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class GeMarginPanel extends PluginPanel
{
    private final SessionTracker sessionTracker;
    private final GeMarginApi api;

    private final JLabel profitLabel = new JLabel("0 gp");
    private final JLabel flipCountLabel = new JLabel("0");
    private final JLabel durationLabel = new JLabel("0m");
    private final JLabel syncStatusLabel = new JLabel("Not linked");
    private final JPanel tradesPanel = new JPanel();

    public GeMarginPanel(SessionTracker sessionTracker, GeMarginApi api)
    {
        super(false);
        this.sessionTracker = sessionTracker;
        this.api = api;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildTradesPanel(), BorderLayout.CENTER);
    }

    private JPanel buildHeaderPanel()
    {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(10, 10, 10, 10));
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel title = new JLabel("GE Margin");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(new Color(255, 215, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(8));

        header.add(buildStatRow("Session Profit:", profitLabel));
        header.add(Box.createVerticalStrut(4));
        header.add(buildStatRow("Flips:", flipCountLabel));
        header.add(Box.createVerticalStrut(4));
        header.add(buildStatRow("Duration:", durationLabel));
        header.add(Box.createVerticalStrut(4));
        header.add(buildStatRow("Cloud Sync:", syncStatusLabel));

        return header;
    }

    private JPanel buildStatRow(String label, JLabel valueLabel)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel left = new JLabel(label);
        left.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        left.setFont(left.getFont().deriveFont(12f));
        row.add(left, BorderLayout.WEST);

        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 12f));
        row.add(valueLabel, BorderLayout.EAST);

        return row;
    }

    private JScrollPane buildTradesPanel()
    {
        tradesPanel.setLayout(new BoxLayout(tradesPanel, BoxLayout.Y_AXIS));
        tradesPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(tradesPanel);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        return scrollPane;
    }

    public void update()
    {
        long profit = sessionTracker.getTotalProfit();
        profitLabel.setText(formatGp(profit));
        profitLabel.setForeground(profit >= 0 ? Color.GREEN : Color.RED);

        flipCountLabel.setText(String.valueOf(sessionTracker.getFlipCount()));
        durationLabel.setText(sessionTracker.getSessionDuration());

        if (api.isConfigured())
        {
            syncStatusLabel.setText("Linked");
            syncStatusLabel.setForeground(Color.GREEN);
        }
        else
        {
            syncStatusLabel.setText("Not linked");
            syncStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        }

        tradesPanel.removeAll();
        var trades = sessionTracker.getTrades();
        int start = Math.max(0, trades.size() - 20);
        for (int i = trades.size() - 1; i >= start; i--)
        {
            var trade = trades.get(i);
            JPanel row = new JPanel(new BorderLayout());
            row.setBorder(new EmptyBorder(4, 10, 4, 10));
            row.setBackground(ColorScheme.DARK_GRAY_COLOR);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            String typeStr = trade.isBuying() ? "BUY" : "SELL";
            JLabel left = new JLabel(typeStr + " " + trade.getQty() + "x " + trade.getItemName());
            left.setForeground(trade.isBuying() ? new Color(100, 200, 255) : new Color(255, 200, 100));
            left.setFont(left.getFont().deriveFont(11f));
            row.add(left, BorderLayout.WEST);

            if (!trade.isBuying() && trade.getProfit() != 0)
            {
                JLabel profitLbl = new JLabel(formatGp(trade.getProfit()));
                profitLbl.setForeground(trade.getProfit() > 0 ? Color.GREEN : Color.RED);
                profitLbl.setFont(profitLbl.getFont().deriveFont(Font.BOLD, 11f));
                row.add(profitLbl, BorderLayout.EAST);
            }

            tradesPanel.add(row);
        }

        tradesPanel.revalidate();
        tradesPanel.repaint();
    }

    private String formatGp(long amount)
    {
        if (Math.abs(amount) >= 10_000_000)
        {
            return String.format("%.1fM gp", amount / 1_000_000.0);
        }
        if (Math.abs(amount) >= 1_000)
        {
            return String.format("%.0fK gp", amount / 1_000.0);
        }
        return amount + " gp";
    }
}
