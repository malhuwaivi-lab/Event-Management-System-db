import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class EMSAdminGUI extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/EMSDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Milo-098";

    private DefaultTableModel organizerModel, eventModel, bookingModel;

    public EMSAdminGUI() {
        setTitle("EMS - Admin Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);

        // Header
        JPanel header = new EMSLogin.GradientHeaderPanel();
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel lblTitle = new JLabel("Admin Control Panel");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);
        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> { dispose(); new EMSLogin().setVisible(true); });

        header.add(lblTitle, BorderLayout.WEST);
        header.add(btnLogout, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Manage Organizers", createOrganizerPanel());
        tabs.addTab("Approve Events", createEventPanel());
        tabs.addTab("View All Bookings", createBookingPanel());
        root.add(tabs, BorderLayout.CENTER);
    }

    private JPanel createOrganizerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel input = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField tfName = new JTextField(10);
        JTextField tfEmail = new JTextField(10);
        JButton btnAdd = new JButton("Add Organizer");
        JButton btnRefresh = new JButton("Refresh");

        input.add(new JLabel("Name:")); input.add(tfName);
        input.add(new JLabel("Email:")); input.add(tfEmail);
        input.add(btnAdd); input.add(btnRefresh);
        panel.add(input, BorderLayout.NORTH);

        organizerModel = new DefaultTableModel(new String[]{"UserID", "Name", "Email"}, 0);
        JTable table = new JTable(organizerModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Logic
        Runnable load = () -> {
            organizerModel.setRowCount(0);
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT UserID, Name, Email FROM Users WHERE Role='Organizer'")) {
                while(rs.next()) organizerModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3)});
            } catch (SQLException e) { e.printStackTrace(); }
        };

        btnAdd.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO Users(Name, Email, Role) VALUES(?, ?, 'Organizer')")) {
                ps.setString(1, tfName.getText());
                ps.setString(2, tfEmail.getText());
                ps.executeUpdate();
                load.run();
                tfName.setText(""); tfEmail.setText("");
                JOptionPane.showMessageDialog(this, "Organizer Added!");
            } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        });

        btnRefresh.addActionListener(e -> load.run());
        load.run();
        return panel;
    }

    private JPanel createEventPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnApprove = new JButton("Approve Selected");
        JButton btnReject = new JButton("Reject Selected");
        JButton btnRefresh = new JButton("Refresh List");
        controls.add(btnRefresh); controls.add(btnApprove); controls.add(btnReject);
        panel.add(controls, BorderLayout.NORTH);

        eventModel = new DefaultTableModel(new String[]{"EventID", "OrganizerID", "Title", "ApprovalStatus"}, 0);
        JTable table = new JTable(eventModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        Runnable load = () -> {
            eventModel.setRowCount(0);
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT EventID, OrganizerID, Title, ApprovalStatus FROM Events")) {
                while(rs.next()) eventModel.addRow(new Object[]{rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4)});
            } catch (SQLException e) { e.printStackTrace(); }
        };

        ActionListener updateAction = e -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            String status = e.getActionCommand(); // "Approved" or "Rejected"
            int id = (int) eventModel.getValueAt(row, 0);
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement("UPDATE Events SET ApprovalStatus=? WHERE EventID=?")) {
                ps.setString(1, status);
                ps.setInt(2, id);
                ps.executeUpdate();
                load.run();
            } catch (SQLException ex) { ex.printStackTrace(); }
        };

        btnApprove.setActionCommand("Approved");
        btnApprove.addActionListener(updateAction);
        btnReject.setActionCommand("Rejected");
        btnReject.addActionListener(updateAction);
        btnRefresh.addActionListener(e -> load.run());
        load.run();
        return panel;
    }

    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        bookingModel = new DefaultTableModel(new String[]{"BookingID", "User", "Date", "Status"}, 0);
        JTable table = new JTable(bookingModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnRefresh = new JButton("Refresh Bookings");
        panel.add(btnRefresh, BorderLayout.NORTH);

        btnRefresh.addActionListener(e -> {
            bookingModel.setRowCount(0);
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT b.BookingID, u.Name, b.BookingDate, b.Status FROM Bookings b JOIN Users u ON b.UserID = u.UserID")) {
                while(rs.next()) bookingModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getTimestamp(3), rs.getString(4)});
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        btnRefresh.doClick();
        return panel;
    }
}