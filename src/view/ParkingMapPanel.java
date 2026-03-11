package view;

import database.*;
import util.ThemeManager;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.sql.*;

/**
 * Panneau de visualisation graphique des places de parking.
 * Affiche chaque place sous forme de case colorée (libre/occupée).
 */
public class ParkingMapPanel extends JPanel {

    private static final int SPOT_W   = 70;
    private static final int SPOT_H   = 50;
    private static final int GAP      = 12;
    private static final int COLS     = 5;
    private static final int PADDING  = 20;

    private java.util.List<PlaceInfo> places = new java.util.ArrayList<>();

    private static class PlaceInfo {
        int id;
        String numero;
        boolean libre;
        String matricule;
        PlaceInfo(int id, String num, boolean libre, String mat) {
            this.id = id; this.numero = num; this.libre = libre; this.matricule = mat;
        }
    }

    public ParkingMapPanel() {
        setOpaque(false);
        refreshData();
    }

    public void refreshData() {
        places.clear();
        ParkingDAO dao = new ParkingDAO();
        ResultSet rs = dao.getToutesLesPlaces();
        try {
            while (rs != null && rs.next()) {
                places.add(new PlaceInfo(
                        rs.getInt("id_place"),
                        rs.getString("numero_place"),
                        rs.getBoolean("est_disponible"),
                        rs.getString("immatriculation")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Taille préférée dynamique
        int rows = (int) Math.ceil((double) places.size() / COLS);
        int w = COLS * (SPOT_W + GAP) + PADDING * 2;
        int h = rows * (SPOT_H + GAP) + PADDING * 2 + 40;
        setPreferredSize(new Dimension(w, h));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fond
        g2.setColor(ThemeManager.card2());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

        // Titre section
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(ThemeManager.textMuted());
        g2.drawString("PLAN DU PARKING", PADDING, PADDING + 14);

        // Légende
        int lx = getWidth() - 200;
        drawLegend(g2, lx, PADDING + 5, ThemeManager.ACCENT_GREEN,   "Libre");
        drawLegend(g2, lx + 80, PADDING + 5, ThemeManager.ACCENT_RED, "Occupée");

        // Places
        int idx = 0;
        for (PlaceInfo place : places) {
            int col = idx % COLS;
            int row = idx / COLS;
            int x = PADDING + col * (SPOT_W + GAP);
            int y = PADDING + 35 + row * (SPOT_H + GAP);

            drawSpot(g2, x, y, place);
            idx++;
        }

        if (places.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            g2.setColor(ThemeManager.textMuted());
            g2.drawString("Aucune place configurée", PADDING, PADDING + 60);
        }
    }

    private void drawSpot(Graphics2D g2, int x, int y, PlaceInfo place) {
        Color bgColor  = place.libre
                ? new Color(ThemeManager.ACCENT_GREEN.getRed(), ThemeManager.ACCENT_GREEN.getGreen(), ThemeManager.ACCENT_GREEN.getBlue(), 30)
                : new Color(ThemeManager.ACCENT_RED.getRed(),   ThemeManager.ACCENT_RED.getGreen(),   ThemeManager.ACCENT_RED.getBlue(),   30);
        Color borderC  = place.libre ? ThemeManager.ACCENT_GREEN : ThemeManager.ACCENT_RED;
        Color textC    = place.libre ? ThemeManager.ACCENT_GREEN : ThemeManager.ACCENT_RED;

        // Fond de la case
        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, SPOT_W, SPOT_H, 8, 8);

        // Bordure
        g2.setColor(borderC);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, SPOT_W, SPOT_H, 8, 8);

        // Numéro de place
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(textC);
        FontMetrics fm = g2.getFontMetrics();
        String numText = "P" + place.numero;
        g2.drawString(numText, x + (SPOT_W - fm.stringWidth(numText)) / 2, y + 20);

        // Icône ou matricule tronqué
        if (place.libre) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            g2.drawString("P", x + (SPOT_W - 12) / 2, y + 38);
        } else {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
            g2.setColor(ThemeManager.textMuted());
            String mat = place.matricule != null ? place.matricule : "";
            if (mat.length() > 8) mat = mat.substring(0, 8);
            g2.drawString(mat, x + (SPOT_W - g2.getFontMetrics().stringWidth(mat)) / 2, y + 40);
        }
    }

    private void drawLegend(Graphics2D g2, int x, int y, Color color, String label) {
        g2.setColor(color);
        g2.fillRoundRect(x, y, 14, 14, 4, 4);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(ThemeManager.textMuted());
        g2.drawString(label, x + 18, y + 12);
    }
}