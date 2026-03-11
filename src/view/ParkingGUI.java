package view;

import com.formdev.flatlaf.FlatLightLaf; // Nécessite l'ajout du JAR FlatLaf
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;
import database.*;

public class ParkingGUI extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JLabel lblLibres;
    private JLabel lblOccupes;

    // Couleurs du thème moderne
    private final Color COLOR_PRIMARY = new Color(41, 128, 185); // Bleu pro
    private final Color COLOR_SUCCESS = new Color(39, 174, 96);  // Vert doux
    private final Color COLOR_DANGER = new Color(192, 57, 43);   // Rouge doux
    private final Color COLOR_BACKGROUND = new Color(245, 245, 245); // Gris très clair

    public ParkingGUI() {
        // 1. Configuration de base
        setTitle("Smart Parking System v1.0");
        setSize(900, 600); // Taille plus grande pour un design aéré
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10)); // Espacement entre les zones

        // 2. Header : Statistiques Visuelles
        JPanel panelStats = new JPanel(new FlowLayout(FlowLayout.CENTER, 60, 20));
        panelStats.setBackground(Color.WHITE); // Fond blanc pour détacher le header
        // Ajout d'une ombre portée légère (bordure inférieure)
        panelStats.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        lblLibres = new JLabel("🟩 Places Libres : -");
        lblOccupes = new JLabel("🟥 Places Occupées : -");

        Font fontStats = new Font("Segoe UI", Font.BOLD, 18);
        lblLibres.setForeground(COLOR_SUCCESS);
        lblOccupes.setForeground(COLOR_DANGER);
        lblLibres.setFont(fontStats);
        lblOccupes.setFont(fontStats);
        lblLibres.setFont(fontStats);
        lblOccupes.setFont(fontStats);

        panelStats.add(lblLibres);
        panelStats.add(lblOccupes);
        add(panelStats, BorderLayout.NORTH);


        // 3. Centre : Tableau Modernisé
        JPanel panelTable = new JPanel(new BorderLayout());
        panelTable.setBorder(new EmptyBorder(10, 20, 10, 20)); // Marges extérieures (Padding)
        panelTable.setBackground(COLOR_BACKGROUND);

        model = new DefaultTableModel(new String[]{"🚗 Matricule", "📍 Numero Place", "🕒 Heure Entrée"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tableau non éditable pour la propreté
            }
        };
        table = new JTable(model);

        // Design du tableau
        table.setRowHeight(35); // Lignes plus hautes
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.setShowGrid(false); // Cache la grille standard
        table.setIntercellSpacing(new Dimension(0, 0)); // Supprime l'espace inter-cellules

        // Design de l'en-tête du tableau
        JTableHeader header = table.getTableHeader();
        header.setBackground(Color.WHITE);
        header.setReorderingAllowed(false); // Empêche de réorganiser les colonnes

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230))); // Bordure douce
        panelTable.add(scrollPane, BorderLayout.CENTER);
        add(panelTable, BorderLayout.CENTER);

        // 4. Sud : Panneau d'Actions Modernisé
        JPanel panelActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        panelActions.setBackground(COLOR_BACKGROUND);
        panelActions.setBorder(new EmptyBorder(10, 0, 20, 0));

        // Création de boutons stylisés
        JButton btnEntree = creerBoutonModerne("📥 Entrée Véhicule", COLOR_SUCCESS);
        JButton btnSortie = creerBoutonModerne("📤 Sortie & Paiement", COLOR_DANGER);
        JButton btnRecherche = creerBoutonModerne("🔍 Rechercher", new Color(52, 73, 94));
        JButton btnHistorique = creerBoutonModerne("📋 Voir Historique", COLOR_PRIMARY);

        // Assignation des actions
        btnEntree.addActionListener(e -> enregistrerEntree());
        btnSortie.addActionListener(e -> enregistrerSortie());
        btnHistorique.addActionListener(e -> {
            HistoriqueFrame hf = new HistoriqueFrame(this);
            hf.setVisible(true);
        });
        btnRecherche.addActionListener(e -> {
            String mat = JOptionPane.showInputDialog(this, "Entrez le matricule à rechercher :");
            if (mat != null && !mat.trim().isEmpty()) {
                ParkingDAO dao = new ParkingDAO();
                String resultat = dao.rechercherVehicule(mat);
                // Utilisation d'une icône de recherche dans la popup
                JOptionPane.showMessageDialog(this, resultat, "Résultat de recherche", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        panelActions.add(btnEntree);
        panelActions.add(btnSortie);
        panelActions.add(btnRecherche);
        panelActions.add(btnHistorique);
        add(panelActions, BorderLayout.SOUTH);

        // 5. Charger les données initiales
        chargerDonnees();
    }

    // Méthode utilitaire pour créer des boutons uniformes et stylisés
    private JButton creerBoutonModerne(String texte, Color couleurFond) {
        JButton btn = new JButton(texte);
        // On utilise Segoe UI Emoji pour supporter les icônes texte
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(couleurFond);
        btn.setPreferredSize(new Dimension(200, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        return btn;
    }

    // --- Les autres méthodes restent inchangées ---
    private void enregistrerEntree() {
        String mat = JOptionPane.showInputDialog(this, "Entrez le matricule du nouveau véhicule :");
        if (mat == null || mat.trim().isEmpty()) return;

        try (Connection conn = Database.getConnection()) {
            String sqlPlace = "SELECT id_place FROM places WHERE est_disponible = TRUE LIMIT 1";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sqlPlace);

            if (rs.next()) {
                int idPlace = rs.getInt("id_place");
                PreparedStatement ps = conn.prepareStatement("INSERT INTO vehicules (immatriculation, type_vehicule, id_place) VALUES (?, 'Voiture', ?)");
                ps.setString(1, mat.trim().toUpperCase()); // Normalisation matricule
                ps.setInt(2, idPlace);
                ps.executeUpdate();

                conn.createStatement().executeUpdate("UPDATE places SET est_disponible = FALSE WHERE id_place = " + idPlace);

                JOptionPane.showMessageDialog(this, "✅ Véhicule enregistré.\nPlace attribuée : P" + idPlace, "Succès", JOptionPane.INFORMATION_MESSAGE);
                chargerDonnees();
            } else {
                JOptionPane.showMessageDialog(this, "⚠️ Parking Plein !", "Complet", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "❌ Erreur base de données : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enregistrerSortie() {
        String mat = JOptionPane.showInputDialog(this, "Entrez le matricule du véhicule sortant :");
        if (mat == null || mat.trim().isEmpty()) return;

        ParkingDAO dao = new ParkingDAO();
        String matriculeUnique = mat.trim().toUpperCase();
        double montant = dao.enregistrerSortie(matriculeUnique);

        if (montant != -1) {
            // 1. Générer le texte du ticket
            String contenuTicket = TicketService.genererContenuTicket(matriculeUnique, montant);

            // 2. Sauvegarder le fichier .txt (Sortie physique)
            TicketService.sauvegarderFichier(matriculeUnique, contenuTicket);

            // 3. Afficher à l'écran dans une zone de texte (Sortie numérique)
            JTextArea textArea = new JTextArea(contenuTicket);
            textArea.setFont(new Font("Monospaced", Font.BOLD, 13)); // Police style ticket
            textArea.setEditable(false);
            textArea.setBackground(new Color(253, 253, 253));

            JOptionPane.showMessageDialog(this,
                    new JScrollPane(textArea),
                    "REÇU DE PAIEMENT - IMPRESSION",
                    JOptionPane.INFORMATION_MESSAGE);

            chargerDonnees(); // Rafraîchir l'interface
        } else {
            JOptionPane.showMessageDialog(this, "Erreur : Véhicule introuvable.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chargerDonnees() {
        // Mise à jour des stats visuelles
        ParkingDAO dao = new ParkingDAO();
        int[] stats = dao.getEtatParking();
        if (lblLibres != null) lblLibres.setText("🟩 Places Libres : " + stats[0]);
        if (lblOccupes != null) lblOccupes.setText("🟥 Places Occupées : " + stats[1]);

        // Mise à jour du tableau
        if (model != null) {
            model.setRowCount(0);
            try (Connection conn = Database.getConnection()) {
                ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT v.immatriculation, p.numero_place, v.heure_entree " +
                                "FROM vehicules v JOIN places p ON v.id_place = p.id_place"
                );
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getString(1),
                            "P" + rs.getString(2), // Ajout du préfixe 'P' pour la clarté
                            rs.getString(3)
                    });
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public static void main(String[] args) {
        try {
            // --- ACTIVATION DU DESIGN MODERNE ---
            FlatLightLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new ParkingGUI().setVisible(true));
    }
}