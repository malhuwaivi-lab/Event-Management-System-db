import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class EMSLogin extends JFrame {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/EMSDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Milo-098";

    private JTextField txtEmail;

    public EMSLogin() {
        setTitle("Event Management System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        // Global Styling
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(250, 252, 255));
        setContentPane(root);

        // Header
        JPanel header = new GradientHeaderPanel();
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel lblTitle = new JLabel("Welcome to EMS", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblInstruction = new JLabel("Enter your Email to login:");
        lblInstruction.setHorizontalAlignment(SwingConstants.CENTER);

        txtEmail = new JTextField(20);
        txtEmail.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200)), new EmptyBorder(8, 8, 8, 8)));

        JButton btnLogin = new JButton("Login");
        stylePrimaryButton(btnLogin);

        JButton btnGuest = new JButton("Continue as Guest (No Login)");
        styleSecondaryButton(btnGuest);

        c.gridx = 0; c.gridy = 0; formPanel.add(lblInstruction, c);
        c.gridy = 1; formPanel.add(txtEmail, c);
        c.gridy = 2; c.insets = new Insets(20, 10, 5, 10); formPanel.add(btnLogin, c);
        c.gridy = 3; c.insets = new Insets(5, 10, 10, 10); formPanel.add(btnGuest, c);

        root.add(formPanel, BorderLayout.CENTER);

        // Actions
        btnLogin.addActionListener(e -> handleLogin());

        btnGuest.addActionListener(e -> {
            new EMSGuestGUI().setVisible(true);
            dispose();
        });

        root.getRootPane().setDefaultButton(btnLogin);
    }

    private void handleLogin() {
        String email = txtEmail.getText().trim();
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an email address.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT Role FROM Users WHERE Email = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String role = rs.getString("Role");
                openDashboard(role);
            } else {
                JOptionPane.showMessageDialog(this, "User not found! Try 'admin@ems.com' or create a user in DB.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
        }
    }

    private void openDashboard(String role) {
        dispose(); // Close login window
        SwingUtilities.invokeLater(() -> {
            switch (role) {
                case "Admin": new EMSAdminGUI().setVisible(true); break;
                case "Organizer": new EMSOrganizerGUI().setVisible(true); break;
                case "Attendee": new EMSGuestGUI().setVisible(true); break; // Guest also uses 'Attendee' role
                default: new EMSLogin().setVisible(true); break;
            }
        });
    }

    // Styles
    private void stylePrimaryButton(JButton btn) {
        btn.setBackground(new Color(58, 123, 213));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    private void styleSecondaryButton(JButton btn) {
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(100, 100, 100));
        btn.setFocusPainted(false);
        btn.setBorder(null);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // Shared Gradient Class
    public static class GradientHeaderPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            GradientPaint gp = new GradientPaint(0, 0, new Color(58, 123, 213), getWidth(), getHeight(), new Color(58, 213, 173));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EMSLogin().setVisible(true));
    }
}