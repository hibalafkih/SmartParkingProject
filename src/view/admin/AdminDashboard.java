package view.admin;

import database.Database;
import database.ParkingDAO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdminDashboard extends JFrame {

    // ── Palette Mode Clair & Vert Forêt (Premium Neumorphism) ────────
    private static final Color BG_MAIN       = new Color(250, 252, 250);
    private static final Color BG_CARD       = new Color(255, 255, 255);
    private static final Color FOREST_GREEN  = new Color(34, 139, 34);
    private static final Color FOREST_LIGHT  = new Color(50, 168, 82);
    private static final Color TEXT_MAIN     = new Color(30, 40, 35);
    private static final Color TEXT_MUTED    = new Color(120, 135, 125);
    private static final Color BORDER        = new Color(230, 238, 233);

    private static final Color BLUE     = new Color(0, 122, 255);
    private static final Color GOLD     = new Color(245, 166, 35);
    private static final Color RED      = new Color(220, 53, 69);
    private static final Color PURPLE   = new Color(138, 43, 226);
    private static final Color TEAL     = new Color(32, 201, 151);

    private final String        login;
    private final String        role;
    private final ParkingDAO    dao = new ParkingDAO();
    private       String        activePage = "dashboard";
    private       JPanel        sidebar;
    private       JPanel        content;
    private       CardLayout    cards;

    // KPI labels
    private JLabel kpiLibres, kpiOccupes, kpiRevenu, kpiVehicules, kpiAlertes, kpiAbonnes;

    public AdminDashboard(String login, String role) {
        this.login = login; this.role = role;
        setTitle("Smart Parking — Dashboard Admin Pro");
        setSize(1240, 780);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){askLogout();}});
        setLayout(new BorderLayout());

        buildUI();
        loadKPI();

        // Un seul timer pour les données SQL, plus aucune animation UI lourde
        new Timer(30000, e -> loadKPI()).start();
    }

    // =========================================================================
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_MAIN);
                g2.fillRect(0,0,getWidth(),getHeight());

                // Halos lumineux fixes (sans lag)
                drawBlob(g2, -100, -80, 500, FOREST_GREEN, 0.03f);
                drawBlob(g2, getWidth()-300, getHeight()-250, 600, FOREST_LIGHT, 0.02f);
                g2.dispose();
            }
            private void drawBlob(Graphics2D g,int x,int y,int r,Color c,float a){
                RadialGradientPaint rg=new RadialGradientPaint(
                        new Point2D.Float(x+r/2f,y+r/2f),r/2f,new float[]{0f,1f},
                        new Color[]{new Color(c.getRed(),c.getGreen(),c.getBlue(),(int)(a*255)),new Color(c.getRed(),c.getGreen(),c.getBlue(),0)});
                g.setPaint(rg);g.fillOval(x,y,r,r);
            }
        };
        root.setOpaque(true);
        setContentPane(root);

        root.add(buildTopBar(),  BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        cards   = new CardLayout();
        content = new JPanel(cards); content.setOpaque(false);
        content.add(buildDashboard(), "dashboard");

        for(String[] item : new String[][]{
                {"abonnements","Abonnements"}, {"historique","Historique"},
                {"revenus","Revenus"}, {"incidents","Incidents"},
                {"tarifs","Tarifs Spéciaux"}, {"notifications","Notifications"}
        }){ content.add(buildPlaceholder(item[0],item[1]),item[0]); }

        root.add(content, BorderLayout.CENTER);
    }

    // ── Top Bar ───────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setColor(new Color(255,255,255,220));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(BORDER); g2.fillRect(0,getHeight()-1,getWidth(),1);
                g2.dispose();
            }
        };
        bar.setOpaque(false); bar.setPreferredSize(new Dimension(0,70));
        bar.setBorder(new EmptyBorder(0,24,0,24));

        JPanel left = new JPanel(new GridLayout(2,1)); left.setOpaque(false);
        left.add(lbl("Vue d'ensemble", 20, Font.BOLD, TEXT_MAIN));
        left.add(lbl("Espace connecté en tant que " + login, 12, Font.PLAIN, TEXT_MUTED));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 18)); right.setOpaque(false);
        JLabel clock = lbl("", 14, Font.BOLD, FOREST_GREEN);
        new Timer(1000,e->clock.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()))).start();
        clock.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()));

        JButton btnOut = styledBtn("Déconnexion", RED, 130, 34);
        btnOut.addActionListener(e->askLogout());

        right.add(clock); right.add(lbl("•", 14, Font.BOLD, BORDER));
        right.add(lbl(role, 13, Font.BOLD, TEXT_MAIN)); right.add(btnOut);

        JPanel lw=new JPanel(new FlowLayout(FlowLayout.LEFT,0,14)); lw.setOpaque(false); lw.add(left);
        bar.add(lw,BorderLayout.WEST); bar.add(right,BorderLayout.EAST);
        return bar;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setColor(BG_CARD); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(BORDER); g2.fillRect(getWidth()-1,0,1,getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(240,0));
        sidebar.setOpaque(false);
        sidebar.setBorder(new EmptyBorder(24,0,24,0));

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT,16,10)); logoRow.setOpaque(false); logoRow.setMaximumSize(new Dimension(240,70));
        JLabel logoIcon = new JLabel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0,0,FOREST_LIGHT,0,44,FOREST_GREEN));
                g2.fillRoundRect(0,0,44,44,14,14);
                g2.setFont(new Font("Segoe UI",Font.BOLD,26)); g2.setColor(Color.WHITE);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString("P",(44-fm.stringWidth("P"))/2,32); g2.dispose();
            }
        };
        logoIcon.setPreferredSize(new Dimension(44,44));
        JPanel lt=new JPanel(new GridLayout(2,1)); lt.setOpaque(false);
        lt.add(lbl("Smart Parking",16,Font.BOLD,TEXT_MAIN));
        lt.add(lbl("Administration",12,Font.PLAIN,TEXT_MUTED));
        logoRow.add(logoIcon); logoRow.add(lt);
        sidebar.add(logoRow);
        sidebar.add(Box.createVerticalStrut(20));

        String[][] items = {
                {"Dashboard", "dashboard"}, {"Abonnements", "abonnements"},
                {"Historique", "historique"}, {"Revenus", "revenus"},
                {"Incidents", "incidents"}, {"Tarifs Spéciaux", "tarifs"},
                {"Notifications", "notifications"},
        };
        for(String[] item : items) sidebar.add(sideItem(item[0],item[1]));

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel sideItem(String label, String page) {
        JPanel item = new JPanel(null) {
            boolean hover = false;
            {
                addMouseListener(new MouseAdapter(){
                    public void mouseEntered(MouseEvent e){ hover = true; repaint(); }
                    public void mouseExited(MouseEvent e){ hover = false; repaint(); }
                    public void mouseClicked(MouseEvent e){ navigate(page); }
                });
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = activePage.equals(page);

                if (active) {
                    g2.setColor(new Color(FOREST_GREEN.getRed(), FOREST_GREEN.getGreen(), FOREST_GREEN.getBlue(), 40));
                    g2.fillRoundRect(14, 6, getWidth()-28, getHeight()-8, 12, 12);
                    g2.setColor(FOREST_GREEN);
                    g2.fillRoundRect(14, 2, getWidth()-28, getHeight()-8, 12, 12);
                } else if (hover) {
                    g2.setColor(new Color(FOREST_GREEN.getRed(), FOREST_GREEN.getGreen(), FOREST_GREEN.getBlue(), 20));
                    g2.fillRoundRect(14, 2, getWidth()-28, getHeight()-8, 12, 12);
                }

                g2.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 14));
                g2.setColor(active ? Color.WHITE : (hover ? FOREST_GREEN : TEXT_MUTED));
                g2.drawString(label, 42, (getHeight()+g2.getFontMetrics().getAscent()-g2.getFontMetrics().getDescent())/2 - 2);
                g2.dispose();
            }
        };
        item.setOpaque(false); item.setMaximumSize(new Dimension(240,48)); item.setPreferredSize(new Dimension(240,48));
        return item;
    }

    // ── Dashboard page ────────────────────────────────────────────────────────
    private JPanel buildDashboard() {
        JPanel p = new JPanel(new BorderLayout(0,24)); p.setOpaque(false); p.setBorder(new EmptyBorder(24,24,24,24));

        JPanel kpiRow = new JPanel(new GridLayout(1,4,20,0)); kpiRow.setOpaque(false); kpiRow.setPreferredSize(new Dimension(0,120));
        kpiLibres    = kpiCard("Places Libres", "—", FOREST_GREEN);
        kpiOccupes   = kpiCard("Occupées", "—", BLUE);
        kpiRevenu    = kpiCard("Revenus (Auj.)", "—", GOLD);
        kpiVehicules = kpiCard("Passages", "—", TEAL);
        kpiRow.add(kpiLibres); kpiRow.add(kpiOccupes); kpiRow.add(kpiRevenu); kpiRow.add(kpiVehicules);
        p.add(kpiRow, BorderLayout.NORTH);

        JPanel actGrid = new JPanel(new GridLayout(2,3,20,20)); actGrid.setOpaque(false);
        actGrid.add(actionCard("Gestion Abonnements", PURPLE, ()-> new AbonnementsFrame(this).setVisible(true)));
        actGrid.add(actionCard("Historique Complet",  BLUE,   ()-> new HistoriqueFrame(this).setVisible(true)));
        actGrid.add(actionCard("Analyse des Revenus", GOLD,   ()-> new RevenusFrame(this).setVisible(true)));
        actGrid.add(actionCard("Suivi des Incidents", RED,    ()-> new IncidentsFrame(this).setVisible(true)));
        actGrid.add(actionCard("Configuration Tarifs",FOREST_GREEN,()-> new TarifsFrame(this).setVisible(true)));
        actGrid.add(actionCard("Carte du Parking",    TEXT_MAIN, ()-> {
            ParkingMapPanel mp=new ParkingMapPanel(); JDialog d=new JDialog(this,"Carte Parking",true);
            d.setContentPane(mp); d.setSize(750,550); d.setLocationRelativeTo(this); d.setVisible(true);
        }));
        p.add(actGrid, BorderLayout.CENTER);

        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 20, 0)); bottomRow.setOpaque(false); bottomRow.setPreferredSize(new Dimension(0, 220));
        bottomRow.add(buildBeautifulChart());
        bottomRow.add(buildRecentAlerts());
        p.add(bottomRow, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildPlaceholder(String page, String nom) {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false);
        JLabel l=lbl(nom,26,Font.BOLD,FOREST_GREEN);
        JButton btn=styledBtn("Retour au Dashboard", FOREST_GREEN, 240, 48);
        btn.addActionListener(e->navigate("dashboard"));
        GridBagConstraints g=new GridBagConstraints(); g.gridx=0; g.insets=new Insets(12,0,12,0);
        g.gridy=0; p.add(l,g); g.gridy=1; p.add(btn,g);
        return p;
    }

    // ── Zone Basse : Graphique Statique ───────────────────────────────────────
    private JPanel buildBeautifulChart() {
        JPanel chart = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                drawSoftShadow(g2, 0, 0, getWidth(), getHeight(), 12);
                g2.setColor(BG_CARD); g2.fillRoundRect(8, 8, getWidth()-16, getHeight()-16, 20, 20);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 14)); g2.setColor(TEXT_MAIN);
                g2.drawString("Évolution des Revenus (7 jrs)", 24, 34);

                int[] points = {30, 80, 50, 110, 70, 130, 100};
                int padLeft = 24, padBottom = 24, w = getWidth() - 48, h = getHeight() - 70;
                int stepX = w / (points.length - 1);

                Path2D path = new Path2D.Float();
                path.moveTo(padLeft, getHeight() - padBottom);

                for(int i=0; i<points.length; i++) {
                    int x = padLeft + (i * stepX);
                    int y = getHeight() - padBottom - (int)((points[i]/150f) * h);
                    path.lineTo(x, y);
                    g2.setColor(FOREST_GREEN); g2.fillOval(x-4, y-4, 8, 8);
                    g2.setColor(Color.WHITE); g2.fillOval(x-2, y-2, 4, 4);
                }
                path.lineTo(padLeft + w, getHeight() - padBottom);
                path.closePath();

                g2.setPaint(new GradientPaint(0, 40, new Color(FOREST_GREEN.getRed(), FOREST_GREEN.getGreen(), FOREST_GREEN.getBlue(), 60), 0, getHeight()-padBottom, new Color(255,255,255,0)));
                g2.fill(path);

                g2.setColor(FOREST_GREEN); g2.setStroke(new BasicStroke(2.5f));
                for(int i=0; i<points.length-1; i++) {
                    int x1 = padLeft + (i * stepX), y1 = getHeight() - padBottom - (int)((points[i]/150f) * h);
                    int x2 = padLeft + ((i+1) * stepX), y2 = getHeight() - padBottom - (int)((points[i+1]/150f) * h);
                    g2.drawLine(x1, y1, x2, y2);
                }
                g2.dispose();
            }
        };
        chart.setOpaque(false);
        return chart;
    }

    private JPanel buildRecentAlerts() {
        JPanel alerts = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                drawSoftShadow(g2, 0, 0, getWidth(), getHeight(), 12);
                g2.setColor(BG_CARD); g2.fillRoundRect(8, 8, getWidth()-16, getHeight()-16, 20, 20);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 14)); g2.setColor(TEXT_MAIN);
                g2.drawString("Alertes Récentes", 24, 34);

                String[] msgs = {"Barrière NORD bloquée", "Paiement échoué (Borne 2)", "Capteur A14 déconnecté"};
                Color[] cols = {RED, GOLD, BLUE};

                for(int i=0; i<msgs.length; i++) {
                    int y = 50 + (i * 45);
                    g2.setColor(new Color(cols[i].getRed(), cols[i].getGreen(), cols[i].getBlue(), 20));
                    g2.fillRoundRect(24, y, 36, 36, 10, 10);
                    g2.setColor(cols[i]);
                    g2.fillOval(38, y+14, 8, 8);

                    g2.setFont(new Font("Segoe UI", Font.BOLD, 13)); g2.setColor(TEXT_MAIN);
                    g2.drawString(msgs[i], 70, y+18);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 11)); g2.setColor(TEXT_MUTED);
                    g2.drawString("Aujourd'hui", 70, y+32);
                }
                g2.dispose();
            }
        };
        alerts.setOpaque(false);
        return alerts;
    }

    // ── Cartes génériques ─────────────────────────────────────────────────────
    private JLabel kpiCard(String titre, String val, Color accent) {
        return new JLabel(val) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                drawSoftShadow(g2, 0, 0, getWidth(), getHeight(), 8);

                g2.setColor(BG_CARD); g2.fillRoundRect(8, 8, getWidth()-16, getHeight()-16, 20, 20);

                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 15));
                g2.fillRoundRect(20, 20, 44, 44, 12, 12);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(32, 32, 20, 20, 6, 6);

                g2.setFont(new Font("Segoe UI",Font.BOLD,13)); g2.setColor(TEXT_MUTED);
                g2.drawString(titre, 20, 90);

                g2.setFont(new Font("Segoe UI",Font.BOLD,28)); g2.setColor(TEXT_MAIN);
                g2.drawString(getText(), 20, 125);
                g2.dispose();
            }
        };
    }

    private JPanel actionCard(String nom, Color accent, Runnable action) {
        return new JPanel(null) {
            boolean hover = false;
            {
                setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter(){
                    public void mouseEntered(MouseEvent e){ hover = true; repaint(); }
                    public void mouseExited(MouseEvent e){ hover = false; repaint(); }
                    public void mouseClicked(MouseEvent e){ action.run(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

                int shadowPad = hover ? 6 : 8;
                drawSoftShadow(g2, shadowPad, shadowPad, getWidth()-shadowPad*2, getHeight()-shadowPad*2, hover ? 14 : 10);

                g2.setColor(BG_CARD);
                g2.fillRoundRect(8, 8, getWidth()-16, getHeight()-16, 16, 16);

                g2.setColor(hover ? accent : BORDER);
                g2.setStroke(new BasicStroke(hover ? 2f : 1f));
                g2.drawRoundRect(8, 8, getWidth()-17, getHeight()-17, 16, 16);

                g2.setColor(accent); g2.fillOval(20, getHeight()/2 - 6, 12, 12);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                g2.setColor(hover ? accent : TEXT_MAIN);
                g2.drawString(nom, 44, getHeight()/2 + 5);
                g2.dispose();
            }
        };
    }

    private void drawSoftShadow(Graphics2D g2, int x, int y, int w, int h, int spread) {
        for (int i=0; i<spread; i++) {
            g2.setColor(new Color(0,0,0, 3 - (i*3/spread)));
            g2.fillRoundRect(x+spread-i, y+spread-i+2, w-(spread*2)+(i*2), h-(spread*2)+(i*2), 24, 24);
        }
    }

    // ── Logique Métier (Inchangée) ────────────────────────────────────────────
    private void loadKPI() {
        SwingWorker<Void,Void> w = new SwingWorker<>(){
            @Override protected Void doInBackground(){
                try(Connection c=Database.getConnection()){
                    int[] e=dao.getEtatParking(); kpiLibres.setText(String.valueOf(e[0])); kpiOccupes.setText(String.valueOf(e[1]));
                    ResultSet r1=c.createStatement().executeQuery("SELECT COALESCE(SUM(montant_paye),0) FROM historique_paiements WHERE DATE(date_sortie)=CURDATE()");
                    if(r1.next()) kpiRevenu.setText(String.format("%.0f DH",r1.getDouble(1)));
                    ResultSet r2=c.createStatement().executeQuery("SELECT COUNT(*) FROM historique_paiements WHERE DATE(date_sortie)=CURDATE()");
                    if(r2.next()) kpiVehicules.setText(String.valueOf(r2.getInt(1)));
                }catch(Exception ex){ex.printStackTrace();} return null;
            }
            @Override protected void done(){ repaint(); }
        }; w.execute();
    }

    private void navigate(String page) { activePage=page; cards.show(content,page); sidebar.repaint(); openPage(page); }

    private void openPage(String page) {
        switch(page){
            case "abonnements":  new AbonnementsFrame(this).setVisible(true);  break;
            case "historique":   new HistoriqueFrame(this).setVisible(true);   break;
            case "revenus":      new RevenusFrame(this).setVisible(true);      break;
            case "incidents":    new IncidentsFrame(this).setVisible(true);    break;
            case "tarifs":       new TarifsFrame(this).setVisible(true);       break;
            case "notifications":new NotificationsFrame(this).setVisible(true);break;
        }
        activePage="dashboard"; cards.show(content,"dashboard"); sidebar.repaint();
    }

    private void askLogout(){ if(JOptionPane.showConfirmDialog(this,"Se déconnecter ?","Déconnexion",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){ dispose(); new LoginFrame().setVisible(true); } }
    private JLabel lbl(String t,int s,int st,Color c){ JLabel l=new JLabel(t); l.setFont(new Font("Segoe UI",st,s)); l.setForeground(c); return l; }

    private JButton styledBtn(String txt, Color accent, int w, int h){
        JButton btn=new JButton(txt){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                g2.setColor(hover ? new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),20) : BG_CARD);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(hover ? accent : BORDER);
                g2.setStroke(new BasicStroke(hover ? 1.5f : 1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.setFont(new Font("Segoe UI",Font.BOLD,13)); g2.setColor(hover ? accent : TEXT_MAIN);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(w,h)); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return btn;
    }
}