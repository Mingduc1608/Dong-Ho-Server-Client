package udb;

//ServerApp.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerApp {
 public static void main(String[] args) {
     SwingUtilities.invokeLater(() -> {
         ServerFrame f = new ServerFrame();
         f.setVisible(true);
     });
 }
}

/* ---------- ServerFrame (GUI) ---------- */
class ServerFrame extends JFrame {
 private JButton btnStart, btnStop;
 private JTextArea logArea;
 private ClockPanel clockPanel;
 private UDPServer server;

 public ServerFrame() {
     setTitle("ĐỒNG HỒ SERVER ĐỒNG BỘ HÓA");
     setSize(980, 620);
     setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     setLocationRelativeTo(null);
     initUI();
 }

 private void initUI() {
     setLayout(new BorderLayout());

     // Top title + buttons
     JPanel top = new JPanel(new BorderLayout());
     top.setBorder(new EmptyBorder(10, 10, 10, 10));
     JLabel title = new JLabel("SYNC APP", SwingConstants.CENTER);
     title.setFont(new Font("Arial", Font.BOLD, 36));
     title.setForeground(new Color(81, 170, 30));
     top.add(title, BorderLayout.CENTER);

     JPanel topRight = new JPanel();
     btnStart = createRoundedButton("Start", new Color(111, 178, 78));
     btnStop = createRoundedButton("Stop", new Color(245, 160, 120));
     topRight.add(btnStart);
     topRight.add(btnStop);
     top.add(topRight, BorderLayout.EAST);

     add(top, BorderLayout.NORTH);

     // Center split: left clock, right log
     JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
     split.setDividerLocation(320);
     split.setResizeWeight(0);
     // left: clock panel + digital label
     JPanel left = new JPanel(new BorderLayout());
     clockPanel = new ClockPanel();
     left.add(clockPanel, BorderLayout.CENTER);

     JLabel timeLabel = clockPanel.getDigitalLabel();
     timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
     timeLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
     left.add(timeLabel, BorderLayout.SOUTH);

     // right: log
     JPanel right = new JPanel(new BorderLayout());
     right.setBorder(new EmptyBorder(8, 8, 8, 8));
     JLabel logTitle = new JLabel("Log");
     logTitle.setFont(new Font("Arial", Font.BOLD, 16));
     right.add(logTitle, BorderLayout.NORTH);

     logArea = new JTextArea();
     logArea.setEditable(false);
     logArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
     JScrollPane sp = new JScrollPane(logArea);
     right.add(sp, BorderLayout.CENTER);

     split.setLeftComponent(left);
     split.setRightComponent(right);
     add(split, BorderLayout.CENTER);

     // button actions
     btnStart.addActionListener(e -> startServer());
     btnStop.addActionListener(e -> stopServer());
     btnStop.setEnabled(false);
 }

 private JButton createRoundedButton(String text, Color bg) {
     JButton b = new JButton(text);
     b.setPreferredSize(new Dimension(110, 46));
     b.setBackground(bg);
     b.setFocusPainted(false);
     b.setForeground(Color.WHITE);
     b.setFont(new Font("Arial", Font.BOLD, 16));
     b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
     return b;
 }

 private void startServer() {
     appendLog("[Start] Server Startting...");
     int port = 9876; // UDP port - bạn có thể đổi
     // DB params - hãy chỉnh theo máy bạn
     String url = "jdbc:mysql://localhost:3306/clock_sync?useSSL=false&serverTimezone=UTC";
     String user = "root";
     String pass = "root120204";

     server = new UDPServer(port, url, user, pass, this::appendLog);
     server.start();
     btnStart.setEnabled(false);
     btnStop.setEnabled(true);
 }

 private void stopServer() {
     appendLog("[Stop] Server Stop!");
     if (server != null) server.stop();
     btnStart.setEnabled(true);
     btnStop.setEnabled(false);
 }

 public void appendLog(String s) {
     SwingUtilities.invokeLater(() -> {
         SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
         String t = f.format(new Date());
         logArea.append("[" + t + "] " + s + "\n");
         logArea.setCaretPosition(logArea.getDocument().getLength());
     });
 }
}

/* ---------- ClockPanel (analog + digital) ---------- */
class ClockPanel extends JPanel {
 private BufferedImage clockImage;
 private JLabel digitalLabel;
 private Timer timer;
 private long adjustmentOffset = 0; // +/- ms for sync adjustment (if needed)

 public ClockPanel() {
     setBackground(Color.WHITE);
     setPreferredSize(new Dimension(320, 480));
     digitalLabel = new JLabel("00:00:00");
     digitalLabel.setFont(new Font("Monospaced", Font.BOLD, 20));
     digitalLabel.setOpaque(true);
     digitalLabel.setBackground(new Color(80, 140, 255));
     digitalLabel.setForeground(Color.YELLOW);
     digitalLabel.setPreferredSize(new Dimension(160, 40));
     digitalLabel.setHorizontalAlignment(SwingConstants.CENTER);

     // load clock image from resources/clock.png if exists
     try {
         clockImage = javax.imageio.ImageIO.read(new File("resources/clock.png"));
     } catch (Exception ex) {
         clockImage = null;
     }

     timer = new Timer(1000, e -> repaint());
     timer.start();
 }

 public JLabel getDigitalLabel() {
     return digitalLabel;
 }

 public void setAdjustmentOffset(long ms) {
     this.adjustmentOffset = ms;
 }

 @Override
 protected void paintComponent(Graphics g) {
     super.paintComponent(g);
     // draw analog clock
     int w = getWidth(), h = getHeight() - 60;
     Graphics2D g2 = (Graphics2D) g.create();
     g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

     int size = Math.min(w, h) - 40;
     int cx = (w / 2);
     int cy = (h / 2) - 10;
     int r = size / 2;

     if (clockImage != null) {
         g2.drawImage(clockImage, cx - r, cy - r, size, size, null);
     } else {
         // draw simple clock
         g2.setColor(new Color(206, 244, 206));
         g2.fillOval(cx - r, cy - r, 2*r, 2*r);
         g2.setColor(Color.GRAY);
         g2.setStroke(new BasicStroke(3f));
         g2.drawOval(cx - r, cy - r, 2*r, 2*r);

         // draw ticks
         for (int i=0;i<12;i++) {
             double ang = Math.toRadians(i * 30);
             int x1 = cx + (int)(Math.cos(ang) * (r-8));
             int y1 = cy + (int)(Math.sin(ang) * (r-8));
             int x2 = cx + (int)(Math.cos(ang) * (r-20));
             int y2 = cy + (int)(Math.sin(ang) * (r-20));
             g2.drawLine(x1,y1,x2,y2);
         }
     }

     // draw hands
     long now = System.currentTimeMillis() + adjustmentOffset;
     Date d = new Date(now);
     java.util.Calendar cal = java.util.Calendar.getInstance();
     cal.setTime(d);
     int sec = cal.get(java.util.Calendar.SECOND);
     int min = cal.get(java.util.Calendar.MINUTE);
     int hour = cal.get(java.util.Calendar.HOUR);

     double secAng = Math.toRadians((sec/60.0)*360 - 90);
     double minAng = Math.toRadians((min/60.0)*360 + (sec/60.0)*6 - 90);
     double hourAng = Math.toRadians((hour/12.0)*360 + (min/60.0)*30 - 90);

     int lenHour = r - 60;
     int lenMin = r - 30;
     int lenSec = r - 20;

     g2.setStroke(new BasicStroke(6f));
     g2.setColor(new Color(50,50,50));
     g2.drawLine(cx,cy, cx + (int)(Math.cos(hourAng)*lenHour), cy + (int)(Math.sin(hourAng)*lenHour));

     g2.setStroke(new BasicStroke(4f));
     g2.drawLine(cx,cy, cx + (int)(Math.cos(minAng)*lenMin), cy + (int)(Math.sin(minAng)*lenMin));

     g2.setStroke(new BasicStroke(2f));
     g2.setColor(Color.RED);
     g2.drawLine(cx,cy, cx + (int)(Math.cos(secAng)*lenSec), cy + (int)(Math.sin(secAng)*lenSec));

     g2.dispose();

     // update digital label
     SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
     digitalLabel.setText(sdf.format(new Date(now)));
 }
}

/* ---------- UDPServer (background thread) ---------- */
class UDPServer {
 private int port;
 private String dbUrl, dbUser, dbPass;
 private DatagramSocket socket;
 private Thread thread;
 private AtomicBoolean running = new AtomicBoolean(false);
 private java.util.function.Consumer<String> logger;

 public UDPServer(int port, String dbUrl, String dbUser, String dbPass, java.util.function.Consumer<String> logger) {
     this.port = port;
     this.dbUrl = dbUrl;
     this.dbUser = dbUser;
     this.dbPass = dbPass;
     this.logger = logger;
 }

 public void start() {
     running.set(true);
     thread = new Thread(this::runServer, "UDPServerThread");
     thread.start();
 }

 public void stop() {
     running.set(false);
     if (socket != null && !socket.isClosed()) socket.close();
 }

 private void runServer() {
     try {
         socket = new DatagramSocket(port);
         logger.accept("UDP server listening on port " + port);
         byte[] buf = new byte[512];
         while (running.get()) {
             DatagramPacket packet = new DatagramPacket(buf, buf.length);
             try {
                 socket.receive(packet);
             } catch (SocketException se) {
                 if (!running.get()) break;
                 throw se;
             }
             InetAddress addr = packet.getAddress();
             int clientPort = packet.getPort();
             String received = new String(packet.getData(), 0, packet.getLength()).trim();

             long serverReceiveTime = System.currentTimeMillis(); // t2 (server receive)
             String response = Long.toString(serverReceiveTime); // send server time back

             // send response
             byte[] respBytes = response.getBytes();
             DatagramPacket resp = new DatagramPacket(respBytes, respBytes.length, addr, clientPort);
             socket.send(resp);

             // log & save to DB
             String summary = String.format("Req from %s:%d  msg=\"%s\"  server_time=%d", addr.getHostAddress(), clientPort, received, serverReceiveTime);
             logger.accept(summary);

             // insert into DB (non-blocking small)
             insertLogToDB(addr.getHostAddress(), clientPort, serverReceiveTime);
         }
     } catch (Exception ex) {
         logger.accept("Server error: " + ex.getMessage());
         ex.printStackTrace();
     } finally {
         if (socket != null && !socket.isClosed()) socket.close();
         logger.accept("UDP server stopped.");
     }
 }

 private void insertLogToDB(String clientIp, int clientPort, long serverTime) {
     // use a new connection for each insert for simplicity
     try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
         String sql = "INSERT INTO sync_logs (client_ip, client_port, server_time) VALUES (?, ?, ?)";
         try (PreparedStatement ps = conn.prepareStatement(sql)) {
             ps.setString(1, clientIp);
             ps.setInt(2, clientPort);
             ps.setLong(3, serverTime);
             ps.executeUpdate();
         }
     } catch (SQLException e) {
         logger.accept("DB insert error: " + e.getMessage());
     }
 }
}
