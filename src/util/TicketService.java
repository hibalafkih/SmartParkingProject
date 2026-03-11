package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TicketService {

    /**
     * Génère le texte formaté du ticket de caisse.
     */
    public static String genererContenuTicket(String matricule, double montant) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("========== TICKET DE PARKING ==========\n");
        sb.append("         SMART PARKING SYSTEM          \n");
        sb.append("=======================================\n");
        sb.append("Matricule : ").append(matricule).append("\n");
        sb.append("Date      : ").append(LocalDateTime.now().format(dtf)).append("\n");
        sb.append("---------------------------------------\n");
        sb.append("TOTAL A PAYER : ").append(String.format("%.2f DH", montant)).append("\n");
        sb.append("=======================================\n");
        sb.append("      Merci et bonne route !           \n");
        return sb.toString();
    }

    /**
     * Sauvegarde le ticket dans un fichier texte localement.
     */
    public static void sauvegarderFichier(String matricule, String contenu) {
        String filename = "ticket_" + matricule + "_" + System.currentTimeMillis() + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(contenu);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}