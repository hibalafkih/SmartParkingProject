package database;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.*;
import java.awt.print.*;
import javax.swing.*;

public class TicketService {

    public static String genererContenuTicket(String matricule, double montant) {
        String date = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now());
        String statut = montant == 0.0 ? "ABONNÉ — GRATUIT" : String.format("%.2f DH", montant);
        return "==========================================\n" +
                "        🅿  SMART PARKING SYSTEM         \n" +
                "==========================================\n" +
                "Date/Heure : " + date + "\n" +
                "Véhicule   : " + matricule + "\n" +
                "Tarif      : 5.0 DH / heure\n" +
                "------------------------------------------\n" +
                "TOTAL PAYÉ : " + statut + "\n" +
                "------------------------------------------\n" +
                "      Merci de votre confiance !         \n" +
                "==========================================\n";
    }

    public static void sauvegarderFichier(String matricule, String contenu) {
        try (FileWriter writer = new FileWriter("Ticket_" + matricule + ".txt")) {
            writer.write(contenu);
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Exporte l'historique au format CSV (compatible Excel)
     */
    public static boolean exporterCSV(java.sql.ResultSet rs, String cheminFichier) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(cheminFichier))) {
            pw.println("ID;Matricule;Durée (min);Montant (DH);Date Sortie");
            while (rs != null && rs.next()) {
                pw.printf("%d;%s;%d;%.2f;%s%n",
                        rs.getInt("id_paiement"),
                        rs.getString("immatriculation"),
                        rs.getInt("duree_minutes"),
                        rs.getDouble("montant_paye"),
                        rs.getTimestamp("date_sortie")
                );
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Imprime / exporte en PDF via le système d'impression Java
     */
    public static void exporterPDF(javax.swing.JTable table, Frame parent) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Historique Parking");

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            // Titre
            g2.setFont(new Font("Monospaced", Font.BOLD, 16));
            g2.drawString("SMART PARKING — Historique des Paiements", 20, 30);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.drawString("Exporté le : " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .format(LocalDateTime.now()), 20, 50);

            // En-têtes
            String[] cols = {"ID", "Matricule", "Durée (min)", "Montant (DH)", "Date Sortie"};
            int[] widths = {30, 100, 80, 80, 130};
            int x = 20, y = 75;
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            for (int i = 0; i < cols.length; i++) {
                g2.drawString(cols[i], x, y);
                x += widths[i];
            }

            // Séparateur
            g2.drawLine(20, y + 5, 450, y + 5);
            y += 20;
            g2.setFont(new Font("Monospaced", Font.PLAIN, 9));

            // Données
            for (int row = 0; row < table.getRowCount(); row++) {
                x = 20;
                for (int col = 0; col < table.getColumnCount(); col++) {
                    Object val = table.getValueAt(row, col);
                    g2.drawString(val != null ? val.toString() : "", x, y);
                    x += widths[col];
                }
                y += 15;
                if (y > pageFormat.getImageableHeight() - 30) break;
            }

            return Printable.PAGE_EXISTS;
        });

        boolean ok = job.printDialog();
        if (ok) {
            try { job.print(); }
            catch (PrinterException e) {
                JOptionPane.showMessageDialog(parent, "Erreur impression : " + e.getMessage());
            }
        }
    }
}