package udb;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class StopwatchFrame extends JFrame {
    private final JLabel timeLabel;
    private final JButton startLapBtn;
    private final JButton stopBtn;
    private final DefaultTableModel tableModel;
    private final JTable lapTable;

    private final Timer timer; // Swing timer chạy GUI thread
    private long startTimeMillis; // thời điểm bắt đầu
    private long pausedOffset = 0L; // nếu cần resume (hiện không dùng resume)
    private boolean running = false;
    private final List<Long> laps = new ArrayList<>();

    public StopwatchFrame() {
        super("BẤM GIỜ");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Title
        JLabel title = new JLabel("BẤM GIỜ", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(new Color(20, 120, 20));
        title.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(title, BorderLayout.NORTH);

        // Big time display
        timeLabel = new JLabel("00:00:00,00", SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 64));
        timeLabel.setForeground(new Color(20, 70, 150));
        timeLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(timeLabel, BorderLayout.CENTER);

        // Left: Start button
        startLapBtn = new JButton("Start");
        startLapBtn.setFont(new Font("Arial", Font.BOLD, 22));
        startLapBtn.setPreferredSize(new Dimension(120, 120));
        startLapBtn.setBackground(new Color(76, 175, 80)); // green
        startLapBtn.setForeground(Color.WHITE);
        startLapBtn.setFocusPainted(false);

        // Right: Stop button
        stopBtn = new JButton("Stop");
        stopBtn.setFont(new Font("Arial", Font.BOLD, 20));
        stopBtn.setPreferredSize(new Dimension(120, 120));
        stopBtn.setBackground(new Color(255, 153, 102)); // orange
        stopBtn.setForeground(Color.BLACK);
        stopBtn.setFocusPainted(false);
        stopBtn.setEnabled(false);

        JPanel controlPanel = new JPanel(new BorderLayout());
        JPanel leftBtnHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel rightBtnHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        leftBtnHolder.add(startLapBtn);
        rightBtnHolder.add(stopBtn);
        controlPanel.add(leftBtnHolder, BorderLayout.WEST);
        controlPanel.add(rightBtnHolder, BorderLayout.EAST);
        controlPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.LIGHT_GRAY));
        add(controlPanel, BorderLayout.SOUTH);

        // Table hiển thị Laps
        String[] columnNames = {"Lượt", "Thời gian"};
        tableModel = new DefaultTableModel(columnNames, 0);
        lapTable = new JTable(tableModel);
        lapTable.setFont(new Font("Arial", Font.PLAIN, 20));
        lapTable.setRowHeight(28);
        JScrollPane lapScroll = new JScrollPane(lapTable);
        lapScroll.setPreferredSize(new Dimension(320, 300));
        add(lapScroll, BorderLayout.EAST);

        // Timer cập nhật hiển thị (10 ms -> centisecond precision)
        timer = new Timer(10, e -> {
            long now = System.currentTimeMillis();
            long elapsed = now - startTimeMillis + pausedOffset;
            timeLabel.setText(formatTime(elapsed));
        });

        // Button hành vi
        startLapBtn.addActionListener(e -> {
            if (!running) {
                // Start mới
                startTimeMillis = System.currentTimeMillis();
                pausedOffset = 0;
                timer.start();
                running = true;
                laps.clear();
                tableModel.setRowCount(0); // clear bảng
                startLapBtn.setText("Lap"); // chuyển chức năng
                stopBtn.setEnabled(true);
             // Gửi UDP thông báo Start
                sendUDPMessage("START", "127.0.0.1", 3306);
            } else {
                // Đang chạy -> ghi lap
                long now = System.currentTimeMillis();
                long elapsed = now - startTimeMillis + pausedOffset;
                laps.add(elapsed);
                String lapMsg = "LAP:" + formatTime(elapsed);
                tableModel.addRow(new Object[]{"Lượt " + laps.size(), formatTime(elapsed)});
                // scroll xuống dưới
                lapTable.scrollRectToVisible(lapTable.getCellRect(tableModel.getRowCount() - 1, 0, true));
             // Gửi UDP thông báo Lap
                sendUDPMessage(lapMsg, "127.0.0.1", 3306);
            }
        });

        stopBtn.addActionListener(e -> {
            if (running) {
                timer.stop();
                running = false;
                startLapBtn.setText("Start");
                stopBtn.setEnabled(false);
                // Gửi UDP thông báo Stop
                sendUDPMessage("STOP", "127.0.0.1", 3306);
            }
        });

        // Make UI nicer on resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                timeLabel.setFont(timeLabel.getFont().deriveFont(Math.max(28f, getHeight() / 10f)));
            }
        });
    }
    private void sendUDPMessage(String message, String ip, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper: format milliseconds -> HH:mm:ss,cc (cc = centiseconds)
    private static String formatTime(long ms) {
        long totalCs = ms / 10; // centiseconds
        long cs = totalCs % 100;
        long totalSec = totalCs / 100;
        long sec = totalSec % 60;
        long totalMin = totalSec / 60;
        long min = totalMin % 60;
        long hour = totalMin / 60;
        return String.format("%02d:%02d:%02d,%02d", hour, min, sec, cs);
    }

    // Quick test
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StopwatchFrame f = new StopwatchFrame();
            f.setVisible(true);
        });
    }
}
