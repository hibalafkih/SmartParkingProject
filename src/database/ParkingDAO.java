package database;
import model.TypeVehicule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe gérant les accès aux données (CRUD) pour le Parking.
 * Version améliorée avec : abonnements, statistiques avancées, recherche.
 */
public class ParkingDAO {

    // =========================================================================
    // FONCTIONNALITÉ : ENTRÉE
    // =========================================================================
    // =========================================================================
    // FONCTIONNALITÉ : ENTRÉE
    // =========================================================================
    public boolean enregistrerEntree(String matricule, int idPlace, String typeVehicule) {
        // NOUVEAU : Ajout de type_vehicule dans la requête SQL
        String queryVehicule = "INSERT INTO vehicules (immatriculation, id_place, type_vehicule, heure_entree) VALUES (?, ?, ?, NOW())";
        String queryPlace    = "UPDATE places SET est_disponible = FALSE WHERE id_place = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psV = conn.prepareStatement(queryVehicule);
                 PreparedStatement psP = conn.prepareStatement(queryPlace)) {

                psV.setString(1, matricule);
                psV.setInt(2, idPlace);
                psV.setString(3, typeVehicule != null ? typeVehicule : "VOITURE"); // NOUVEAU
                psV.executeUpdate();

                psP.setInt(1, idPlace);
                psP.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // =========================================================================
    // FONCTIONNALITÉ : SORTIE & CALCUL PRIX
    // =========================================================================
    public double enregistrerSortie(String matricule) {
        // Vérifier si le véhicule a un abonnement actif
        if (aAbonnementActif(matricule)) {
            String deleteVehicule = "DELETE FROM vehicules WHERE immatriculation = ?";
            String selectPlace = "SELECT id_place FROM vehicules WHERE immatriculation = ?";
            try (Connection conn = Database.getConnection()) {
                PreparedStatement psS = conn.prepareStatement(selectPlace);
                psS.setString(1, matricule);
                ResultSet rs = psS.executeQuery();
                if (rs.next()) {
                    int idPlace = rs.getInt("id_place");
                    conn.createStatement().executeUpdate(
                            "UPDATE places SET est_disponible = TRUE WHERE id_place = " + idPlace);
                }
                PreparedStatement psH = conn.prepareStatement(
                        "INSERT INTO historique_paiements (immatriculation, duree_minutes, montant_paye) VALUES (?, 0, 0.0)");
                psH.setString(1, matricule + " [ABONNÉ]");
                psH.executeUpdate();
                PreparedStatement psD = conn.prepareStatement(deleteVehicule);
                psD.setString(1, matricule);
                psD.executeUpdate();
                return 0.0; // Abonné = gratuit
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // NOUVEAU : Ajout de type_vehicule dans le SELECT
        String selectQuery   = "SELECT id_place, type_vehicule, heure_entree, TIMESTAMPDIFF(MINUTE, heure_entree, NOW()) as duree FROM vehicules WHERE immatriculation = ?";
        String deleteVehicule = "DELETE FROM vehicules WHERE immatriculation = ?";
        String insertHistorique = "INSERT INTO historique_paiements (immatriculation, duree_minutes, montant_paye) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection()) {
            PreparedStatement psSelect = conn.prepareStatement(selectQuery);
            psSelect.setString(1, matricule);
            ResultSet rs = psSelect.executeQuery();

            if (rs.next()) {
                int  idPlace = rs.getInt("id_place");
                long duree   = rs.getLong("duree");
                String typeVehicule = rs.getString("type_vehicule"); // NOUVEAU

                if (typeVehicule == null) typeVehicule = "VOITURE"; // Sécurité
                if (duree == 0) duree = 1;

                // NOUVEAU : Utilisation de votre méthode de calcul dynamique
                double montant = calculerMontant(duree, typeVehicule);

                conn.createStatement().executeUpdate(
                        "UPDATE places SET est_disponible = TRUE WHERE id_place = " + idPlace);

                PreparedStatement psH = conn.prepareStatement(insertHistorique);
                psH.setString(1, matricule);
                psH.setLong(2, duree);
                psH.setDouble(3, montant);
                psH.executeUpdate();

                PreparedStatement psD = conn.prepareStatement(deleteVehicule);
                psD.setString(1, matricule);
                psD.executeUpdate();

                return montant;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1.0; // <-- AJOUTEZ CECI
    }
    // =========================================================================
    // HISTORIQUE
    // =========================================================================
    public ResultSet getHistorique() {
        try {
            Connection conn = Database.getConnection();
            String sql = "SELECT * FROM historique_paiements ORDER BY date_sortie DESC";
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================================
    // ÉTAT DU PARKING
    // =========================================================================
    public int[] getEtatParking() {
        int[] stats = new int[2];
        try (Connection conn = Database.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT est_disponible, COUNT(*) as nb FROM places GROUP BY est_disponible");
            while (rs.next()) {
                if (rs.getBoolean("est_disponible")) stats[0] = rs.getInt("nb");
                else                                  stats[1] = rs.getInt("nb");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }

    // =========================================================================
    // TOUTES LES PLACES (pour la carte visuelle)
    // =========================================================================
    public ResultSet getToutesLesPlaces() {
        try {
            Connection conn = Database.getConnection();
            // Correction : Suppression de la condition sur date_sortie qui n'existe pas
            // Correction : Jointure propre entre places et vehicules sur id_place
            String sql = "SELECT p.id_place, p.numero_place, p.est_disponible, p.type_place, " +
                    "v.immatriculation, v.type_vehicule " +
                    "FROM places p " +
                    "LEFT JOIN vehicules v ON p.id_place = v.id_place " +
                    "ORDER BY CAST(p.numero_place AS UNSIGNED)";
            return conn.createStatement().executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================================
    // RECHERCHE VÉHICULE
    // =========================================================================
    public String rechercherVehicule(String matricule) {
        String sql = "SELECT p.numero_place, v.heure_entree FROM vehicules v " +
                "JOIN places p ON v.id_place = p.id_place WHERE v.immatriculation = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricule);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String abonne = aAbonnementActif(matricule) ? " [ABONNÉ ACTIF]" : "";
                return "✅ Véhicule trouvé" + abonne + "\n📍 Place : " + rs.getString("numero_place") +
                        "\n🕒 Entrée : " + rs.getTimestamp("heure_entree");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "❌ Véhicule non trouvé dans le parking.";
    }

    // =========================================================================
    // STATISTIQUES AVANCÉES
    // =========================================================================
    public double getChiffreAffairesTotal() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(montant_paye),0) FROM historique_paiements")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public int getNombreVehiculesDuJour() {
        String sql = "SELECT COUNT(*) FROM historique_paiements WHERE DATE(date_sortie) = CURDATE()";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public double getRevenusParJour() {
        String sql = "SELECT COALESCE(SUM(montant_paye),0) FROM historique_paiements WHERE DATE(date_sortie) = CURDATE()";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    /** Revenus des 7 derniers jours pour le graphique */
    public List<double[]> getRevenus7Jours() {
        List<double[]> data = new ArrayList<>();
        String sql = "SELECT DATE(date_sortie) as jour, COALESCE(SUM(montant_paye),0) as total " +
                "FROM historique_paiements WHERE date_sortie >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
                "GROUP BY jour ORDER BY jour ASC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                data.add(new double[]{ rs.getDate("jour").getTime(), rs.getDouble("total") });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }

    /** Nombre de passages par heure (distribution horaire) */
    public int[] getDistributionHoraire() {
        int[] heures = new int[24];
        String sql = "SELECT HOUR(date_sortie) as h, COUNT(*) as nb " +
                "FROM historique_paiements GROUP BY h";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                heures[rs.getInt("h")] = rs.getInt("nb");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return heures;
    }

    // =========================================================================
    // ABONNEMENTS
    // =========================================================================
    // Surcharge : enregistre aussi le paiement
    public boolean ajouterAbonnement(String matricule, String nomClient, int dureeJours,
                                     double montant, String modePaiement,
                                     String statutPaiement, String numTransaction) {
        // Essayer avec colonnes paiement (apres migration SQL)
        String sql = "INSERT INTO abonnements (matricule, nom_client, date_debut, date_fin, montant, mode_paiement, statut_paiement, num_transaction) " +
                "VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY), ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricule.toUpperCase());
            ps.setString(2, nomClient);
            ps.setInt(3, dureeJours);
            ps.setDouble(4, montant);
            ps.setString(5, modePaiement);
            ps.setString(6, statutPaiement);
            ps.setString(7, numTransaction);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Fallback : colonnes paiement pas encore ajoutees -> utiliser methode simple
            return ajouterAbonnement(matricule, nomClient, dureeJours);
        }
    }

    public boolean ajouterAbonnement(String matricule, String nomClient, int dureeJours) {
        String sql = "INSERT INTO abonnements (matricule, nom_client, date_debut, date_fin) " +
                "VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY))";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricule.toUpperCase());
            ps.setString(2, nomClient);
            ps.setInt(3, dureeJours);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean aAbonnementActif(String matricule) {
        String sql = "SELECT COUNT(*) FROM abonnements WHERE matricule = ? AND date_fin >= NOW()";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricule.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public ResultSet getTousAbonnements() {
        try {
            Connection conn = Database.getConnection();
            String sql = "SELECT *, DATEDIFF(date_fin, NOW()) as jours_restants FROM abonnements ORDER BY date_fin DESC";
            return conn.createStatement().executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean supprimerAbonnement(int idAbonnement) {
        String sql = "DELETE FROM abonnements WHERE id_abonnement = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAbonnement);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
    // Exemple de calcul lors de l'enregistrement de la sortie
    public double calculerMontant(long dureeMinutes, String typeVehiculeStr) {
        if (dureeMinutes <= 15) {
            return 0.0; // Période de grâce de 15 minutes (Gratuit)
        }

        TypeVehicule type = TypeVehicule.valueOf(typeVehiculeStr.toUpperCase());
        return (dureeMinutes / 60.0) * type.getTarifHoraire();
    }
}