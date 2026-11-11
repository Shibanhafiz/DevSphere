package com.devsphere.ui;

import com.devsphere.model.ContributionDay;
import com.devsphere.service.ContributionService;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A database-backed, GitHub-style activity calendar for one signed-in user. */
public class ContributionHeatmapPanel extends JPanel {
    private static final Color[] LEVEL_COLORS = {
            new Color(235, 239, 246), new Color(198, 230, 205), new Color(137, 205, 146),
            new Color(67, 160, 80), new Color(27, 105, 47)
    };
    private static final DateTimeFormatter RANGE_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

    private final ContributionService service;
    private final long userId;
    private final JPanel monthLabels = new JPanel();
    private final JPanel grid = new JPanel();
    private final JLabel rangeLabel = new JLabel();
    private YearMonth endMonth = YearMonth.now();

    public ContributionHeatmapPanel(ContributionService service, long userId) {
        this.service = service;
        this.userId = userId;
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);
        JButton previous = new JButton("Previous 12 months");
        previous.addActionListener(e -> { endMonth = endMonth.minusMonths(12); refresh(); });
        JButton next = new JButton("Next 12 months");
        next.addActionListener(e -> { if (endMonth.isBefore(YearMonth.now())) { endMonth = endMonth.plusMonths(12); if (endMonth.isAfter(YearMonth.now())) endMonth = YearMonth.now(); refresh(); } });
        rangeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        controls.add(previous, BorderLayout.WEST);
        controls.add(rangeLabel, BorderLayout.CENTER);
        controls.add(next, BorderLayout.EAST);
        add(controls, BorderLayout.NORTH);

        JPanel calendar = new JPanel(new BorderLayout(5, 0));
        calendar.setOpaque(false);
        JPanel weekdayLabels = new JPanel(new GridLayout(7, 1, 0, 3));
        weekdayLabels.setOpaque(false);
        for (String day : new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}) {
            JLabel label = new JLabel(day);
            label.setFont(new Font("SansSerif", Font.PLAIN, 10));
            label.setForeground(new Color(90, 100, 115));
            weekdayLabels.add(label);
        }
        calendar.add(weekdayLabels, BorderLayout.WEST);

        JPanel calendarGrid = new JPanel(new BorderLayout(0, 3));
        calendarGrid.setOpaque(false);
        calendarGrid.add(monthLabels, BorderLayout.NORTH);
        calendarGrid.add(new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        calendar.add(calendarGrid, BorderLayout.CENTER);
        add(calendar, BorderLayout.CENTER);
        add(legend(), BorderLayout.SOUTH);
        refresh();
    }

    public void refresh() {
        LocalDate from = endMonth.minusMonths(11).atDay(1);
        LocalDate to = endMonth.plusMonths(1).atDay(1);
        rangeLabel.setText(from.format(RANGE_FORMAT) + " – " + endMonth.format(RANGE_FORMAT));
        try {
            Map<LocalDate, ContributionDay> days = new HashMap<>();
            for (ContributionDay day : service.heatmap(userId, from, to)) days.put(day.date(), day);
            paintCalendar(from, to, days);
        } catch (Exception e) {
            grid.removeAll();
            grid.add(new JLabel("Contribution activity is currently unavailable."));
            grid.revalidate(); grid.repaint();
        }
    }

    private void paintCalendar(LocalDate from, LocalDate to, Map<LocalDate, ContributionDay> days) {
        LocalDate firstCell = from.minusDays(from.getDayOfWeek().getValue() % 7);
        LocalDate lastDate = to.minusDays(1);
        LocalDate lastCell = lastDate.plusDays(6 - (lastDate.getDayOfWeek().getValue() % 7));
        int columns = (int) (java.time.temporal.ChronoUnit.DAYS.between(firstCell, lastCell) / 7) + 1;

        monthLabels.setLayout(new GridLayout(1, columns));
        monthLabels.removeAll();
        for (int column = 0; column < columns; column++) {
            LocalDate weekStart = firstCell.plusDays(column * 7L);
            JLabel label = new JLabel(weekStart.getDayOfMonth() <= 7 ? weekStart.format(DateTimeFormatter.ofPattern("MMM")) : "");
            label.setFont(new Font("SansSerif", Font.PLAIN, 10));
            label.setForeground(new Color(90, 100, 115));
            monthLabels.add(label);
        }

        grid.setLayout(new GridLayout(7, columns, 3, 3));
        grid.removeAll();
        grid.setBackground(Color.WHITE);
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < 7; row++) {
                LocalDate date = firstCell.plusDays(column * 7L + row);
                ContributionDay day = days.getOrDefault(date, new ContributionDay(date, 0, 0, List.of()));
                boolean visible = !date.isBefore(from) && date.isBefore(to);
                grid.add(new HeatCell(day, visible));
            }
        }
        grid.revalidate(); grid.repaint(); monthLabels.revalidate(); monthLabels.repaint();
    }

    private JPanel legend() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setOpaque(false);
        panel.add(new JLabel("Less"));
        String[] labels = {"0", "1–2", "3–5", "6–9", "10+"};
        for (int i = 0; i < LEVEL_COLORS.length; i++) {
            JLabel swatch = new JLabel(" ");
            swatch.setOpaque(true); swatch.setBackground(LEVEL_COLORS[i]);
            swatch.setPreferredSize(new Dimension(16, 16));
            panel.add(swatch); panel.add(new JLabel(labels[i]));
        }
        panel.add(new JLabel("activities"));
        return panel;
    }

    private final class HeatCell extends JButton {
        private final ContributionDay day;

        HeatCell(ContributionDay day, boolean visible) {
            this.day = day;
            setText(visible ? String.valueOf(day.date().getDayOfMonth()) : "");
            setFont(new Font("SansSerif", Font.PLAIN, 9));
            setPreferredSize(new Dimension(22, 22));
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(BorderFactory.createLineBorder(Color.WHITE));
            setOpaque(true);
            setBackground(visible ? LEVEL_COLORS[level(day.activityCount())] : Color.WHITE);
            setEnabled(visible);
            setToolTipText(visible ? day.date() + ": " + day.activityCount() + " activities" : null);
            if (visible) addActionListener(e -> showDetails());
        }

        private int level(int count) {
            if (count >= 10) return 4;
            if (count >= 6) return 3;
            if (count >= 3) return 2;
            if (count >= 1) return 1;
            return 0;
        }

        private void showDetails() {
            String types = day.activityTypes().isEmpty() ? "None" : String.join(", ", day.activityTypes());
            JOptionPane.showMessageDialog(ContributionHeatmapPanel.this,
                    "Selected date: " + day.date() + "\nActivities: " + day.activityCount()
                            + "\nActivity types: " + types + "\nPoints earned: " + day.points(),
                    "Contribution details", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
