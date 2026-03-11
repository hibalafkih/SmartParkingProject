package view;

import database.*;
import util.ThemeManager;
import util.NotificationManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.File;

/**
 * Fenêtre historique des paiements avec export CSV & PDF.
 */
public class HistoriqueFrame extends JDialog {

    private JTable table;
    private DefaultTableModel model;
    private double totalGlobal = 0;
    private Frame parent;

    public HistoriqueFrame(Frame parent) {
        super(parent, "Historique des Paiements", true);
        this.parent = parent;
        setSize(750, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeManager.bg());

        buildUI();
        chargerHistorique();
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        header.setBackground(ThemeManager.card());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.border()));
        JLabel title = new JLabel("  Historique des Paiements");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(ThemeManager.text());
        header.add(title);
        add(header, BorderLayout.NORTH);

        // Tableau
        model = new DefaultTableModel(
                new String[]{"ID", "Matricule", "Durée (min)", "Montant (DH)", "Date Sortie"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable();

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(ThemeManager.card());
        add(sp, BorderLayout.CENTER);

        // Boutons
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        south.setBackground(ThemeManager.bg());

        JButton btnCSV    = ThemeManager.createButton(" Exporter CSV",  ThemeManager.ACCENT_GREEN,  Color.WHITE);
        JButton btnPDF    = ThemeManager.createButton("  Imprimer/PDF",  ThemeManager.ACCENT_PURPLE, Color.WHITE);
        JButton btnFermer = ThemeManager.createButton("X  Fermer",        ThemeManager.DARK_CARD2,    ThemeManager.DARK_TEXT);

        btnCSV.addActionListener(e -> exporterCSV());
        btnPDF.addActionListener(e -> TicketService.exporterPDF(table, parent));
        btnFermer.addActionListener(e -> dispose());

        south.add(btnCSV);
        south.add(btnPDF);
        south.add(btnFermer);
        add(south, BorderLayout.SOUTH);
    }

    private void styleTable() {
        table.setBackground(ThemeManager.card());
        table.setForeground(ThemeManager.text());
        table.setGridColor(ThemeManager.border());
        table.setRowHeight(38);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(ThemeManager.card2());
        table.getTableHeader().setForeground(ThemeManager.textMuted());
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setSelectionBackground(new Color(88, 166, 255, 50));
        table.setSelectionForeground(ThemeManager.text());

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < 5; i++) table.getColumnModel().getColumn(i).setCellRenderer(center);
    }

    private void chargerHistorique() {
        ParkingDAO dao = new ParkingDAO();
        ResultSet rs = dao.getHistorique();
        totalGlobal = 0;
        try {
            while (rs != null && rs.next()) {
                double montant = rs.getDouble("montant_paye");
                totalGlobal += montant;
                model.addRow(new Object[]{
                        rs.getInt("id_paiement"),
                        rs.getString("immatriculation"),
                        rs.getInt("duree_minutes"),
                        String.format("%.2f DH", montant),
                        rs.getTimestamp("date_sortie")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        setTitle(String.format("Historique — Chiffre d'Affaires Total : %.2f DH", totalGlobal));
    }

    private void exporterCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("historique_parking.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            ParkingDAO dao = new ParkingDAO();
            ResultSet rs = dao.getHistorique();
            boolean ok = TicketService.exporterCSV(rs, fc.getSelectedFile().getAbsolutePath());
            if (ok) NotificationManager.show(parent, "Export CSV réussi !", NotificationManager.Type.SUCCESS);
            else    NotificationManager.show(parent, "Erreur lors de l'export.", NotificationManager.Type.ERROR);
        }
    }
}