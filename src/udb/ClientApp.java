package udb;

//ClientApp.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import udb.StopwatchFrame;

public class ClientApp {
 public static void main(String[] args) {
     SwingUtilities.invokeLater(() -> {
         ClientFrame f = new ClientFrame();
         f.setVisible(true);
     });
 }
}

class ClientFrame extends JFrame {
 private ClockPanel clockPanel;
 private JPanel centerPanel;

 public ClientFrame() {
     setTitle("ĐỒNG HỒ SERVER ĐỒNG BỘ HÓA");
     setSize(980, 620);
     setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     setLocationRelativeTo(null);
     initUI();
 }

 private void initUI() {
     setLayout(new BorderLayout());
     JPanel top = new JPanel(new BorderLayout());
     top.setBorder(new EmptyBorder(10, 10, 10, 10));
     JLabel title = new JLabel("SYNC APP", SwingConstants.CENTER);
     title.setFont(new Font("Arial", Font.BOLD, 36));
     title.setForeground(new Color(81,170,30));
     top.add(title, BorderLayout.CENTER);
     add(top, BorderLayout.NORTH);

     JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
     split.setDividerLocation(320);
     split.setLeftComponent(createLeft());
     centerPanel = createCenter();
     split.setRightComponent(centerPanel);
     add(split, BorderLayout.CENTER);
 }

 private JPanel createLeft() {
     JPanel left = new JPanel(new BorderLayout());
     clockPanel = new ClockPanel();
     left.add(clockPanel, BorderLayout.CENTER);
     JLabel digital = clockPanel.getDigitalLabel();
     digital.setHorizontalAlignment(SwingConstants.CENTER);
     digital.setBorder(new EmptyBorder(10,10,10,10));
     left.add(digital, BorderLayout.SOUTH);
     return left;
 }

 private JPanel createCenter() {
     JPanel p = new JPanel(new GridBagLayout());
     p.setBackground(Color.WHITE);
     GridBagConstraints c = new GridBagConstraints();
     c.insets = new Insets(20,20,20,20);
     c.gridx = 0; c.gridy = 0;

     Font btnFont = new Font("Arial", Font.BOLD, 22);
     Dimension big = new Dimension(260, 140);

     JButton syncBtn = new JButton("Đồng bộ hóa");
     syncBtn.setPreferredSize(big);
     syncBtn.setBackground(new Color(255, 230, 150));
     syncBtn.setForeground(Color.RED);
     syncBtn.setFont(btnFont);
     syncBtn.addActionListener(e -> openSyncPanel());

     JButton alarmBtn = new JButton("Báo thức");
     alarmBtn.setPreferredSize(big);
     alarmBtn.setBackground(new Color(255, 230, 150));
     alarmBtn.setForeground(Color.RED);
     alarmBtn.setFont(btnFont);
     
     alarmBtn.addActionListener(e -> {
    	    SwingUtilities.invokeLater(() -> {
    	        AlarmFrame af = new AlarmFrame();
    	        af.setVisible(true);
    	    });
    	});

     
     JButton timerBtn = new JButton("Hẹn giờ");
     timerBtn.setPreferredSize(big);
     timerBtn.setBackground(new Color(255, 230, 150));
     timerBtn.setForeground(Color.RED);
     timerBtn.setFont(btnFont);
     
  // >>> thêm action mở TimerFrame
     timerBtn.addActionListener(e -> {
         SwingUtilities.invokeLater(() -> {
             TimerFrame tf = new TimerFrame();
             tf.setVisible(true);
         });
     });


     JButton stopwatchBtn = new JButton("Bấm giờ");
     stopwatchBtn.setPreferredSize(big);
     stopwatchBtn.setBackground(new Color(255, 230, 150));
     stopwatchBtn.setForeground(Color.RED);
     stopwatchBtn.setFont(btnFont);
     
     stopwatchBtn.addActionListener(e -> {
    	    SwingUtilities.invokeLater(() -> {
    	        StopwatchFrame sw = new StopwatchFrame();
    	        sw.setVisible(true);
    	    });
    	});


     c.gridx = 0; c.gridy = 0;
     p.add(syncBtn, c);
     c.gridx = 1; c.gridy = 0;
     p.add(alarmBtn, c);
     c.gridx = 0; c.gridy = 1;
     p.add(timerBtn, c);
     c.gridx = 1; c.gridy = 1;
     p.add(stopwatchBtn, c);
     return p;
 }

 private void openSyncPanel() {
     SyncFrame sf = new SyncFrame(clockPanel);
     sf.setVisible(true);
 }
}

/* ---------- SyncFrame (pop-up) ---------- */
class SyncFrame extends JFrame {
 private ClockPanel clockPanelReference;
 private JTextField tfIP, tfPort, tfSamples, tfInterval, tfTimeout;
 private JButton btnRun, btnStop;
 private JTable table;
 private DefaultTableModel tableModel;
 private JTextArea resultArea;
 private AtomicBoolean running = new AtomicBoolean(false);
 private UDPClient client;

 public SyncFrame(ClockPanel cp) {
     this.clockPanelReference = cp;
     setTitle("ĐỒNG BỘ HÓA");
     setSize(980, 520);
     setLocationRelativeTo(null);
     initUI();
 }

 private void initUI() {
     setLayout(new BorderLayout());

     JPanel top = new JPanel(new BorderLayout());
     JLabel title = new JLabel("ĐỒNG BỘ HÓA", SwingConstants.CENTER);
     title.setFont(new Font("Arial", Font.BOLD, 26));
     
     title.setForeground(new Color(10,80,170));
     top.add(title, BorderLayout.CENTER);
     add(top, BorderLayout.NORTH);

     JPanel center = new JPanel(new BorderLayout());
     JPanel config = new JPanel(new GridBagLayout());
     config.setBorder(new EmptyBorder(12,12,12,12));
     GridBagConstraints c = new GridBagConstraints();
     c.insets = new Insets(8,8,8,8);
     c.anchor = GridBagConstraints.WEST;

     c.gridx=0;c.gridy=0; config.add(new JLabel("Server IP:"), c);
     tfIP = new JTextField("127.0.0.1", 10);
     c.gridx=1; config.add(tfIP, c);

     c.gridx=0;c.gridy=1; config.add(new JLabel("Port:"), c);
     tfPort = new JTextField("9876", 6);
     c.gridx=1; config.add(tfPort, c);

     c.gridx=0;c.gridy=2; config.add(new JLabel("Samples:"), c);
     tfSamples = new JTextField("6", 6);
     c.gridx=1; config.add(tfSamples, c);

     c.gridx=0;c.gridy=3; config.add(new JLabel("Interval(ms):"), c);
     tfInterval = new JTextField("250", 6);
     c.gridx=1; config.add(tfInterval, c);

     c.gridx=0;c.gridy=4; config.add(new JLabel("Timeout(ms):"), c);
     tfTimeout = new JTextField("1000", 6);
     c.gridx=1; config.add(tfTimeout, c);

     JPanel rightButtons = new JPanel(new GridLayout(2,1,6,12));
     btnRun = new JButton("Run");
     btnRun.setBackground(new Color(111,178,78));
     btnRun.setForeground(Color.WHITE);
     btnRun.setPreferredSize(new Dimension(100,48));
     btnStop = new JButton("Stop");
     btnStop.setBackground(new Color(245,160,120));
     btnStop.setForeground(Color.WHITE);
     btnStop.setPreferredSize(new Dimension(100,48));
     btnStop.setEnabled(false);
     rightButtons.add(btnRun);
     rightButtons.add(btnStop);
     
  // Clock ở giữa
     ClockPanel syncClock = new ClockPanel();
     JPanel clockWrapper = new JPanel(new BorderLayout());
     clockWrapper.add(syncClock, BorderLayout.CENTER);
     //clockWrapper.add(syncClock.getDigitalLabel(), BorderLayout.SOUTH);

     JPanel topConfig = new JPanel(new BorderLayout());
     topConfig.add(config, BorderLayout.WEST);
     topConfig.add(clockWrapper, BorderLayout.CENTER);
     topConfig.add(rightButtons, BorderLayout.EAST);

     center.add(topConfig, BorderLayout.NORTH);

     // bottom: two columns (table stats | result)
     JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
     bottomSplit.setDividerLocation(600);

     // table
     tableModel = new DefaultTableModel(new Object[]{"STT", "Delay (ms)", "Offset (ms)"}, 0);
     table = new JTable(tableModel);
     JScrollPane spTable = new JScrollPane(table);
     JPanel pTable = new JPanel(new BorderLayout());
     pTable.add(new JLabel("Bảng thống kê"), BorderLayout.NORTH);
     pTable.add(spTable, BorderLayout.CENTER);

     // result
     resultArea = new JTextArea();
     resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
     JScrollPane spResult = new JScrollPane(resultArea);
     JPanel pResult = new JPanel(new BorderLayout());
     pResult.add(new JLabel("Kết quả đồng bộ"), BorderLayout.NORTH);
     pResult.add(spResult, BorderLayout.CENTER);

     bottomSplit.setLeftComponent(pTable);
     bottomSplit.setRightComponent(pResult);
     center.add(bottomSplit, BorderLayout.CENTER);

     add(center, BorderLayout.CENTER);

     // actions
     btnRun.addActionListener(e -> doRun());
     btnStop.addActionListener(e -> doStop());
 }

 private void doRun() {
     String ip = tfIP.getText().trim();
     int port = Integer.parseInt(tfPort.getText().trim());
     int samples = Integer.parseInt(tfSamples.getText().trim());
     int interval = Integer.parseInt(tfInterval.getText().trim());
     int timeout = Integer.parseInt(tfTimeout.getText().trim());

     tableModel.setRowCount(0);
     resultArea.setText("");
     btnRun.setEnabled(false);
     btnStop.setEnabled(true);
     running.set(true);

     client = new UDPClient(ip, port, timeout);
     new Thread(() -> {
         List<Long> offsets = new ArrayList<>();
         List<Long> delays = new ArrayList<>();
         int validCount = 0;
         for (int i=0;i<samples && running.get();i++) {
             try {
                 long t1 = System.currentTimeMillis();
                 String req = "SYNC";
                 // returns serverReceiveTime (ms)
                 Long serverTime = client.requestTime(req);
                 long t4 = System.currentTimeMillis();
                 if (serverTime == null) {
                     appendTable(i+1, -1, Long.MIN_VALUE);
                     resultArea.append(String.format("Sample %d: TIMEOUT\n", i+1));
                 } else {
                     long delay = (t4 - t1); // simple RTT
                     long offset = serverTime - ((t1 + t4)/2); // approx offset
                     offsets.add(offset);
                     delays.add(delay);
                     validCount++;
                     appendTable(i+1, delay, offset);
                 }
             } catch (Exception ex) {
                 appendTable(i+1, -1, Long.MIN_VALUE);
                 resultArea.append("Error sampling: " + ex.getMessage() + "\n");
             }

             try { Thread.sleep(interval); } catch (InterruptedException ignored){}
         }
         // calculate statistics
         SwingUtilities.invokeLater(() -> {
             btnRun.setEnabled(true);
             btnStop.setEnabled(false);
             running.set(false);
         });

         if (validCount > 0) {
             double avgDelay = delays.stream().mapToLong(Long::longValue).average().orElse(0.0);
             long medianOffset = median(offsets);
             // set clock adjustment on client-side (apply median offset)
             clockPanelReference.setAdjustmentOffset(medianOffset);

             long localNow = System.currentTimeMillis();
             long serverEst = localNow + medianOffset;

             StringBuilder sb = new StringBuilder();
             sb.append("KẾT QUẢ ĐỒNG BỘ (logic)\n");
             sb.append(String.format(" - Số mẫu hợp lệ: %d / %d\n", validCount, samples));
             sb.append(String.format(" - Độ trễ trung bình: %.2f ms\n", avgDelay));
             sb.append(String.format(" - Offset (median): %d ms\n", medianOffset));
             sb.append(String.format(" - Local now (ms): %d\n", localNow));
             sb.append(String.format(" - Server est (ms): %d\n", serverEst));
             SwingUtilities.invokeLater(() -> resultArea.setText(sb.toString()));
         } else {
             SwingUtilities.invokeLater(() -> resultArea.setText("Không có mẫu hợp lệ (tất cả timeout)."));
         }
     }).start();
 }

 private void doStop() {
     running.set(false);
     if (client != null) client.close();
     btnRun.setEnabled(true);
     btnStop.setEnabled(false);
     resultArea.append("\nĐã dừng thủ công.\n");
 }

 private void appendTable(int stt, long delay, long offset) {
     SwingUtilities.invokeLater(() -> {
         String sDelay = delay>=0? String.valueOf(delay): "TIMEOUT";
         String sOffset = offset!=Long.MIN_VALUE ? String.valueOf(offset) : "-";
         tableModel.addRow(new Object[]{stt, sDelay, sOffset});
     });
 }

 private long median(List<Long> list) {
     Collections.sort(list);
     int n = list.size();
     if (n==0) return 0;
     if (n%2==1) return list.get(n/2);
     else return (list.get(n/2 -1) + list.get(n/2))/2;
 }
}
/* -------------------- Hẹn giờ (Timer UI) -------------------- */
class TimerFrame extends JFrame {
    private JPanel listPanel;
    private java.util.List<TimerRow> rows = new ArrayList<>();
    private javax.swing.Timer tickTimer; // Swing timer, tick 1s

    public TimerFrame() {
        setTitle("HẸN GIỜ");
        setSize(820, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(8,8,8,8));
        JLabel title = new JLabel("HẸN GIỜ", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(new Color(10, 80, 170));
        top.add(title, BorderLayout.CENTER);

        JButton addBtn = new JButton("+");
        addBtn.setFont(new Font("Arial", Font.BOLD, 28));
        addBtn.setPreferredSize(new Dimension(60,60));
        addBtn.addActionListener(e -> {
            AddEditDialog d = new AddEditDialog(this, 0); // thêm mới
            Integer secs = d.showDialog();
            if (secs != null && secs > 0) addTimer(secs);
        });
        top.add(addBtn, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(listPanel);
        add(sp, BorderLayout.CENTER);

        // Start swing timer (tick 1s)
        tickTimer = new javax.swing.Timer(1000, e -> tickAll());
        tickTimer.start();

        // sample: (bỏ nếu không muốn khởi tạo ví dụ)
        // addTimer(10); addTimer(20); addTimer(60); addTimer(3600);
    }

    private void addTimer(int seconds) {
        TimerItem item = new TimerItem(seconds);
        TimerRow row = new TimerRow(item);
        rows.add(row);
        listPanel.add(row);
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void removeRow(TimerRow r) {
        rows.remove(r);
        listPanel.remove(r);
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void tickAll() {
        for (TimerRow r : new ArrayList<>(rows)) {
            if (r.item.isActive()) {
                r.item.decrement();
                r.updateLabels();
                if (r.item.getRemainingSeconds() <= 0) {
                    // đạt 0 -> stop và hiện popup
                    r.item.setActive(false);
                    r.updateToggle();
                    AlarmPopup.showMessage();
                }
            } else {
                r.updateLabels(); // luôn cập nhật nhãn
            }
        }
    }

    // ---- dòng từng hẹn giờ ----
    class TimerRow extends JPanel {
        TimerItem item;
        JLabel timeLabel;
        JLabel descLabel;
        JToggleButton toggle;
        JButton btnEdit, btnDelete;

        public TimerRow(TimerItem item) {
            this.item = item;
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createLineBorder(Color.BLACK));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            setPreferredSize(new Dimension(760, 100));

            // left: time big
            JPanel left = new JPanel(new BorderLayout());
            left.setPreferredSize(new Dimension(180, 100));
            timeLabel = new JLabel(item.formatRemaining(), SwingConstants.CENTER);
            timeLabel.setFont(new Font("Arial", Font.PLAIN, 34));
            left.add(timeLabel, BorderLayout.CENTER);
            descLabel = new JLabel(item.humanDescription(), SwingConstants.CENTER);
            descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            left.add(descLabel, BorderLayout.SOUTH);

            // center: toggle image placeholder
            JPanel center = new JPanel();
            center.setPreferredSize(new Dimension(160,100));
            toggle = new JToggleButton("OFF");
            toggle.setPreferredSize(new Dimension(120, 45));
            toggle.addActionListener(e -> {
                boolean sel = toggle.isSelected();
                item.setActive(sel);
                updateToggle();
            });
            updateToggle();
            center.add(toggle);

            // right: buttons Sửa / Xóa
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 20));
            btnEdit = new JButton("Sửa");
            btnEdit.setPreferredSize(new Dimension(100, 45));
            btnEdit.addActionListener(e -> {
                AddEditDialog d = new AddEditDialog(TimerFrame.this, item.getTotalSeconds());
                Integer secs = d.showDialog();
                if (secs != null && secs > 0) {
                    item.setTotalSeconds(secs);
                    item.reset();
                    updateLabels();
                }
            });
            btnDelete = new JButton("Xóa");
            btnDelete.setPreferredSize(new Dimension(100, 45));
            btnDelete.addActionListener(e -> {
                // xóa dòng
                TimerFrame.this.removeRow(this);
            });
            right.add(btnEdit);
            right.add(btnDelete);

            add(left, BorderLayout.WEST);
            add(center, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        void updateLabels() {
            timeLabel.setText(item.formatRemaining());
            descLabel.setText(item.humanDescription());
        }

        void updateToggle() {
            if (item.isActive()) {
                toggle.setText("ON");
                toggle.setSelected(true);
            } else {
                toggle.setText("OFF");
                toggle.setSelected(false);
            }
        }
    }
}

/* ---- model timer item ---- */
class TimerItem {
    private int totalSeconds;
    private int remainingSeconds;
    private boolean active = false;

    public TimerItem(int totalSeconds) {
        this.totalSeconds = Math.max(1, totalSeconds);
        this.remainingSeconds = this.totalSeconds;
    }

    public int getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(int sec) { this.totalSeconds = Math.max(1, sec); }

    public int getRemainingSeconds() { return remainingSeconds; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }

    public void decrement() {
        if (remainingSeconds > 0) remainingSeconds--;
    }

    public void reset() { this.remainingSeconds = this.totalSeconds; }

    public String formatRemaining() {
        int s = remainingSeconds;
        int h = s / 3600;
        int m = (s % 3600) / 60;
        int sec = s % 60;
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, sec);
        else return String.format("%02d:%02d", m, sec);
    }

    public String humanDescription() {
        if (totalSeconds < 60) return String.format("%02d giây", totalSeconds);
        if (totalSeconds < 3600) return String.format("%02d phút", totalSeconds / 60);
        return String.format("%02d tiếng", totalSeconds / 3600);
    }
}

/* ---- Dialog Thêm / Sửa ---- */
class AddEditDialog extends JDialog {
    private JSpinner spH, spM, spS;
    private Integer result = null;
    public AddEditDialog(JFrame owner, int initialTotalSeconds) {
        super(owner, true);
        setTitle(initialTotalSeconds > 0 ? "Sửa hẹn giờ" : "Thêm hẹn giờ");
        setSize(360, 200);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        JPanel p = new JPanel(new GridLayout(2, 1));
        JPanel spPanel = new JPanel(new FlowLayout());
        spH = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        spM = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        spS = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        spPanel.add(new JLabel("Giờ:")); spPanel.add(spH);
        spPanel.add(new JLabel("Phút:")); spPanel.add(spM);
        spPanel.add(new JLabel("Giây:")); spPanel.add(spS);

        // nếu sửa: chia initial -> h,m,s
        if (initialTotalSeconds > 0) {
            int s = initialTotalSeconds;
            spH.setValue(s / 3600);
            spM.setValue((s % 3600) / 60);
            spS.setValue(s % 60);
        }

        p.add(spPanel);

        JPanel btnP = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            int h = (Integer)spH.getValue();
            int m = (Integer)spM.getValue();
            int s = (Integer)spS.getValue();
            int total = h*3600 + m*60 + s;
            if (total <= 0) {
                JOptionPane.showMessageDialog(this, "Phải nhập thời gian > 0", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            result = total;
            setVisible(false);
        });
        JButton cancel = new JButton("Hủy");
        cancel.addActionListener(e -> {
            result = null;
            setVisible(false);
        });
        btnP.add(ok);
        btnP.add(cancel);
        p.add(btnP);
        add(p, BorderLayout.CENTER);
    }

    public Integer showDialog() {
        setVisible(true);
        return result;
    }
}

/* ---- Alarm popup ---- */
class AlarmPopup {
    public static void showMessage() {
        // hiển thị trên EDT
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ĐỒNG HỒ SERVER ĐỒNG BỘ HÓA");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(900, 400);
            frame.setLocationRelativeTo(null);
            frame.setAlwaysOnTop(true);

            JLabel label = new JLabel("Thầy Lê Tuấn Anh Đáng Yêu <3", SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 48));
            label.setForeground(new Color(81,170,30)); // màu xanh như hình
            frame.add(label);
            frame.setVisible(true);
        });
    }
}

/* ---------- UDPClient (simple request/response) ---------- */
class UDPClient {
 private String host;
 private int port;
 private DatagramSocket socket;
 private int timeoutMs;

 public UDPClient(String host, int port, int timeoutMs) {
     this.host = host;
     this.port = port;
     this.timeoutMs = timeoutMs;
     try {
         socket = new DatagramSocket();
         socket.setSoTimeout(timeoutMs);
     } catch (SocketException e) {
         throw new RuntimeException(e);
     }
 }

 // returns serverTime in ms, or null if timeout/error
 public Long requestTime(String msg) {
     try {
         byte[] bs = msg.getBytes();
         InetAddress addr = InetAddress.getByName(host);
         DatagramPacket p = new DatagramPacket(bs, bs.length, addr, port);
         long t1 = System.currentTimeMillis();
         socket.send(p);

         byte[] buf = new byte[512];
         DatagramPacket resp = new DatagramPacket(buf, buf.length);
         socket.receive(resp);
         long t4 = System.currentTimeMillis();

         String s = new String(resp.getData(), 0, resp.getLength()).trim();
         try {
             return Long.parseLong(s);
         } catch (NumberFormatException ex) {
             return null;
         }
     } catch (SocketTimeoutException ste) {
         return null;
     } catch (Exception ex) {
         ex.printStackTrace();
         return null;
     }
 }

 public void close() {
     if (socket != null && !socket.isClosed()) socket.close();
 }
}
