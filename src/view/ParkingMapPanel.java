package view;

import database.*;
import util.ThemeManager;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParkingMapPanel extends JPanel {
    private static final int SPOT_W = 85;  // Légèrement plus large
    private static final int SPOT_H = 65;  // Légèrement plus haut
    private static final int GAP = 15;
    private static final int COLS = 5;
    private static final int PADDING = 25;

    private List<PlaceInfo> places = new ArrayList<>();

    private static class PlaceInfo {
        int id;
        String numero;
        boolean libre;
        String matricule;
        String typePlace;
        String typeVehicule;

        PlaceInfo(int id, String num, boolean libre, String mat, String typePlace, String typeVehicule) {
            this.id = id;
            this.numero = num;
            this.libre = libre;
            this.matricule = mat;
            this.typePlace = typePlace != null ? typePlace.toUpperCase() : "STANDARD";
            this.typeVehicule = typeVehicule;
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
                        rs.getString("immatriculation"),
                        rs.getString("type_place"),
                        rs.getString("type_vehicule")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        int rows = (int) Math.ceil((double) places.size() / COLS);
        int totalH = rows * (SPOT_H + GAP) + PADDING * 2 + 60;
        setPreferredSize(new Dimension(COLS * (SPOT_W + GAP) + PADDING * 2, totalH));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fond principal avec ombre légère
        g2.setColor(ThemeManager.card2());
        g2.fillRoundRect(5, 5, getWidth()-10, getHeight()-10, 20, 20);

        // Titre élégant
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        g2.drawString("PLAN TEMPS RÉEL", PADDING, 35);

        // Légende horizontale moderne
        drawModernLegend(g2);

        // Dessin des places
        int idx = 0;
        for (PlaceInfo place : places) {
            int col = idx % COLS;
            int row = idx / COLS;
            int x = PADDING + col * (SPOT_W + GAP);
            int y = PADDING + 70 + row * (SPOT_H + GAP);

            drawStylizedSpot(g2, x, y, place);
            idx++;
        }
    }

    private void drawStylizedSpot(Graphics2D g2, int x, int y, PlaceInfo place) {
        Color mainColor;
        String icon = "";

        // Définition du style selon le type et l'état
        if (!place.libre) {
            mainColor = new Color(255, 82, 82); // Rouge vif
        } else {
            switch (place.typePlace) {
                case "HANDICAP":
                    mainColor = new Color(33, 150, 243); // Bleu
                    icon = "♿";
                    break;
                case "ELECTRIQUE":
                    mainColor = new Color(156, 39, 176); // Violet
                    icon = "⚡";
                    break;
                default:
                    mainColor = new Color(76, 175, 80); // Vert
                    break;
            }
        }

        // 1. Dessin de l'ombre de la place
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillRoundRect(x+2, y+2, SPOT_W, SPOT_H, 12, 12);

        // 2. Fond de la place (Gradient léger)
        GradientPaint gp = new GradientPaint(x, y, new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 60),
                x, y + SPOT_H, new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 20));
        g2.setPaint(gp);
        g2.fillRoundRect(x, y, SPOT_W, SPOT_H, 12, 12);

        // 3. Bordure lumineuse
        g2.setColor(mainColor);
        g2.setStroke(new BasicStroke(place.libre ? 1.5f : 3.0f));
        g2.drawRoundRect(x, y, SPOT_W, SPOT_H, 12, 12);

        // 4. Numéro de la place (Badge)
        g2.setColor(mainColor);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString(place.numero, x + 8, y + 18);

        // 5. Contenu central
        if (!place.libre) {
            // Dessin d'une petite icône voiture simplifiée ou texte
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            String mat = place.matricule != null ? place.matricule : "---";
            g2.drawString(mat, x + (SPOT_W - g2.getFontMetrics().stringWidth(mat))/2, y + 40);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(255, 255, 255, 180));
            String typeV = place.typeVehicule != null ? place.typeVehicule : "AUTO";
            g2.drawString(typeV, x + (SPOT_W - g2.getFontMetrics().stringWidth(typeV))/2, y + 55);
        } else {
            // Icône de type si libre
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 20));
            g2.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 150));
            if (!icon.isEmpty()) {
                g2.drawString(icon, x + 45, y + 25);
            }
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString("L", x + (SPOT_W-10)/2, y + 45);
        }
    }

    private void drawModernLegend(Graphics2D g2) {
        int lx = PADDING;
        int ly = 55;
        drawSmallLegend(g2, lx, ly, new Color(76, 175, 80), "Libre");
        drawSmallLegend(g2, lx + 70, ly, new Color(255, 82, 82), "Occupé");
        drawSmallLegend(g2, lx + 150, ly, new Color(33, 150, 243), "Handicap");
        drawSmallLegend(g2, lx + 240, ly, new Color(156, 39, 176), "Électrique");
    }

    private void drawSmallLegend(Graphics2D g2, int x, int y, Color c, String txt) {
        g2.setColor(c);
        g2.fillOval(x, y-8, 8, 8);
        g2.setColor(ThemeManager.textMuted());
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString(txt, x + 12, y);
    }
}