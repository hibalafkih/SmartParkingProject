package view;

import database.ParkingDAO;
import util.ThemeManager;
import util.NotificationManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Fenêtre de gestion des abonnements mensuels.
 */
public class AbonnementsFrame extends JDialog {

    private JTable table;
    private DefaultTableModel model;
    private ParkingDAO dao = new ParkingDAO();
    private Frame parent;

    public AbonnementsFrame(Frame parent) {
        super(parent, "Gestion des Abonnements", true);
        this.parent = parent;
        setSize(700, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(ThemeManager.bg());

        buildUI();
        charger();
    }

    private void buildUI() {
        // ---- Header ----
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        header.setBackground(ThemeManager.card());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.border()));

        JLabel title = new JLabel("  Abonnements Mensuels");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(ThemeManager.text());
        header.add(title);
        add(header, BorderLayout.NORTH);

        // ---- Tableau ----
        model = new DefaultTableModel(
                new String[]{"ID", "Matricule", "Nom Client", "Début", "Fin", "Jours Restants"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable();

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(ThemeManager.card());
        add(sp, BorderLayout.CENTER);

        // ---- Boutons ----
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        south.setBackground(ThemeManager.bg());

        JButton btnAjouter   = ThemeManager.createButton("+ Nouvel Abonnement", ThemeManager.ACCENT_BLUE, Color.WHITE);
        JButton btnSupprimer = ThemeManager.createButton("  Supprimer",          ThemeManager.ACCENT_RED,  Color.WHITE);
        JButton btnFermer    = ThemeManager.createButton("X  Fermer",             ThemeManager.DARK_CARD2,  ThemeManager.DARK_TEXT);

        btnAjouter.addActionListener(e -> formulaireAjout());
        btnSupprimer.addActionListener(e -> supprimer());
        btnFermer.addActionListener(e -> dispose());

        south.add(btnAjouter);
        south.add(btnSupprimer);
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

        // Colonne "Jours Restants" colorée
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                setBackground(ThemeManager.card());
                try {
                    int jours = Integer.parseInt(value.toString());
                    if (jours <= 0) {
                        setForeground(ThemeManager.ACCENT_RED);
                        setText("Expiré");
                    } else if (jours <= 7) {
                        setForeground(ThemeManager.ACCENT_ORANGE);
                        setText(jours + " jours [!]");
                    } else {
                        setForeground(ThemeManager.ACCENT_GREEN);
                        setText(jours + " jours [OK]");
                    }
                } catch (NumberFormatException e) {
                    setForeground(ThemeManager.textMuted());
                }
                setHorizontalAlignment(CENTER);
                return this;
            }
        });
    }

    private void charger() {
        model.setRowCount(0);
        ResultSet rs = dao.getTousAbonnements();
        try {
            while (rs != null && rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id_abonnement"),
                        rs.getString("matricule"),
                        rs.getString("nom_client"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin"),
                        rs.getInt("jours_restants")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void formulaireAjout() {
        JTextField txtMat  = new JTextField(12);
        JTextField txtNom  = new JTextField(18);
        JComboBox<String> comboDuree = new JComboBox<>(new String[]{"30 jours (1 mois)", "60 jours (2 mois)", "90 jours (3 mois)", "365 jours (1 an)"});

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBackground(ThemeManager.card());
        form.setForeground(ThemeManager.text());
        form.add(label("Matricule :"));      form.add(txtMat);
        form.add(label("Nom client :"));     form.add(txtNom);
        form.add(label("Durée :"));          form.add(comboDuree);

        int r = JOptionPane.showConfirmDialog(this, form, "Nouvel Abonnement",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (r == JOptionPane.OK_OPTION) {
            String mat  = txtMat.getText().trim().toUpperCase();
            String nom  = txtNom.getText().trim();
            int duree   = new int[]{30, 60, 90, 365}[comboDuree.getSelectedIndex()];

            if (mat.isEmpty() || nom.isEmpty()) {
                NotificationManager.show(parent, "Veuillez remplir tous les champs.", NotificationManager.Type.WARNING);
                return;
            }
            if (dao.ajouterAbonnement(mat, nom, duree)) {
                NotificationManager.show(parent, "Abonnement créé pour " + mat + " (" + duree + " jours)", NotificationManager.Type.SUCCESS);
                charger();
            } else {
                NotificationManager.show(parent, "Erreur lors de la création.", NotificationManager.Type.ERROR);
            }
        }
    }

    private void supprimer() {
        int row = table.getSelectedRow();
        if (row < 0) {
            NotificationManager.show(parent, "Sélectionnez un abonnement à supprimer.", NotificationManager.Type.WARNING);
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Supprimer l'abonnement ID " + id + " ?", "Confirmer", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (dao.supprimerAbonnement(id)) {
                NotificationManager.show(parent, "Abonnement supprimé.", NotificationManager.Type.SUCCESS);
                charger();
            }
        }
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(ThemeManager.text());
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }
}