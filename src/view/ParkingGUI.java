package view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import util.ThemeManager;
import util.NotificationManager;
import database.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/**
 * Dashboard principal — Smart Parking System v3.0
 * Avec : Dark Mode, Plan visuel, Statistiques, Abonnements, Recherche, Export
 */
public class ParkingGUI extends JFrame {

    // === Composants principaux ===
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblLibres, lblOccupes, lblRevJour, lblVehJour;
    private JProgressBar barOccupation;
    private ParkingMapPanel mapPanel;
    private StatsPanel statsPanel;
    private JTextField searchField;
    private JLabel searchResult;
    private JToggleButton btnTheme;
    private JTabbedPane tabs;

    private ParkingDAO dao = new ParkingDAO();

    // =========================================================================
    public ParkingGUI() {
        setTitle("Smart Parking System v3.0");
        setSize(1150, 780);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 650));

        buildUI();
        chargerDonnees();

        // Auto-refresh toutes les 30 secondes
        Timer autoRefresh = new Timer(30_000, e -> chargerDonnees());
        autoRefresh.start();
    }

    // =========================================================================
    private void buildUI() {
        applyBackground();

        setLayout(new BorderLayout(0, 0));
        add(buildHeader(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    // =========================================================================
    // HEADER
    // =========================================================================
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, ThemeManager.ACCENT_BLUE.darker().darker(),
                        getWidth(), 0, new Color(41, 98, 255, 200)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 70));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        // Gauche : logo + titre
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        JLabel logo = new JLabel("P") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 4, 38, 38, 8, 8);
                g2.setColor(ThemeManager.ACCENT_BLUE.darker());
                g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("P", (38 - fm.stringWidth("P")) / 2, 4 + (38 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(38, 46));
        logo.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 0));
        titles.setOpaque(false);
        JLabel t1 = new JLabel("Smart Parking System");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 18));
        t1.setForeground(Color.WHITE);
        JLabel t2 = new JLabel("Dashboard de Gestion");
        t2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        t2.setForeground(new Color(255, 255, 255, 180));
        titles.add(t1); titles.add(t2);
        left.add(logo); left.add(titles);
        header.add(left, BorderLayout.WEST);

        // Droite : recherche + toggle thème
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 18));
        right.setOpaque(false);

        searchField = new JTextField(14);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchField.putClientProperty("JTextField.placeholderText", "Rechercher matricule...");
        searchField.addActionListener(e -> rechercherVehicule());

        JButton btnSearch = ThemeManager.createButton("Chercher", new Color(255, 255, 255, 60), Color.WHITE);
        btnSearch.setPreferredSize(new Dimension(90, 34));
        btnSearch.addActionListener(e -> rechercherVehicule());

        btnTheme = new JToggleButton("Mode Dark");
        btnTheme.setSelected(true);
        btnTheme.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnTheme.setForeground(Color.WHITE);
        btnTheme.setBackground(new Color(255, 255, 255, 40));
        btnTheme.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTheme.setBorderPainted(false);
        btnTheme.setFocusPainted(false);
        btnTheme.setPreferredSize(new Dimension(90, 34));
        btnTheme.addActionListener(e -> toggleTheme());

        right.add(searchField);
        right.add(btnSearch);
        right.add(btnTheme);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // =========================================================================
    // KPI CARDS
    // =========================================================================
    private JPanel buildKpiCards() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 14, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 20, 8, 20));

        lblLibres   = createKpiCard("PLACES LIBRES",   "—", ThemeManager.ACCENT_GREEN,  "FREE");
        lblOccupes  = createKpiCard("PLACES OCCUPEES", "—", ThemeManager.ACCENT_RED,    "OCC");
        lblRevJour  = createKpiCard("REVENUS DU JOUR", "—", ThemeManager.ACCENT_BLUE,   "REV");
        lblVehJour  = createKpiCard("PASSAGES / JOUR", "—", ThemeManager.ACCENT_ORANGE, "VEH");

        panel.add(wrapCard(lblLibres));
        panel.add(wrapCard(lblOccupes));
        panel.add(wrapCard(lblRevJour));
        panel.add(wrapCard(lblVehJour));

        return panel;
    }

    private JLabel createKpiCard(String titre, String valeur, Color accent, String icon) {
        JLabel lbl = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fond carte
                g2.setColor(ThemeManager.card());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // Barre accent bas
                g2.setColor(accent);
                g2.fillRoundRect(0, getHeight() - 4, getWidth(), 4, 4, 4);
                // Bordure subtile
                g2.setColor(ThemeManager.border());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                // Icône dessinée en Java2D (sans emoji)
                drawIcon(g2, icon, 14, 14, 22, accent);
                // Titre
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(ThemeManager.textMuted());
                g2.drawString(titre, 14, 55);
                // Valeur
                String val = getText();
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g2.setColor(accent);
                g2.drawString(val, 14, 82);
                g2.dispose();
            }
        };
        lbl.setText(valeur);
        lbl.setPreferredSize(new Dimension(0, 100));
        lbl.setOpaque(false);
        lbl.putClientProperty("titre", titre);
        lbl.putClientProperty("icon", icon);
        lbl.putClientProperty("accent", accent);
        return lbl;
    }

    private JPanel wrapCard(JLabel lbl) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    private void drawIcon(Graphics2D g2, String code, int x, int y, int size, Color color) {
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
        g2.fillRoundRect(x, y, size, size, 6, 6);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));
        int p = 4;
        switch (code) {
            case "FREE":
                g2.drawOval(x + p, y + p, size - p*2, size - p*2);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 7, y + size/2, x + size/2 - 1, y + size - 6);
                g2.drawLine(x + size/2 - 1, y + size - 6, x + size - 5, y + 6);
                break;
            case "OCC":
                g2.drawOval(x + p, y + p, size - p*2, size - p*2);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 7, y + 7, x + size - 7, y + size - 7);
                g2.drawLine(x + size - 7, y + 7, x + 7, y + size - 7);
                break;
            case "REV":
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x + p, y + p, size - p*2, size - p*2, 4, 4);
                g2.setFont(new Font("Segoe UI", Font.BOLD, size - 8));
                g2.drawString("DH", x + p + 1, y + size - p - 1);
                break;
            case "VEH":
                g2.setStroke(new BasicStroke(2f));
                int bx = x + 2, by = y + size/2, bw = size - 4, bh = size/3;
                g2.drawRoundRect(bx, by, bw, bh, 4, 4);
                g2.drawRoundRect(bx + 4, y + 4, bw - 8, size/2 - 2, 4, 4);
                g2.fillOval(bx + 3,     by + bh - 3, 6, 6);
                g2.fillOval(bx + bw - 9, by + bh - 3, 6, 6);
                break;
            default:
                g2.setFont(new Font("Segoe UI", Font.BOLD, size - 6));
                g2.drawString(code.substring(0, Math.min(2, code.length())), x + p, y + size - p);
        }
    }

    // =========================================================================
    // CONTENU PRINCIPAL (onglets)
    // =========================================================================
    private JComponent buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        wrapper.add(buildKpiCards(), BorderLayout.NORTH);

        JPanel barPanel = new JPanel(new BorderLayout(8, 0));
        barPanel.setOpaque(false);
        barPanel.setBorder(new EmptyBorder(0, 20, 8, 20));
        JLabel lblBar = new JLabel("Taux d'occupation :");
        lblBar.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblBar.setForeground(ThemeManager.textMuted());
        barOccupation = new JProgressBar(0, 100);
        barOccupation.setStringPainted(true);
        barOccupation.setForeground(ThemeManager.ACCENT_BLUE);
        barOccupation.setBackground(ThemeManager.card2());
        barOccupation.setFont(new Font("Segoe UI", Font.BOLD, 11));
        barOccupation.setPreferredSize(new Dimension(0, 24));
        barPanel.add(lblBar, BorderLayout.WEST);
        barPanel.add(barOccupation, BorderLayout.CENTER);
        wrapper.add(barPanel, BorderLayout.CENTER);

        tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.setBackground(ThemeManager.bg());
        tabs.setForeground(ThemeManager.text());

        tabs.addTab("Vehicules",   buildTableTab());
        tabs.addTab("Plan",        buildMapTab());
        tabs.addTab("Statistiques", buildStatsTab());

        JPanel tabWrapper = new JPanel(new BorderLayout());
        tabWrapper.setOpaque(false);
        tabWrapper.setBorder(new EmptyBorder(0, 14, 0, 14));
        tabWrapper.add(tabs, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(barPanel, BorderLayout.NORTH);
        south.add(tabWrapper, BorderLayout.CENTER);

        wrapper.add(south, BorderLayout.SOUTH);

        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setOpaque(false);
        main.add(buildKpiCards(), BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.setOpaque(false);
        mid.add(barPanel, BorderLayout.NORTH);
        mid.add(tabWrapper, BorderLayout.CENTER);
        main.add(mid, BorderLayout.CENTER);

        return main;
    }

    private JPanel buildTableTab() {
        tableModel = new DefaultTableModel(new String[]{"Matricule", "Place", "Heure Entree", "Statut"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleMainTable();

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(ThemeManager.card());

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(ThemeManager.card());
        p.add(sp, BorderLayout.CENTER);

        searchResult = new JLabel(" ");
        searchResult.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        searchResult.setForeground(ThemeManager.ACCENT_CYAN);
        searchResult.setBorder(new EmptyBorder(6, 12, 6, 12));
        p.add(searchResult, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildMapTab() {
        mapPanel = new ParkingMapPanel();
        JScrollPane sp = new JScrollPane(mapPanel);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(ThemeManager.bg());

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(ThemeManager.bg());
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildStatsTab() {
        statsPanel = new StatsPanel();
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(10, 0, 10, 0));
        p.add(statsPanel, BorderLayout.CENTER);
        return p;
    }

    private void styleMainTable() {
        table.setBackground(ThemeManager.card());
        table.setForeground(ThemeManager.text());
        table.setGridColor(ThemeManager.border());
        table.setRowHeight(44);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(ThemeManager.card2());
        table.getTableHeader().setForeground(ThemeManager.textMuted());
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(88, 166, 255, 60));
        table.setSelectionForeground(ThemeManager.text());

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(ThemeManager.card());
                String s = val != null ? val.toString() : "";
                if (s.contains("ABONNÉ")) { setForeground(ThemeManager.ACCENT_PURPLE); }
                else                      { setForeground(ThemeManager.ACCENT_CYAN); }
                setHorizontalAlignment(CENTER);
                return this;
            }
        });
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < 3; i++) table.getColumnModel().getColumn(i).setCellRenderer(center);
    }

    // =========================================================================
    // FOOTER (boutons d'action)
    // =========================================================================
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 12));
        footer.setBackground(ThemeManager.card());
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.border()));

        JButton btnEntree     = ThemeManager.createButton(">> ENTREE",      ThemeManager.ACCENT_GREEN,  Color.WHITE);
        JButton btnSortie     = ThemeManager.createButton("<< SORTIE",      ThemeManager.ACCENT_RED,    Color.WHITE);
        JButton btnAbonnement = ThemeManager.createButton("ABONNEMENTS",    ThemeManager.ACCENT_PURPLE, Color.WHITE);
        JButton btnHistorique = ThemeManager.createButton("HISTORIQUE",     ThemeManager.ACCENT_BLUE,   Color.WHITE);
        JButton btnRefresh    = ThemeManager.createButton("Actualiser",     ThemeManager.DARK_CARD2,    ThemeManager.DARK_TEXT);

        btnEntree.addActionListener(e -> enregistrerEntree());
        btnSortie.addActionListener(e -> enregistrerSortie());
        btnAbonnement.addActionListener(e -> new AbonnementsFrame(this).setVisible(true));
        btnHistorique.addActionListener(e -> new HistoriqueFrame(this).setVisible(true));
        btnRefresh.addActionListener(e -> { chargerDonnees(); NotificationManager.show(this, "Données actualisées.", NotificationManager.Type.INFO); });

        footer.add(btnEntree);
        footer.add(btnSortie);
        footer.add(btnAbonnement);
        footer.add(btnHistorique);
        footer.add(Box.createHorizontalStrut(20));
        footer.add(btnRefresh);

        return footer;
    }

    // =========================================================================
    // DONNÉES
    // =========================================================================
    private void chargerDonnees() {
        int[] stats = dao.getEtatParking();
        lblLibres.setText(String.valueOf(stats[0]));
        lblOccupes.setText(String.valueOf(stats[1]));
        lblRevJour.setText(String.format("%.1f DH", dao.getRevenusParJour()));
        lblVehJour.setText(String.valueOf(dao.getNombreVehiculesDuJour()));

        int total = stats[0] + stats[1];
        if (total > 0) {
            int pct = (stats[1] * 100) / total;
            barOccupation.setValue(pct);
            barOccupation.setForeground(pct > 80 ? ThemeManager.ACCENT_RED :
                    pct > 50 ? ThemeManager.ACCENT_ORANGE : ThemeManager.ACCENT_GREEN);
            if (pct >= 100) NotificationManager.show(this, "[!] Parking complet !", NotificationManager.Type.WARNING);
            else if (pct >= 90) NotificationManager.show(this, "Parking presque plein (" + pct + "%)", NotificationManager.Type.WARNING);
        }

        tableModel.setRowCount(0);
        try (Connection conn = Database.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT v.immatriculation, p.numero_place, v.heure_entree FROM vehicules v JOIN places p ON v.id_place = p.id_place ORDER BY v.heure_entree DESC"
            );
            while (rs.next()) {
                String mat = rs.getString(1);
                String statut = dao.aAbonnementActif(mat) ? "[*] ABONNE" : "Standard";
                tableModel.addRow(new Object[]{
                        mat,
                        "Place P" + rs.getString(2),
                        rs.getString(3),
                        statut
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        if (mapPanel != null) mapPanel.refreshData();
        if (statsPanel != null) statsPanel.refresh();

        repaint();
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================
    private void enregistrerEntree() {
        // NOUVEAU : On crée le champ texte ET le menu déroulant !
        JTextField matField = new JTextField(14);
        JComboBox<String> comboType = new JComboBox<>(new String[]{"VOITURE", "MOTO", "CAMION"});

        // On modifie la grille pour qu'elle ait 2 lignes au lieu d'1
        JPanel form = new JPanel(new GridLayout(2, 2, 10, 10));
        form.setBackground(ThemeManager.card());

        JLabel lblMat = new JLabel("Matricule :");
        lblMat.setForeground(ThemeManager.text());
        JLabel lblType = new JLabel("Type Véhicule :");
        lblType.setForeground(ThemeManager.text());

        // Ajout des éléments au formulaire
        form.add(lblMat); form.add(matField);
        form.add(lblType); form.add(comboType);

        int r = JOptionPane.showConfirmDialog(this, form, "Enregistrer une Entrée",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String mat = matField.getText().trim().toUpperCase();
        if (mat.isEmpty()) return;

        // On récupère le type choisi
        String typeChoisi = comboType.getSelectedItem().toString();

        try (Connection conn = Database.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT id_place, numero_place FROM places WHERE est_disponible = TRUE ORDER BY id_place LIMIT 1");
            if (rs.next()) {
                int idPlace   = rs.getInt(1);
                String numPlace = rs.getString(2);

                // NOUVEAU : On envoie bien les 3 arguments (Matricule, ID Place, Type)
                if (dao.enregistrerEntree(mat, idPlace, typeChoisi)) {
                    String abonne = dao.aAbonnementActif(mat) ? " [Abonne]" : "";
                    NotificationManager.show(this, "Vehicule " + mat + abonne + " -> Place P" + numPlace, NotificationManager.Type.SUCCESS);
                    chargerDonnees();
                }
            } else {
                NotificationManager.show(this, "Parking complet ! Aucune place disponible.", NotificationManager.Type.ERROR);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            NotificationManager.show(this, "Erreur DB : " + ex.getMessage(), NotificationManager.Type.ERROR);
        }
    }

    private void enregistrerSortie() {
        JTextField matField = new JTextField(14);
        JPanel form = new JPanel(new GridLayout(1, 2, 10, 0));
        form.setBackground(ThemeManager.card());
        JLabel lbl = new JLabel("Matricule :"); lbl.setForeground(ThemeManager.text());
        form.add(lbl); form.add(matField);

        int r = JOptionPane.showConfirmDialog(this, form, "Enregistrer une Sortie",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String mat = matField.getText().trim().toUpperCase();
        if (mat.isEmpty()) return;

        double montant = dao.enregistrerSortie(mat);
        if (montant >= 0) {
            String contenu = TicketService.genererContenuTicket(mat, montant);
            TicketService.sauvegarderFichier(mat, contenu);

            JTextArea txt = new JTextArea(contenu);
            txt.setFont(new Font("Monospaced", Font.BOLD, 13));
            txt.setEditable(false);
            txt.setBackground(ThemeManager.card());
            txt.setForeground(ThemeManager.text());
            JOptionPane.showMessageDialog(this, new JScrollPane(txt), "RECU DE PAIEMENT", JOptionPane.PLAIN_MESSAGE);

            String msg = montant == 0.0 ? mat + " sorti (Abonné — Gratuit)" : mat + " → " + String.format("%.2f DH", montant);
            NotificationManager.show(this, msg, NotificationManager.Type.SUCCESS);
            chargerDonnees();
        } else {
            NotificationManager.show(this, "Véhicule " + mat + " non trouvé.", NotificationManager.Type.ERROR);
        }
    }

    private void rechercherVehicule() {
        String mat = searchField.getText().trim().toUpperCase();
        if (mat.isEmpty()) return;
        String result = dao.rechercherVehicule(mat);
        searchResult.setText(result.replace("\n", "   |   "));
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (mat.equals(tableModel.getValueAt(row, 0))) {
                table.setRowSelectionInterval(row, row);
                table.scrollRectToVisible(table.getCellRect(row, 0, true));
                tabs.setSelectedIndex(0);
                break;
            }
        }
    }

    // =========================================================================
    // THÈME
    // =========================================================================
    private void toggleTheme() {
        ThemeManager.setDarkMode(btnTheme.isSelected());
        btnTheme.setText(ThemeManager.isDarkMode() ? "Mode Dark" : "Mode Light");
        try {
            if (ThemeManager.isDarkMode()) FlatDarkLaf.setup();
            else                           FlatLightLaf.setup();
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {}
        applyBackground();
        repaint();
        chargerDonnees();
        NotificationManager.show(this,
                ThemeManager.isDarkMode() ? "Thème sombre activé" : "Thème clair activé",
                NotificationManager.Type.INFO);
    }

    private void applyBackground() {
        getContentPane().setBackground(ThemeManager.bg());
    }

    // =========================================================================
    // MAIN
    // =========================================================================
    public static void main(String[] args) {
        try { FlatDarkLaf.setup(); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ParkingGUI().setVisible(true));
    }
}