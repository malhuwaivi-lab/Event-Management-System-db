import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;

public class EMSOrganizerGUI extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/EMSDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Milo-098";

    private DefaultTableModel eventModel, ticketModel, bookingModel;
    private JTextField tfOrgId; // Organizer identifies themselves here

    public EMSOrganizerGUI() {
        setTitle("EMS - Organizer Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);

        JPanel header = new EMSLogin.GradientHeaderPanel();
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        idPanel.setOpaque(false);
        idPanel.add(new JLabel("My Organizer ID:"));
        tfOrgId = new JTextField("1", 4); // Default to ID 1 for testing
        idPanel.add(tfOrgId);

        JLabel lblTitle = new JLabel("Organizer Dashboard");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> { dispose(); new EMSLogin().setVisible(true); });

        header.add(lblTitle, BorderLayout.WEST);
        header.add(idPanel, BorderLayout.CENTER);
        header.add(btnLogout, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("My Events", createEventPanel());
        tabs.addTab("Manage Tickets", createTicketPanel());
        tabs.addTab("My Bookings", createBookingPanel());
        root.add(tabs, BorderLayout.CENTER);
    }

    private JPanel createEventPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel input = new JPanel(new GridLayout(2, 6, 5, 5));

        JTextField tfTitle = new JTextField();
        JTextField tfDate = new JTextField(10);
        JTextField tfTime = new JTextField("12:00:00");
        JTextField tfLoc = new JTextField();
        JButton btnAdd = new JButton("Create Event");
        JButton btnRefresh = new JButton("Refresh");

        input.add(new JLabel("Title:")); input.add(tfTitle);
        input.add(new JLabel("Date:")); input.add(tfDate);
        input.add(new JLabel("Time:")); input.add(tfTime);
        input.add(new JLabel("Location:")); input.add(tfLoc);
        input.add(btnAdd); input.add(btnRefresh);
        panel.add(input, BorderLayout.NORTH);

        eventModel = new DefaultTableModel(new String[]{"ID", "Title", "Date", "Status"}, 0);
        panel.add(new JScrollPane(new JTable(eventModel)), BorderLayout.CENTER);

        Runnable load = () -> {
            eventModel.setRowCount(0);
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement("SELECT EventID, Title, EventDate, ApprovalStatus FROM Events WHERE OrganizerID=?")) {
                ps.setInt(1, Integer.parseInt(tfOrgId.getText()));
                ResultSet rs = ps.executeQuery();
                while(rs.next()) eventModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
            } catch (Exception e) { e.printStackTrace(); }
        };

        btnAdd.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO Events(OrganizerID, Title, EventDate, EventTime, Location, ApprovalStatus) VALUES(?,?,?,?,?,'Pending')")) {
                ps.setInt(1, Integer.parseInt(tfOrgId.getText()));
                ps.setString(2, tfTitle.getText());
                ps.setString(3, tfDate.getText());
                ps.setString(4, tfTime.getText());
                ps.setString(5, tfLoc.getText());
                ps.executeUpdate();
                load.run();
                JOptionPane.showMessageDialog(this, "Event Created (Status: Pending)");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        });

        btnRefresh.addActionListener(e -> load.run());
        return panel;
    }

    private JPanel createTicketPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel input = new JPanel(new FlowLayout());

        JTextField tfEvtId = new JTextField(4);
        JTextField tfType = new JTextField(8);
        JTextField tfPrice = new JTextField(6);
        JTextField tfQty = new JTextField(4);
        JButton btnAdd = new JButton("Add Tickets");
        JButton btnRefresh = new JButton("Refresh");

        input.add(new JLabel("Event ID:")); input.add(tfEvtId);
        input.add(new JLabel("Type:")); input.add(tfType);
        input.add(new JLabel("Price:")); input.add(tfPrice);
        input.add(new JLabel("Qty:")); input.add(tfQty);
        input.add(btnAdd); input.add(btnRefresh);
        panel.add(input, BorderLayout.NORTH);

        ticketModel = new DefaultTableModel(new String[]{"TicketID", "Event", "Type", "Price", "Qty"}, 0);
        panel.add(new JScrollPane(new JTable(ticketModel)), BorderLayout.CENTER);

        Runnable load = () -> {
            ticketModel.setRowCount(0);
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT t.TicketID, e.Title, t.Type, t.Price, t.Quantity FROM Tickets t JOIN Events e ON t.EventID=e.EventID WHERE e.OrganizerID=?")) {
                ps.setInt(1, Integer.parseInt(tfOrgId.getText()));
                ResultSet rs = ps.executeQuery();
                while(rs.next()) ticketModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getInt(5)});
            } catch (Exception e) { e.printStackTrace(); }
        };

        btnAdd.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO Tickets(EventID, Type, Price, Quantity) VALUES(?,?,?,?)")) {
                ps.setInt(1, Integer.parseInt(tfEvtId.getText()));
                ps.setString(2, tfType.getText());
                ps.setBigDecimal(3, new BigDecimal(tfPrice.getText()));
                ps.setInt(4, Integer.parseInt(tfQty.getText()));
                ps.executeUpdate();
                load.run();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        });

        btnRefresh.addActionListener(e -> load.run());
        return panel;
    }

    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel controls = new JPanel();
        JButton btnRefresh = new JButton("Refresh");
        JButton btnPaid = new JButton("Mark Paid");
        JButton btnFailed = new JButton("Mark Failed");
        controls.add(btnRefresh); controls.add(btnPaid); controls.add(btnFailed);
        panel.add(controls, BorderLayout.NORTH);

        bookingModel = new DefaultTableModel(new String[]{"BookingID", "Guest", "Event", "Ticket", "Status"}, 0);
        JTable table = new JTable(bookingModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        Runnable load = () -> {
            bookingModel.setRowCount(0);
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT b.BookingID, u.Name, e.Title, t.Type, b.Status FROM Bookings b " +
                                 "JOIN BookingItems bi ON b.BookingID=bi.BookingID JOIN Tickets t ON bi.TicketID=t.TicketID " +
                                 "JOIN Events e ON t.EventID=e.EventID JOIN Users u ON b.UserID=u.UserID WHERE e.OrganizerID=?")) {
                ps.setInt(1, Integer.parseInt(tfOrgId.getText()));
                ResultSet rs = ps.executeQuery();
                while(rs.next()) bookingModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            } catch (Exception e) { e.printStackTrace(); }
        };

        ActionListener updateStatus = e -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement("UPDATE Bookings SET Status=? WHERE BookingID=?")) {
                ps.setString(1, e.getActionCommand()); // Paid or Failed
                ps.setInt(2, (int) bookingModel.getValueAt(row, 0));
                ps.executeUpdate();
                load.run();
            } catch (Exception ex) { ex.printStackTrace(); }
        };

        btnPaid.setActionCommand("Paid"); btnPaid.addActionListener(updateStatus);
        btnFailed.setActionCommand("Failed"); btnFailed.addActionListener(updateStatus);
        btnRefresh.addActionListener(e -> load.run());
        return panel;
    }
}