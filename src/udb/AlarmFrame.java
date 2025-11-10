package udb;

import java.net.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AlarmFrame extends JFrame {

    private final JPanel listPanel;    // panel chứa các hàng báo thức
    private final JScrollPane scrollPane;
    private final List<Alarm> alarms = new ArrayList<>();

    public AlarmFrame() {
        super("BÁO THỨC");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 520);
        setLocationRelativeTo(null);

        // Title
        JLabel title = new JLabel("BÁO THỨC", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setForeground(Color.BLUE);
        title.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Main container
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Panel list with vertical layout
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        // Right side: add button (plus)
        JButton addBtn = new JButton("+");
        addBtn.setFont(new Font("Arial", Font.BOLD, 22));
        addBtn.setPreferredSize(new Dimension(50, 50));
        addBtn.addActionListener(e -> addAlarmDialog(null));
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(addBtn, BorderLayout.NORTH);
        rightPanel.setBorder(new EmptyBorder(0, 8, 0, 0));
        rightPanel.setBackground(Color.WHITE);

        content.add(scrollPane, BorderLayout.CENTER);
        content.add(rightPanel, BorderLayout.EAST);

        getContentPane().add(title, BorderLayout.NORTH);
        getContentPane().add(content, BorderLayout.CENTER);

        // sample data to match your screenshot
        alarms.add(new Alarm("03:10", "Thứ 2, Thứ 3"));
        alarms.add(new Alarm("05:20", "Thứ 5"));
        alarms.add(new Alarm("10:00", "Chủ Nhật"));
        alarms.add(new Alarm("06:00", "All ngày"));

        refreshList();
        Timer timer = new Timer(1000, e -> checkAlarms());
        timer.start();
    }
 // Gửi thông báo qua UDP khi báo thức kích hoạt
    private void sendUDPMessage(String message, String ip, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] buf = message.getBytes();
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
            socket.close();
            System.out.println("Đã gửi UDP: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

 // Hàm kiểm tra báo thức
    private void checkAlarms() {
        java.time.LocalTime now = java.time.LocalTime.now();
        java.time.DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();

        for (Alarm a : alarms) {
            if (!a.enabled) continue;

            // So sánh giờ:phút
            String nowTime = String.format("%02d:%02d", now.getHour(), now.getMinute());
            if (a.time.equals(nowTime)) {
                // Kiểm tra ngày áp dụng
                if (a.days.contains("All") || a.days.contains(convertDay(today))) {
                    triggerAlarm(a);
                }
            }
        }
    }

    // Chuyển DayOfWeek sang chuỗi tiếng Việt
    private String convertDay(java.time.DayOfWeek d) {
        switch (d) {
            case MONDAY: return "Thứ 2";
            case TUESDAY: return "Thứ 3";
            case WEDNESDAY: return "Thứ 4";
            case THURSDAY: return "Thứ 5";
            case FRIDAY: return "Thứ 6";
            case SATURDAY: return "Thứ 7";
            case SUNDAY: return "Chủ Nhật";
        }
        return "";
    }
 // Hàm xử lý khi báo thức kêu
    private void triggerAlarm(Alarm a) {
    	// Gửi tín hiệu báo thức qua UDP
        sendUDPMessage("ALARM_TRIGGERED " + a.time + " " + a.days, "127.0.0.1", 3306);
    	// Hiện popup
        JOptionPane.showMessageDialog(this,
            "Đến giờ báo thức: " + a.time + "\n" + a.days,
            "BÁO THỨC!",
            JOptionPane.INFORMATION_MESSAGE);

        // Nếu có nhạc chuông thì phát (chỉ hỗ trợ .wav)
        if (a.ringtone != null) {
            try {
                javax.sound.sampled.AudioInputStream audioIn =
                    javax.sound.sampled.AudioSystem.getAudioInputStream(a.ringtone);
                javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // Rebuild the list panel from alarms list
    private void refreshList() {
        listPanel.removeAll();
        for (Alarm a : alarms) {
            listPanel.add(new AlarmRow(a));
            listPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    // Show dialog to add or edit an alarm
    private void addAlarmDialog(Alarm existing) {
        JDialog dlg = new JDialog(this, (existing == null ? "Thêm báo thức" : "Sửa báo thức"), true);
        dlg.setSize(380, 260);
        dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(10,10,10,10));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        p.add(new JLabel("Thời gian (HH:mm):"), c);
        c.gridx = 1;
        JTextField timeField = new JTextField(existing != null ? existing.time : "07:00");
        p.add(timeField, c);

        c.gridx = 0; c.gridy = 1;
        p.add(new JLabel("Ngày áp dụng:"), c);
        c.gridx = 1;

        // Tạo combo box chọn ngày, tháng, năm
        JComboBox<Integer> dayBox = new JComboBox<>();
        for (int i = 1; i <= 31; i++) dayBox.addItem(i);

        JComboBox<Integer> monthBox = new JComboBox<>();
        for (int i = 1; i <= 12; i++) monthBox.addItem(i);

        JComboBox<Integer> yearBox = new JComboBox<>();
        for (int i = 2024; i <= 2030; i++) yearBox.addItem(i);

        // Nếu đang sửa báo thức thì parse lại ngày/tháng/năm (nếu có)
        if (existing != null && existing.days.matches("\\d{2}/\\d{2}/\\d{4}")) {
            String[] parts = existing.days.split("/");
            dayBox.setSelectedItem(Integer.parseInt(parts[0]));
            monthBox.setSelectedItem(Integer.parseInt(parts[1]));
            yearBox.setSelectedItem(Integer.parseInt(parts[2]));
        }

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        datePanel.add(dayBox);
        datePanel.add(new JLabel("/"));
        datePanel.add(monthBox);
        datePanel.add(new JLabel("/"));
        datePanel.add(yearBox);

        p.add(datePanel, c);


        c.gridx = 0; c.gridy = 2;
        p.add(new JLabel("Nhạc chuông:"), c);
        c.gridx = 1;
        JPanel chooseRow = new JPanel(new BorderLayout(6,0));
        JTextField ringtoneField = new JTextField(existing != null ? (existing.ringtone == null ? "" : existing.ringtone.getName()) : "");
        ringtoneField.setEditable(false);
        JButton chooseBtn = new JButton("Chọn...");
        chooseBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            int rv = fc.showOpenDialog(dlg);
            if (rv == JFileChooser.APPROVE_OPTION) {
                File sel = fc.getSelectedFile();
                ringtoneField.setText(sel.getName());
                if (existing != null) existing.ringtone = sel;
                else ringtoneField.putClientProperty("selectedFile", sel);
            }
        });
        chooseRow.add(ringtoneField, BorderLayout.CENTER);
        chooseRow.add(chooseBtn, BorderLayout.EAST);
        p.add(chooseRow, c);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("Lưu");
        JButton cancel = new JButton("Hủy");
        ok.addActionListener(ev -> {
            String t = timeField.getText().trim();
            String d = String.format("%02d/%02d/%04d",
                    (Integer) dayBox.getSelectedItem(),
                    (Integer) monthBox.getSelectedItem(),
                    (Integer) yearBox.getSelectedItem());

            if (t.isEmpty()) { JOptionPane.showMessageDialog(dlg, "Nhập thời gian!"); return; }
            if (existing == null) {
                Alarm a = new Alarm(t, d);
                Object maybeFile = ringtoneField.getClientProperty("selectedFile");
                if (maybeFile instanceof File) a.ringtone = (File) maybeFile;
                alarms.add(a);
            } else {
                existing.time = t; existing.days = d;
            }
            refreshList();
            dlg.dispose();
        });
        cancel.addActionListener(ev -> dlg.dispose());
        btnRow.add(ok);
        btnRow.add(cancel);
        p.add(btnRow, c);

        dlg.getContentPane().add(p);
        dlg.setVisible(true);
    }

    // Alarm data holder
    private static class Alarm {
        String time;
        String days;
        File ringtone;
        boolean enabled = false;
        Alarm(String time, String days) { this.time = time; this.days = days; }
    }

    // Single row UI for an alarm
 // Single row UI for an alarm
    private class AlarmRow extends JPanel {
        private final Alarm alarm;
        private final JLabel timeLabel;
        private final JLabel daysLabel;
        private final JButton ringtoneBtn;
        private final JButton toggleBtn;
        private final JButton editBtn;
        private final JButton deleteBtn;
        
        AlarmRow(Alarm a) {
            this.alarm = a;
            setLayout(new GridBagLayout());
            setBackground(new Color(0xF3F3F3));
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(12,12,12,12);
            c.anchor = GridBagConstraints.WEST;

            // Time column (big)
            timeLabel = new JLabel(alarm.time);
            timeLabel.setFont(new Font("Arial", Font.BOLD, 28));
            c.gridx = 0; c.gridy = 0;
            c.weightx = 0;
            add(timeLabel, c);

            // Days column
            daysLabel = new JLabel(alarm.days);
            daysLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            c.gridx = 1; c.gridy = 0;
            c.weightx = 0.4;
            add(daysLabel, c);

            // Ringtone button (màu xanh ngọc)
            ringtoneBtn = new JButton(alarm.ringtone == null ? "Chọn nhạc chuông" : alarm.ringtone.getName());
            ringtoneBtn.setPreferredSize(new Dimension(180, 50));
            ringtoneBtn.setFocusPainted(false);
            ringtoneBtn.setOpaque(true);
            ringtoneBtn.setBorderPainted(false);
            ringtoneBtn.setBackground(new Color(0, 191, 165)); // xanh ngọc
            ringtoneBtn.setForeground(Color.WHITE);
            c.gridx = 2; c.gridy = 0;
            c.weightx = 0.2;
            add(ringtoneBtn, c);
            ringtoneBtn.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                int rv = fc.showOpenDialog(AlarmFrame.this);
                if (rv == JFileChooser.APPROVE_OPTION) {
                    alarm.ringtone = fc.getSelectedFile();
                    ringtoneBtn.setText(alarm.ringtone.getName());
                }
            });

            // Toggle ON/OFF
            toggleBtn = new JButton(alarm.enabled ? "ON" : "OFF");
            toggleBtn.setPreferredSize(new Dimension(120, 50));
            toggleBtn.setOpaque(true);
            toggleBtn.setBorderPainted(false);
            updateToggleColor();
            c.gridx = 3; c.gridy = 0;
            c.weightx = 0.1;
            add(toggleBtn, c);
            toggleBtn.addActionListener(e -> {
                alarm.enabled = !alarm.enabled;
                toggleBtn.setText(alarm.enabled ? "ON" : "OFF");
                updateToggleColor();
            });
            
            // Edit button (màu cam)
            editBtn = new JButton("Sửa");
            editBtn.setPreferredSize(new Dimension(100, 50));
            editBtn.setOpaque(true);
            editBtn.setBorderPainted(false);
            editBtn.setBackground(new Color(255, 140, 0)); // cam
            editBtn.setForeground(Color.WHITE);
            c.gridx = 4; c.gridy = 0;
            c.weightx = 0;
            add(editBtn, c);
            editBtn.addActionListener(e -> addAlarmDialog(alarm));

            // Delete button (màu đỏ)
            deleteBtn = new JButton("Xóa");
            deleteBtn.setPreferredSize(new Dimension(100, 50));
            deleteBtn.setOpaque(true);
            deleteBtn.setBorderPainted(false);
            deleteBtn.setBackground(new Color(220, 20, 60)); // đỏ tươi
            deleteBtn.setForeground(Color.WHITE);
            c.gridx = 5; c.gridy = 0;
            c.weightx = 0;
            add(deleteBtn, c);
            deleteBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                        AlarmFrame.this,
                        "Bạn có chắc muốn xóa báo thức " + alarm.time + " ?",
                        "Xác nhận",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    alarms.remove(alarm);
                    refreshList();
                }
            });
        }

        private void updateToggleColor() {
            if (alarm.enabled) {
                toggleBtn.setBackground(Color.GREEN.darker());
                toggleBtn.setForeground(Color.BLACK);
            } else {
                toggleBtn.setBackground(Color.RED);
                toggleBtn.setForeground(Color.WHITE);
            }
        }
    }

    

    // For quick testing when running this class directly
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AlarmFrame af = new AlarmFrame();
            af.setVisible(true);
        });
    }
}
