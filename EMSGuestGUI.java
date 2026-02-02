import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.math.BigDecimal;

public class EMSGuestGUI extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/EMSDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Milo-098";

    private DefaultTableModel eventsModel;
    private JComboBox<String> cbEvents; // Holds "ID: Name" string
    private JComboBox<TicketItem> cbTickets; // Holds TicketItem objects (Cleaner display)
    private JTextField tfName, tfEmail;
    private JSpinner spQty;

    public EMSGuestGUI() {
        setTitle("EMS - Guest Portal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);

        // Header
        JPanel header = new EMSLogin.GradientHeaderPanel();
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel lblTitle = new JLabel("Browse & Book Events");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);
        JButton btnLogout = new JButton("Back to Login");
        btnLogout.addActionListener(e -> { dispose(); new EMSLogin().setVisible(true); });

        header.add(lblTitle, BorderLayout.WEST);
        header.add(btnLogout, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Browse Events", createBrowsePanel());
        tabs.addTab("Buy Tickets", createBuyPanel());
        tabs.addTab("My Bookings", createMyBookingsPanel());
        root.add(tabs, BorderLayout.CENTER);
    }

    // --- Tab 1: Browse ---
    private JPanel createBrowsePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        eventsModel = new DefaultTableModel(new String[]{"EventID", "Title", "Date", "Location"}, 0);
        panel.add(new JScrollPane(new JTable(eventsModel)), BorderLayout.CENTER);

        JButton btnRefresh = new JButton("Refresh Approved Events");
        panel.add(btnRefresh, BorderLayout.NORTH);

        btnRefresh.addActionListener(e -> loadEvents());
        loadEvents();
        return panel;
    }

    private void loadEvents() {
        eventsModel.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT EventID, Title, EventDate, Location FROM Events WHERE ApprovalStatus='Approved'")) {
            while(rs.next()) eventsModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getDate(3), rs.getString(4)});
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- Tab 2: Buy Tickets ---
    private JPanel createBuyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5); c.fill = GridBagConstraints.HORIZONTAL;

        cbEvents = new JComboBox<>();
        cbTickets = new JComboBox<>(); // Now uses TicketItem
        spQty = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        tfName = new JTextField(15);
        tfEmail = new JTextField(15);
        JButton btnLoadTickets = new JButton("Load Ticket Types");
        JButton btnBuy = new JButton("Confirm Purchase");

        // Layout
        c.gridx=0; c.gridy=0; panel.add(new JLabel("1. Select Event:"), c);
        c.gridx=1; panel.add(cbEvents, c);
        c.gridx=2; panel.add(btnLoadTickets, c);

        c.gridx=0; c.gridy=1; panel.add(new JLabel("2. Ticket Type:"), c);
        c.gridx=1; panel.add(cbTickets, c);

        c.gridx=0; c.gridy=2; panel.add(new JLabel("3. Quantity:"), c);
        c.gridx=1; panel.add(spQty, c);

        c.gridx=0; c.gridy=3; panel.add(new JLabel("4. Name:"), c);
        c.gridx=1; panel.add(tfName, c);

        c.gridx=0; c.gridy=4; panel.add(new JLabel("5. Email:"), c);
        c.gridx=1; panel.add(tfEmail, c);

        c.gridx=1; c.gridy=5; panel.add(btnBuy, c);

        // Logic
        loadEventComboBox();

        btnLoadTickets.addActionListener(e -> {
            cbTickets.removeAllItems();
            String evtStr = (String) cbEvents.getSelectedItem();
            if (evtStr == null) return;

            int evtId = Integer.parseInt(evtStr.split(":")[0]);

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement("SELECT TicketID, Type, Price FROM Tickets WHERE EventID=?")) {
                ps.setInt(1, evtId);
                ResultSet rs = ps.executeQuery();
                while(rs.next()) {
                    // Create a clean TicketItem object instead of a raw string
                    TicketItem item = new TicketItem(
                            rs.getInt("TicketID"),
                            rs.getString("Type"),
                            rs.getBigDecimal("Price")
                    );
                    cbTickets.addItem(item);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        btnBuy.addActionListener(e -> handlePurchase());
        return panel;
    }

    private void loadEventComboBox() {
        cbEvents.removeAllItems();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT EventID, Title FROM Events WHERE ApprovalStatus='Approved'")) {
            while(rs.next()) cbEvents.addItem(rs.getInt(1) + ": " + rs.getString(2));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handlePurchase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Get selected TicketItem object
            TicketItem selectedTicket = (TicketItem) cbTickets.getSelectedItem();

            if (selectedTicket == null || tfEmail.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a ticket and enter email.");
                return;
            }

            int ticketId = selectedTicket.getId(); // Hidden ID
            int qty = (int) spQty.getValue();

            // 1. Get/Create User
            int userId = getOrCreateUser(conn, tfName.getText(), tfEmail.getText());

            // 2. Create Booking
            PreparedStatement psBk = conn.prepareStatement("INSERT INTO Bookings(UserID, Status) VALUES(?, 'Pending')", Statement.RETURN_GENERATED_KEYS);
            psBk.setInt(1, userId);
            psBk.executeUpdate();
            ResultSet rsBk = psBk.getGeneratedKeys(); rsBk.next();
            int bookingId = rsBk.getInt(1);

            // 3. Add Item & Reduce Qty
            PreparedStatement psItem = conn.prepareStatement("INSERT INTO BookingItems(BookingID, TicketID, Quantity) VALUES(?,?,?)");
            psItem.setInt(1, bookingId); psItem.setInt(2, ticketId); psItem.setInt(3, qty);
            psItem.executeUpdate();

            conn.prepareStatement("UPDATE Tickets SET Quantity=Quantity-" + qty + " WHERE TicketID=" + ticketId).executeUpdate();

            JOptionPane.showMessageDialog(this, "Success! Booking ID: " + bookingId);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
    }

    private int getOrCreateUser(Connection conn, String name, String email) throws SQLException {
        PreparedStatement psCheck = conn.prepareStatement("SELECT UserID FROM Users WHERE Email=?");
        psCheck.setString(1, email);
        ResultSet rs = psCheck.executeQuery();
        if (rs.next()) return rs.getInt(1);

        PreparedStatement psIns = conn.prepareStatement("INSERT INTO Users(Name, Email, Role) VALUES(?, ?, 'Attendee')", Statement.RETURN_GENERATED_KEYS);
        psIns.setString(1, name); psIns.setString(2, email);
        psIns.executeUpdate();
        ResultSet rsKey = psIns.getGeneratedKeys(); rsKey.next();
        return rsKey.getInt(1);
    }

    // --- Tab 3: My Bookings ---
    private JPanel createMyBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel();
        JTextField tfBookingId = new JTextField(10);
        JButton btnFind = new JButton("Find Booking");

        top.add(new JLabel("Enter Booking ID:")); top.add(tfBookingId); top.add(btnFind);
        panel.add(top, BorderLayout.NORTH);

        DefaultTableModel myModel = new DefaultTableModel(new String[]{"BookingID", "Event", "Date", "TicketType", "Status"}, 0);
        panel.add(new JScrollPane(new JTable(myModel)), BorderLayout.CENTER);

        btnFind.addActionListener(e -> {
                    String inputId = tfBookingId.getText().trim();
                    if(inputId.isEmpty()) return;

                    myModel.setRowCount(0); // Clear table

                    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                         PreparedStatement ps = conn.prepareStatement(
                                 "SELECT b.BookingID, e.Title, b.BookingDate, t.Type, b.Status FROM Bookings b " +
                                         "JOIN BookingItems bi ON b.BookingID = bi.BookingID " +
                                         "JOIN Tickets t ON bi.TicketID = t.TicketID " +
                                         "JOIN Events e ON t.EventID = e.EventID " +
                                         "WHERE b.BookingID = ?")) {

                        ps.setInt(1, Integer.parseInt(inputId));
                        ResultSet rs = ps.executeQuery();

                        boolean found = false;
                        while(rs.next()) {
                            myModel.addRow(new Object[]{
                                    rs.getInt(1),         // BookingID
                                    rs.getString(2),      // Event Title
                                    rs.getTimestamp(3),   // Date
                                    rs.getString(4),      // Ticket Type (This was missing!)
                                    rs.getString(5)       // Status
                            });
                            found = true;
                        }
                if(!found) JOptionPane.showMessageDialog(this, "No booking found.");

            } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Invalid ID"); }
        });
        return panel;
    }

    // --- Helper Class to Hide IDs ---
    private static class TicketItem {
        private int id;
        private String name;
        private BigDecimal price;

        public TicketItem(int id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public int getId() { return id; }

        // This toString() determines what is shown in the Dropdown
        @Override
        public String toString() {
            return name + " ($" + price + ")";
        }
    }
}