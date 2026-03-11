package view;

import database.ParkingDAO;
import util.ThemeManager;
import util.NotificationManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Abonnements — Module complet avec paiement Especes / Carte bancaire.
 * Theme : Teal / Or.
 */
public class AbonnementsFrame extends JDialog {

    // Palette teal/or
    private static final Color T1   = new Color(0, 150, 136);
    private static final Color T2   = new Color(0, 188, 170);
    private static final Color GOLD = new Color(255, 193,   7);
    private static final Color T_BG = new Color( 14,  30,  28);

    // Tarifs
    private static final double[] PRIX  = {150.0, 280.0, 400.0, 1500.0};
    private static final int[]    JOURS = {30, 60, 90, 365};
    private static final String[] LBLS  = {"1 MOIS","2 MOIS","3 MOIS","1 AN"};
    private static final Color[]  COLS  = {
            new Color(0,200,150), new Color(30,160,230),
            new Color(255,180,0), new Color(180,100,255)
    };

    private JTable table;
    private DefaultTableModel model;
    private ParkingDAO dao = new ParkingDAO();
    private Frame parent;
    private CartePanel cartePanel;

    public AbonnementsFrame(Frame parent) {
        super(parent, "Abonnements — Smart Parking", true);
        this.parent = parent;
        setSize(1020, 690);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ThemeManager.bg());
        buildUI();
        charger();
    }

    // =========================================================================
    // UI PRINCIPALE
    // =========================================================================
    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeft(), buildRight());
        split.setDividerLocation(590); split.setDividerSize(3);
        split.setBorder(null); split.setBackground(ThemeManager.bg());
        add(split, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0,0,T_BG,getWidth(),0,new Color(0,60,55)));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.setPaint(new GradientPaint(0,getHeight()-2,T1,getWidth(),getHeight()-2,T2));
                g2.fillRect(0,getHeight()-2,getWidth(),2);
            }
        };
        h.setPreferredSize(new Dimension(0,70));
        h.setBorder(new EmptyBorder(0,24,0,24));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,14,14)); left.setOpaque(false);
        JLabel ico = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(T2); g2.fillRoundRect(2,6,38,26,7,7);
                g2.setColor(T1.darker()); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(2,6,38,26,7,7);
                g2.setColor(new Color(0,0,0,120)); g2.fillRect(2,12,38,7);
                g2.setColor(GOLD); g2.fillRoundRect(8,20,12,7,3,3);
                g2.setColor(new Color(255,255,255,40)); g2.fillRoundRect(2,6,38,8,7,7);
                g2.dispose();
            }
        };
        ico.setPreferredSize(new Dimension(44,40));
        JPanel titles = new JPanel(new GridLayout(2,1)); titles.setOpaque(false);
        JLabel t1 = new JLabel("Gestion des Abonnements");
        t1.setFont(new Font("Segoe UI",Font.BOLD,17)); t1.setForeground(Color.WHITE);
        JLabel t2 = new JLabel("Cartes membres  |  Paiement  |  Recus");
        t2.setFont(new Font("Segoe UI",Font.PLAIN,11));
        t2.setForeground(new Color(T2.getRed(),T2.getGreen(),T2.getBlue(),200));
        titles.add(t1); titles.add(t2);
        left.add(ico); left.add(titles);
        h.add(left, BorderLayout.WEST);
        return h;
    }

    private JPanel buildLeft() {
        JPanel p = new JPanel(new BorderLayout(0,8));
        p.setBackground(ThemeManager.bg()); p.setBorder(new EmptyBorder(14,14,0,7));
        JLabel lbl = new JLabel("Abonnes enregistres");
        lbl.setFont(new Font("Segoe UI",Font.BOLD,12)); lbl.setForeground(T2);
        lbl.setBorder(new EmptyBorder(0,2,4,0)); p.add(lbl, BorderLayout.NORTH);
        model = new DefaultTableModel(
                new String[]{"ID","Matricule","Nom Client","Formule","Mode Paiement","Statut","Expiration"},0) {
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        table = new JTable(model);
        styleTable();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow()>=0) updateCarte(table.getSelectedRow());
        });
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(ThemeManager.border(),1));
        sp.getViewport().setBackground(ThemeManager.card());
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRight() {
        JPanel p = new JPanel(new BorderLayout(0,12));
        p.setBackground(ThemeManager.bg()); p.setBorder(new EmptyBorder(14,7,0,14));
        p.add(buildTarifsPanel(), BorderLayout.NORTH);
        cartePanel = new CartePanel();
        JPanel cw = new JPanel(new BorderLayout(0,6)); cw.setBackground(ThemeManager.bg());
        JLabel lbl = new JLabel("Apercu carte membre");
        lbl.setFont(new Font("Segoe UI",Font.BOLD,12)); lbl.setForeground(T2);
        lbl.setBorder(new EmptyBorder(0,2,4,0));
        cw.add(lbl,BorderLayout.NORTH); cw.add(cartePanel,BorderLayout.CENTER);
        p.add(cw,BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTarifsPanel() {
        JPanel w = new JPanel(new BorderLayout(0,8)); w.setOpaque(false);
        JLabel t = new JLabel("Formules d'abonnement");
        t.setFont(new Font("Segoe UI",Font.BOLD,12)); t.setForeground(T2);
        t.setBorder(new EmptyBorder(0,2,2,0)); w.add(t,BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(1,4,8,0)); grid.setOpaque(false);
        for(int i=0;i<4;i++) grid.add(makeTarifCard(LBLS[i],JOURS[i]+" jours",PRIX[i],COLS[i],i==2));
        w.add(grid,BorderLayout.CENTER);
        return w;
    }

    private JPanel makeTarifCard(String label,String sub,double prix,Color c,boolean pop) {
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.card()); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                g2.setPaint(new GradientPaint(0,0,c,getWidth(),0,new Color(c.getRed(),c.getGreen(),c.getBlue(),120)));
                g2.fillRoundRect(0,0,getWidth(),5,5,5);
                g2.setColor(pop?new Color(c.getRed(),c.getGreen(),c.getBlue(),160):ThemeManager.border());
                g2.setStroke(new BasicStroke(pop?2f:1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                if(pop){g2.setColor(c);g2.fillRoundRect(getWidth()/2-34,0,68,17,8,8);g2.setFont(new Font("Segoe UI",Font.BOLD,8));g2.setColor(Color.WHITE);g2.drawString("POPULAIRE",getWidth()/2-25,12);}
                g2.setFont(new Font("Segoe UI",Font.BOLD,12)); g2.setColor(c);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString(label,(getWidth()-fm.stringWidth(label))/2,pop?37:30);
                String ps=String.format("%.0f",prix);
                g2.setFont(new Font("Segoe UI",Font.BOLD,21)); g2.setColor(ThemeManager.text()); fm=g2.getFontMetrics();
                int px=(getWidth()-fm.stringWidth(ps))/2-7; g2.drawString(ps,px,pop?60:53);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.setColor(c); g2.drawString("DH",px+fm.stringWidth(ps)+2,pop?60:53);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(ThemeManager.textMuted()); fm=g2.getFontMetrics();
                g2.drawString(sub,(getWidth()-fm.stringWidth(sub))/2,pop?74:67);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setPreferredSize(new Dimension(0,84)); return card;
    }

    private JPanel buildFooter() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,12));
        f.setBackground(ThemeManager.card());
        f.setBorder(BorderFactory.createMatteBorder(1,0,0,0,ThemeManager.border()));
        JButton btnAdd   = ThemeManager.createButton("+ Nouvel Abonnement", T1, Color.WHITE);
        JButton btnRec   = ThemeManager.createButton("Voir Recu",  GOLD, new Color(30,30,30));
        JButton btnImp   = ThemeManager.createButton("Imprimer Carte", new Color(30,160,230), Color.WHITE);
        JButton btnDel   = ThemeManager.createButton("Supprimer",  ThemeManager.ACCENT_RED, Color.WHITE);
        JButton btnClose = ThemeManager.createButton("Fermer", ThemeManager.DARK_CARD2, ThemeManager.DARK_TEXT);
        btnAdd.addActionListener(e -> formulaireAjout());
        btnRec.addActionListener(e -> afficherRecu());
        btnImp.addActionListener(e -> {
            if(table.getSelectedRow()<0){NotificationManager.show(parent,"Selectionnez un abonnement.",NotificationManager.Type.WARNING);return;}
            cartePanel.imprimer(parent);
        });
        btnDel.addActionListener(e -> supprimer());
        btnClose.addActionListener(e -> dispose());
        f.add(btnAdd); f.add(btnRec); f.add(btnImp); f.add(btnDel); f.add(btnClose);
        return f;
    }

    // =========================================================================
    // TABLE STYLE
    // =========================================================================
    private void styleTable() {
        table.setBackground(ThemeManager.card()); table.setForeground(ThemeManager.text());
        table.setGridColor(ThemeManager.border()); table.setRowHeight(42);
        table.setShowHorizontalLines(true); table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(ThemeManager.card2());
        table.getTableHeader().setForeground(ThemeManager.textMuted());
        table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,11));
        table.setFont(new Font("Segoe UI",Font.PLAIN,12));
        table.setSelectionBackground(new Color(T1.getRed(),T1.getGreen(),T1.getBlue(),55));
        table.setSelectionForeground(ThemeManager.text());
        // Masquer ID
        table.getColumnModel().getColumn(0).setMinWidth(0); table.getColumnModel().getColumn(0).setMaxWidth(0);

        // Formule
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int row,int col){
                JLabel lbl=new JLabel(v!=null?v.toString():""){
                    @Override protected void paintComponent(Graphics g){
                        Graphics2D g2=(Graphics2D)g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                        Color c=cf(getText());
                        g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),28)); g2.fillRoundRect(3,5,getWidth()-6,getHeight()-10,10,10);
                        g2.setColor(c); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(3,5,getWidth()-6,getHeight()-10,10,10);
                        g2.setFont(getFont()); g2.setColor(c);
                        FontMetrics fm=g2.getFontMetrics(); g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                        g2.dispose();
                    }
                    Color cf(String s){if(s.contains("1 AN"))return new Color(180,100,255);if(s.contains("3"))return new Color(255,180,0);if(s.contains("2"))return new Color(30,160,230);return new Color(0,200,150);}
                };
                lbl.setFont(new Font("Segoe UI",Font.BOLD,11)); lbl.setHorizontalAlignment(CENTER);
                lbl.setBackground(sel?new Color(T1.getRed(),T1.getGreen(),T1.getBlue(),40):ThemeManager.card()); lbl.setOpaque(true); return lbl;
            }
        });

        // Mode Paiement
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int r,int c){
                super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                setBackground(ThemeManager.card()); setHorizontalAlignment(CENTER);
                String s=v!=null?v.toString():"";
                if(s.contains("CARTE")){setForeground(new Color(30,160,230)); setText("Carte bancaire");}
                else{setForeground(new Color(0,200,150)); setText("Especes");}
                return this;
            }
        });

        // Statut
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int r,int c){
                super.getTableCellRendererComponent(t,v,sel,foc,r,c); setBackground(ThemeManager.card()); setHorizontalAlignment(CENTER);
                String s=v!=null?v.toString():"";
                if(s.equals("EXPIRE")){setForeground(ThemeManager.ACCENT_RED);setText("Expire");}
                else if(s.contains("(!)")){setForeground(new Color(255,180,0));}
                else{setForeground(T2);}
                return this;
            }
        });
    }

    // =========================================================================
    // DONNEES
    // =========================================================================
    private void charger() {
        model.setRowCount(0);
        ResultSet rs = dao.getTousAbonnements();
        try {
            while(rs!=null&&rs.next()){
                int j=rs.getInt("jours_restants");
                String formule=deduireFormule(rs.getDate("date_debut"),rs.getDate("date_fin"));
                String statut=j<=0?"EXPIRE":j<=7?j+" j (!)":j+" jours";
                String mode="ESPECES";
                try{mode=rs.getString("mode_paiement");}catch(Exception ignored){}
                model.addRow(new Object[]{
                        rs.getInt("id_abonnement"),rs.getString("matricule"),rs.getString("nom_client"),
                        formule,mode,statut,rs.getDate("date_fin")
                });
            }
        } catch(SQLException e){e.printStackTrace();}
    }

    private String deduireFormule(java.sql.Date d1,java.sql.Date d2){
        if(d1==null||d2==null) return "1 MOIS";
        long d=(d2.getTime()-d1.getTime())/(86400000L);
        if(d>=300)return "1 AN"; if(d>=80)return "3 MOIS"; if(d>=50)return "2 MOIS"; return "1 MOIS";
    }

    private void updateCarte(int row){
        if(row<0||row>=model.getRowCount()) return;
        String mat=model.getValueAt(row,1).toString();
        String nom=model.getValueAt(row,2).toString();
        String formule=model.getValueAt(row,3).toString();
        String mode=model.getValueAt(row,4).toString();
        String fin=model.getValueAt(row,6).toString();
        String statut=model.getValueAt(row,5).toString();
        cartePanel.setData(nom,mat,formule,fin,statut,prixFormule(formule),mode);
    }

    private double prixFormule(String f){
        if(f.contains("1 AN"))return PRIX[3]; if(f.contains("3"))return PRIX[2]; if(f.contains("2"))return PRIX[1]; return PRIX[0];
    }
    private int idxFormule(String f){
        if(f.contains("1 AN"))return 3; if(f.contains("3"))return 2; if(f.contains("2"))return 1; return 0;
    }

    // =========================================================================
    // FORMULAIRE AJOUT + PAIEMENT
    // =========================================================================
    private void formulaireAjout() {
        // Etape 1 : informations
        JDialog dlg=new JDialog(this,"Nouvel Abonnement - Informations",true);
        dlg.setSize(500,390); dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(ThemeManager.bg());

        // Header teal
        JPanel dh=new JPanel(){@Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g;
            g2.setPaint(new GradientPaint(0,0,T_BG,getWidth(),0,new Color(0,60,55)));
            g2.fillRect(0,0,getWidth(),getHeight());
            g2.setColor(T2); g2.fillRect(0,getHeight()-2,getWidth(),2);
        }};
        dh.setPreferredSize(new Dimension(0,56)); dh.setLayout(new FlowLayout(FlowLayout.LEFT,20,15));
        JLabel dtitle=new JLabel("Etape 1/2 — Informations client");
        dtitle.setFont(new Font("Segoe UI",Font.BOLD,14)); dtitle.setForeground(Color.WHITE); dh.add(dtitle);
        dlg.add(dh,BorderLayout.NORTH);

        JPanel form=new JPanel(new GridBagLayout()); form.setBackground(ThemeManager.bg()); form.setBorder(new EmptyBorder(22,32,12,32));
        GridBagConstraints gbc=new GridBagConstraints(); gbc.fill=GridBagConstraints.HORIZONTAL; gbc.insets=new Insets(7,6,7,6);
        JTextField fMat=sField(), fNom=sField();
        String[] opts=new String[4];
        for(int i=0;i<4;i++) opts[i]=LBLS[i]+"  -  "+(int)PRIX[i]+" DH  ("+JOURS[i]+" jours)";
        JComboBox<String> combo=new JComboBox<>(opts);
        combo.setFont(new Font("Segoe UI",Font.PLAIN,12)); combo.setBackground(ThemeManager.card()); combo.setForeground(ThemeManager.text());
        JLabel lblInfo=new JLabel("  150 DH — 30 jours d'acces illimite");
        lblInfo.setFont(new Font("Segoe UI",Font.ITALIC,11)); lblInfo.setForeground(T2);
        combo.addActionListener(e->{int i=combo.getSelectedIndex(); lblInfo.setText("  "+(int)PRIX[i]+" DH — "+JOURS[i]+" jours d'acces illimite");});
        row(form,gbc,"Matricule :",fMat,0); row(form,gbc,"Nom complet :",fNom,1); row(form,gbc,"Formule :",combo,2);
        gbc.gridx=0;gbc.gridy=3;gbc.gridwidth=2; form.add(lblInfo,gbc);
        dlg.add(form,BorderLayout.CENTER);

        JPanel bp=new JPanel(new FlowLayout(FlowLayout.RIGHT,12,12)); bp.setBackground(ThemeManager.bg());
        JButton ok=ThemeManager.createButton("Suivant  >>",T1,Color.WHITE);
        JButton ann=ThemeManager.createButton("Annuler",ThemeManager.DARK_CARD2,ThemeManager.DARK_TEXT);
        ann.addActionListener(e->dlg.dispose());
        ok.addActionListener(e->{
            String mat=fMat.getText().trim().toUpperCase(), nom=fNom.getText().trim();
            if(mat.isEmpty()||nom.isEmpty()){NotificationManager.show(parent,"Remplissez tous les champs.",NotificationManager.Type.WARNING);return;}
            int idx=combo.getSelectedIndex();
            dlg.dispose();
            ouvrirPaiement(mat, nom, JOURS[idx], PRIX[idx], LBLS[idx]);
        });
        bp.add(ok); bp.add(ann); dlg.add(bp,BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /**
     * Etape 2 : fenetre de paiement
     */
    private void ouvrirPaiement(String mat, String nom, int jours, double montant, String formule) {
        JDialog pay=new JDialog(this,"Etape 2/2 — Paiement",true);
        pay.setSize(560,520); pay.setLocationRelativeTo(this);
        pay.setLayout(new BorderLayout());
        pay.getContentPane().setBackground(ThemeManager.bg());

        // Header or/gold
        JPanel ph=new JPanel(){@Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g;
            g2.setPaint(new GradientPaint(0,0,new Color(80,60,0),getWidth(),0,new Color(120,90,0)));
            g2.fillRect(0,0,getWidth(),getHeight());
            g2.setColor(GOLD); g2.fillRect(0,getHeight()-2,getWidth(),2);
        }};
        ph.setPreferredSize(new Dimension(0,56)); ph.setLayout(new BorderLayout());
        ph.setBorder(new EmptyBorder(0,20,0,20));
        JPanel phL=new JPanel(new FlowLayout(FlowLayout.LEFT,10,14)); phL.setOpaque(false);
        JLabel phT=new JLabel("Paiement de l'abonnement"); phT.setFont(new Font("Segoe UI",Font.BOLD,14)); phT.setForeground(Color.WHITE);
        phL.add(phT); ph.add(phL,BorderLayout.WEST);
        // Montant
        JLabel phM=new JLabel(String.format("%.0f DH",montant)); phM.setFont(new Font("Segoe UI",Font.BOLD,22)); phM.setForeground(GOLD);
        JPanel phR=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,12)); phR.setOpaque(false); phR.add(phM); ph.add(phR,BorderLayout.EAST);
        pay.add(ph,BorderLayout.NORTH);

        // Corps central
        JPanel body=new JPanel(new BorderLayout(0,14)); body.setBackground(ThemeManager.bg()); body.setBorder(new EmptyBorder(18,24,10,24));

        // Recap commande
        JPanel recap=new JPanel(){@Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(ThemeManager.card()); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
            g2.setColor(ThemeManager.border()); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
            g2.dispose();
        }};
        recap.setLayout(new GridLayout(3,2,8,6)); recap.setOpaque(false); recap.setBorder(new EmptyBorder(12,16,12,16));
        recap.add(recapLbl("Client :")); recap.add(recapVal(nom));
        recap.add(recapLbl("Matricule :")); recap.add(recapVal(mat));
        recap.add(recapLbl("Formule :")); recap.add(recapVal(formule+" — "+jours+" jours"));
        body.add(recap,BorderLayout.NORTH);

        // Choix mode paiement
        JPanel modePanel=new JPanel(new BorderLayout(0,10)); modePanel.setOpaque(false);
        JLabel modeLbl=new JLabel("Choisissez le mode de paiement :");
        modeLbl.setFont(new Font("Segoe UI",Font.BOLD,12)); modeLbl.setForeground(ThemeManager.textMuted()); modePanel.add(modeLbl,BorderLayout.NORTH);

        JPanel modes=new JPanel(new GridLayout(1,2,14,0)); modes.setOpaque(false);

        // Bouton Especes
        JToggleButton btnEsp=new JToggleButton(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg=isSelected()?new Color(0,150,136,40):ThemeManager.card();
                g2.setColor(bg); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                Color border=isSelected()?T1:ThemeManager.border();
                g2.setColor(border); g2.setStroke(new BasicStroke(isSelected()?2f:1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                // Icone billet
                g2.setColor(isSelected()?T1:ThemeManager.textMuted());
                g2.fillRoundRect(12,22,36,22,5,5);
                g2.setColor(ThemeManager.card()); g2.fillOval(24,28,12,10);
                g2.setStroke(new BasicStroke(1.2f)); g2.setColor(isSelected()?T1:ThemeManager.textMuted());
                g2.drawRoundRect(12,22,36,22,5,5); g2.drawOval(24,28,12,10);
                // Texte
                g2.setFont(new Font("Segoe UI",Font.BOLD,13));
                g2.setColor(isSelected()?T1:ThemeManager.text());
                g2.drawString("Especes",60,38);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10));
                g2.setColor(ThemeManager.textMuted());
                g2.drawString("Paiement comptant",60,54);
                g2.dispose();
            }
        };
        btnEsp.setSelected(true); btnEsp.setOpaque(false); btnEsp.setPreferredSize(new Dimension(0,80));
        btnEsp.setBorderPainted(false); btnEsp.setFocusPainted(false); btnEsp.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Bouton Carte
        JToggleButton btnCarte=new JToggleButton(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg=isSelected()?new Color(30,160,230,40):ThemeManager.card();
                g2.setColor(bg); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                Color border=isSelected()?new Color(30,160,230):ThemeManager.border();
                g2.setColor(border); g2.setStroke(new BasicStroke(isSelected()?2f:1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                // Icone carte
                Color cc=isSelected()?new Color(30,160,230):ThemeManager.textMuted();
                g2.setColor(cc); g2.fillRoundRect(10,20,40,28,5,5);
                g2.setColor(new Color(0,0,0,100)); g2.fillRect(10,26,40,8);
                g2.setColor(GOLD); g2.fillRoundRect(14,34,12,8,3,3);
                // Texte
                g2.setFont(new Font("Segoe UI",Font.BOLD,13));
                g2.setColor(isSelected()?new Color(30,160,230):ThemeManager.text());
                g2.drawString("Carte bancaire",58,36);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.setColor(ThemeManager.textMuted());
                g2.drawString("Simulation terminal",58,52);
                g2.dispose();
            }
        };
        btnCarte.setOpaque(false); btnCarte.setPreferredSize(new Dimension(0,80));
        btnCarte.setBorderPainted(false); btnCarte.setFocusPainted(false); btnCarte.setCursor(new Cursor(Cursor.HAND_CURSOR));

        ButtonGroup bg2=new ButtonGroup(); bg2.add(btnEsp); bg2.add(btnCarte);
        btnEsp.addActionListener(e->{ modes.repaint(); });
        btnCarte.addActionListener(e->{ modes.repaint(); });

        modes.add(btnEsp); modes.add(btnCarte);
        modePanel.add(modes,BorderLayout.CENTER);
        body.add(modePanel,BorderLayout.CENTER);

        // Panneau details carte (visible si carte selectionnee)
        JPanel carteDetails=new JPanel(new GridLayout(2,2,10,8));
        carteDetails.setOpaque(false); carteDetails.setBorder(new EmptyBorder(6,0,0,0));
        JTextField fNomCarte=sField(); fNomCarte.putClientProperty("JTextField.placeholderText","Nom sur la carte");
        JTextField fNumCarte=sField(); fNumCarte.putClientProperty("JTextField.placeholderText","XXXX XXXX XXXX XXXX");
        JTextField fExp=sField(); fExp.putClientProperty("JTextField.placeholderText","MM/AA");
        JTextField fCvv=sField(); fCvv.putClientProperty("JTextField.placeholderText","CVV");
        fExp.setPreferredSize(new Dimension(100,32)); fCvv.setPreferredSize(new Dimension(100,32));
        carteDetails.add(fNomCarte); carteDetails.add(fNumCarte); carteDetails.add(fExp); carteDetails.add(fCvv);
        carteDetails.setVisible(false);
        body.add(carteDetails,BorderLayout.SOUTH);

        btnCarte.addActionListener(e->{ carteDetails.setVisible(true); body.revalidate(); });
        btnEsp.addActionListener(e->{ carteDetails.setVisible(false); body.revalidate(); });

        pay.add(body,BorderLayout.CENTER);

        // Footer
        JPanel fp=new JPanel(new FlowLayout(FlowLayout.RIGHT,12,12)); fp.setBackground(ThemeManager.card());
        fp.setBorder(BorderFactory.createMatteBorder(1,0,0,0,ThemeManager.border()));
        JLabel totalLbl=new JLabel("TOTAL : "+String.format("%.0f DH",montant));
        totalLbl.setFont(new Font("Segoe UI",Font.BOLD,16)); totalLbl.setForeground(GOLD);
        JButton btnPay=ThemeManager.createButton("Confirmer le paiement",T1,Color.WHITE);
        JButton btnAnn=ThemeManager.createButton("Annuler",ThemeManager.DARK_CARD2,ThemeManager.DARK_TEXT);
        btnAnn.addActionListener(e->pay.dispose());
        btnPay.addActionListener(e->{
            boolean estCarte=btnCarte.isSelected();
            String modePaie=estCarte?"CARTE_BANCAIRE":"ESPECES";
            String numTrans=null;

            if(estCarte){
                // Validation basique carte
                String nomC=fNomCarte.getText().trim();
                String numC=fNumCarte.getText().replaceAll("\\s","");
                if(nomC.isEmpty()||numC.length()<12){
                    NotificationManager.show(parent,"Informations carte incompletes.",NotificationManager.Type.WARNING); return;
                }
                // Simulation traitement
                pay.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try{Thread.sleep(1200);}catch(InterruptedException ignored){}
                pay.setCursor(Cursor.getDefaultCursor());
                // Numero transaction genere
                numTrans="TXN-"+String.format("%08d",new Random().nextInt(99999999));
            }

            // Enregistrement
            if(dao.ajouterAbonnement(mat,nom,jours,montant,modePaie,"PAYE",numTrans)){
                pay.dispose();
                NotificationManager.show(parent,"Paiement confirme ! Abonnement actif.",NotificationManager.Type.SUCCESS);
                charger();
                // Afficher recu automatiquement
                afficherRecuDirect(mat,nom,formule,jours,montant,modePaie,numTrans);
            } else {
                NotificationManager.show(parent,"Erreur base de donnees.",NotificationManager.Type.ERROR);
            }
        });
        fp.add(totalLbl); fp.add(Box.createHorizontalStrut(20)); fp.add(btnPay); fp.add(btnAnn);
        pay.add(fp,BorderLayout.SOUTH);
        pay.setVisible(true);
    }

    private JLabel recapLbl(String t){JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",Font.PLAIN,11));l.setForeground(ThemeManager.textMuted());return l;}
    private JLabel recapVal(String t){JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",Font.BOLD,12));l.setForeground(ThemeManager.text());return l;}

    // =========================================================================
    // RECU D'ABONNEMENT
    // =========================================================================
    private void afficherRecu() {
        int row=table.getSelectedRow();
        if(row<0){NotificationManager.show(parent,"Selectionnez un abonnement.",NotificationManager.Type.WARNING);return;}
        String mat=model.getValueAt(row,1).toString();
        String nom=model.getValueAt(row,2).toString();
        String formule=model.getValueAt(row,3).toString();
        String mode=model.getValueAt(row,4).toString();
        String fin=model.getValueAt(row,6).toString();
        double p=prixFormule(formule); int j=JOURS[idxFormule(formule)];
        afficherRecuDirect(mat,nom,formule,j,p,mode,null);
    }

    private void afficherRecuDirect(String mat,String nom,String formule,int jours,double prix,String mode,String numTrans){
        JDialog rd=new JDialog(this,"Recu d'Abonnement",true);
        rd.setSize(480,590); rd.setLocationRelativeTo(this);
        rd.setLayout(new BorderLayout());
        rd.getContentPane().setBackground(ThemeManager.bg());
        RecuPanel rp=new RecuPanel(mat,nom,formule,jours,prix,mode,numTrans);
        rd.add(rp,BorderLayout.CENTER);
        JPanel bp=new JPanel(new FlowLayout(FlowLayout.CENTER,14,12)); bp.setBackground(ThemeManager.bg());
        JButton btnP=ThemeManager.createButton("Imprimer le Recu",T1,Color.WHITE);
        JButton btnC=ThemeManager.createButton("Fermer",ThemeManager.DARK_CARD2,ThemeManager.DARK_TEXT);
        btnP.addActionListener(e->rp.imprimer(parent));
        btnC.addActionListener(e->rd.dispose());
        bp.add(btnP); bp.add(btnC); rd.add(bp,BorderLayout.SOUTH);
        rd.setVisible(true);
    }

    // =========================================================================
    // SUPPRIMER
    // =========================================================================
    private void supprimer(){
        int row=table.getSelectedRow();
        if(row<0){NotificationManager.show(parent,"Selectionnez un abonnement.",NotificationManager.Type.WARNING);return;}
        String mat=model.getValueAt(row,1).toString(); int id=(int)model.getValueAt(row,0);
        if(JOptionPane.showConfirmDialog(this,"Supprimer l'abonnement de "+mat+" ?","Confirmer",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION&&dao.supprimerAbonnement(id)){
            NotificationManager.show(parent,"Abonnement supprime.",NotificationManager.Type.SUCCESS); cartePanel.clearData(); charger();
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private void row(JPanel f,GridBagConstraints g,String label,JComponent c,int r){
        g.gridwidth=1;g.weightx=0.35;g.gridx=0;g.gridy=r;
        JLabel l=new JLabel(label);l.setFont(new Font("Segoe UI",Font.PLAIN,12));l.setForeground(ThemeManager.textMuted()); f.add(l,g);
        g.gridx=1;g.weightx=0.65; f.add(c,g);
    }
    private JTextField sField(){
        JTextField tf=new JTextField(16); tf.setFont(new Font("Segoe UI",Font.PLAIN,12));
        tf.setBackground(ThemeManager.card()); tf.setForeground(ThemeManager.text()); tf.setCaretColor(ThemeManager.text());
        tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ThemeManager.border()),new EmptyBorder(5,9,5,9)));
        return tf;
    }

    // =========================================================================
    // CLASSE : CARTE MEMBRE
    // =========================================================================
    static class CartePanel extends JPanel {
        String nom="",mat="",formule="",fin="",statut="",mode=""; double prix=0; boolean ok=false;
        CartePanel(){setOpaque(false);setPreferredSize(new Dimension(380,215));}
        void setData(String n,String m,String f,String df,String st,double p,String md){nom=n;mat=m;formule=f;fin=df;statut=st;prix=p;mode=md;ok=true;repaint();}
        void clearData(){ok=false;repaint();}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int cw=Math.min(getWidth()-16,400),ch=205,cx=(getWidth()-cw)/2,cy=(getHeight()-ch)/2;
            if(!ok){
                g2.setColor(ThemeManager.card2()); g2.fillRoundRect(cx,cy,cw,ch,18,18);
                g2.setColor(ThemeManager.border());
                g2.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,0,new float[]{7,4},0));
                g2.drawRoundRect(cx,cy,cw,ch,18,18);
                g2.setFont(new Font("Segoe UI",Font.ITALIC,12)); g2.setColor(ThemeManager.textMuted());
                String msg="Cliquez sur un abonne pour voir sa carte"; FontMetrics fm=g2.getFontMetrics();
                g2.drawString(msg,cx+(cw-fm.stringWidth(msg))/2,cy+ch/2+4);
                g2.dispose(); return;
            }
            Color T1c=new Color(0,150,136),T2c=new Color(0,188,170),GOLDc=new Color(255,193,7);
            g2.setPaint(new GradientPaint(cx,cy,new Color(14,30,28),cx+cw,cy+ch,new Color(0,80,70)));
            g2.fillRoundRect(cx,cy,cw,ch,18,18);
            g2.setPaint(new GradientPaint(cx,cy,new Color(255,255,255,18),cx,cy+60,new Color(255,255,255,0)));
            g2.fillRoundRect(cx,cy,cw,ch/2,18,18);
            g2.setColor(new Color(T2c.getRed(),T2c.getGreen(),T2c.getBlue(),15)); g2.fillOval(cx+cw-120,cy-40,200,200); g2.fillOval(cx-50,cy+ch-90,170,170);
            g2.setColor(new Color(T2c.getRed(),T2c.getGreen(),T2c.getBlue(),50)); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(cx,cy,cw,ch,18,18);
            g2.setFont(new Font("Segoe UI",Font.BOLD,10)); g2.setColor(new Color(T2c.getRed(),T2c.getGreen(),T2c.getBlue(),200));
            g2.drawString("SMART PARKING  |  CARTE MEMBRE",cx+18,cy+22);
            g2.setColor(new Color(255,255,255,20)); g2.setStroke(new BasicStroke(0.8f)); g2.drawLine(cx+18,cy+28,cx+cw-18,cy+28);
            Color bc=cf(formule);
            g2.setColor(new Color(bc.getRed(),bc.getGreen(),bc.getBlue(),210)); g2.fillRoundRect(cx+cw-88,cy+10,74,22,10,10);
            g2.setFont(new Font("Segoe UI",Font.BOLD,10)); g2.setColor(Color.WHITE); FontMetrics fm=g2.getFontMetrics();
            g2.drawString(formule,cx+cw-88+(74-fm.stringWidth(formule))/2,cy+25);
            drawChip(g2,cx+18,cy+40,GOLDc);
            g2.setFont(new Font("Courier New",Font.BOLD,16)); g2.setColor(Color.WHITE); g2.drawString(sp(mat),cx+18,cy+107);
            g2.setFont(new Font("Segoe UI",Font.BOLD,13)); g2.setColor(Color.WHITE); g2.drawString(nom.toUpperCase(),cx+18,cy+135);
            g2.setFont(new Font("Segoe UI",Font.PLAIN,8)); g2.setColor(new Color(255,255,255,130)); g2.drawString("EXPIRE LE",cx+18,cy+157);
            g2.setFont(new Font("Segoe UI",Font.BOLD,11)); g2.setColor(Color.WHITE); g2.drawString(fin,cx+18,cy+171);
            g2.setFont(new Font("Segoe UI",Font.PLAIN,8)); g2.setColor(new Color(255,255,255,130)); g2.drawString("MONTANT",cx+cw-108,cy+157);
            g2.setFont(new Font("Segoe UI",Font.BOLD,17)); g2.setColor(GOLDc); g2.drawString(String.format("%.0f DH",prix),cx+cw-108,cy+175);
            boolean actif=!statut.startsWith("EXPIRE");
            g2.setColor(actif?new Color(0,200,150,210):new Color(255,70,70,210)); g2.fillRoundRect(cx+cw-72,cy+ch-26,62,18,8,8);
            g2.setFont(new Font("Segoe UI",Font.BOLD,9)); g2.setColor(Color.WHITE);
            String st2=actif?"ACTIF":"EXPIRE"; fm=g2.getFontMetrics(); g2.drawString(st2,cx+cw-72+(62-fm.stringWidth(st2))/2,cy+ch-13);
            // Mode paiement
            g2.setFont(new Font("Segoe UI",Font.PLAIN,8)); g2.setColor(new Color(255,255,255,100));
            String modeAff=mode.contains("CARTE")?"Carte bancaire":"Especes";
            g2.drawString(modeAff,cx+20,cy+ch-14);
            g2.setColor(new Color(T2c.getRed(),T2c.getGreen(),T2c.getBlue(),70)); g2.setStroke(new BasicStroke(1.5f));
            for(int i=0;i<3;i++){int r=7+i*8;g2.drawArc(cx+cw-44-r,cy+ch-44-r,r*2,r*2,-50,100);}
            g2.dispose();
        }
        private void drawChip(Graphics2D g2,int x,int y,Color gold){
            g2.setColor(new Color(gold.getRed(),gold.getGreen(),gold.getBlue(),230)); g2.fillRoundRect(x,y,36,26,5,5);
            g2.setColor(new Color(200,160,0,200)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(x,y,36,26,5,5);
            g2.drawLine(x+12,y,x+12,y+26); g2.drawLine(x+24,y,x+24,y+26); g2.drawLine(x,y+9,x+36,y+9); g2.drawLine(x,y+17,x+36,y+17);
        }
        private String sp(String m){if(m.length()<=8)return m; StringBuilder sb=new StringBuilder();for(int i=0;i<m.length();i++){if(i>0&&i%3==0)sb.append(' ');sb.append(m.charAt(i));}return sb.toString();}
        private Color cf(String f){if(f.contains("1 AN"))return new Color(180,100,255);if(f.contains("3"))return new Color(255,180,0);if(f.contains("2"))return new Color(30,160,230);return new Color(0,200,150);}
        void imprimer(Frame parent){
            PrinterJob job=PrinterJob.getPrinterJob(); job.setJobName("Carte-"+mat); final CartePanel self=this;
            job.setPrintable((g,pf,pi)->{if(pi>0)return Printable.NO_SUCH_PAGE; Graphics2D g2=(Graphics2D)g; g2.translate(pf.getImageableX(),pf.getImageableY()); g2.scale(2.0,2.0); self.paint(g2); return Printable.PAGE_EXISTS;});
            if(job.printDialog()){try{job.print();}catch(PrinterException ex){JOptionPane.showMessageDialog(parent,"Erreur : "+ex.getMessage());}}
        }
    }

    // =========================================================================
    // CLASSE : RECU D'ABONNEMENT
    // =========================================================================
    static class RecuPanel extends JPanel {
        String mat,nom,formule,mode,numTrans; double prix; int jours;
        String dateEmission=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now());
        Color T1c=new Color(0,150,136),T2c=new Color(0,188,170),GOLDc=new Color(255,193,7),T_BGc=new Color(14,30,28);

        RecuPanel(String mat,String nom,String formule,int jours,double prix,String mode,String numTrans){
            this.mat=mat;this.nom=nom;this.formule=formule;this.jours=jours;this.prix=prix;
            this.mode=mode!=null?mode:"ESPECES"; this.numTrans=numTrans;
            setOpaque(false); setPreferredSize(new Dimension(440,540));
        }

        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int pw=420,ph=510,px=(getWidth()-pw)/2,py=10;
            // Ombre
            g2.setColor(new Color(0,0,0,40)); g2.fillRoundRect(px+4,py+4,pw,ph,18,18);
            // Fond
            g2.setColor(ThemeManager.card()); g2.fillRoundRect(px,py,pw,ph,18,18);
            // Bande teal haut
            g2.setPaint(new GradientPaint(px,py,T_BGc,px+pw,py,new Color(0,80,70)));
            g2.fillRoundRect(px,py,pw,84,18,18); g2.fillRect(px,py+66,pw,20);
            // Bord
            g2.setColor(ThemeManager.border()); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(px,py,pw,ph,18,18);
            // Titre
            g2.setFont(new Font("Segoe UI",Font.BOLD,20)); g2.setColor(Color.WHITE);
            FontMetrics fm=g2.getFontMetrics(); String title="SMART PARKING";
            g2.drawString(title,px+(pw-fm.stringWidth(title))/2,py+36);
            g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.setColor(new Color(T2c.getRed(),T2c.getGreen(),T2c.getBlue(),200));
            fm=g2.getFontMetrics(); String sub="RECU D'ABONNEMENT";
            g2.drawString(sub,px+(pw-fm.stringWidth(sub))/2,py+56);
            // Separateur
            g2.setColor(ThemeManager.border()); g2.setStroke(new BasicStroke(1f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,0,new float[]{5,4},0));
            g2.drawLine(px+20,py+96,px+pw-20,py+96);
            // Lignes info
            int y=py+120;
            ligne(g2,"Date d'emission :",dateEmission,px,y,pw,T2c); y+=34;
            ligne(g2,"Matricule :",mat,px,y,pw,ThemeManager.text()); y+=34;
            ligne(g2,"Client :",nom,px,y,pw,ThemeManager.text()); y+=34;
            ligne(g2,"Formule :",formule,px,y,pw,cfColor(formule)); y+=34;
            ligne(g2,"Duree :",jours+" jours",px,y,pw,ThemeManager.text()); y+=34;
            // Mode paiement avec icone
            String modeAff=mode.contains("CARTE")?"Carte bancaire":"Especes (cash)";
            Color modeColor=mode.contains("CARTE")?new Color(30,160,230):T2c;
            ligne(g2,"Mode de paiement :",modeAff,px,y,pw,modeColor); y+=34;
            if(numTrans!=null&&!numTrans.isEmpty()){
                ligne(g2,"N Transaction :",numTrans,px,y,pw,new Color(180,100,255)); y+=34;
            }
            // Separateur
            g2.setColor(ThemeManager.border()); g2.setStroke(new BasicStroke(1f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,0,new float[]{5,4},0));
            g2.drawLine(px+20,y+4,px+pw-20,y+4); y+=22;
            // Total
            g2.setFont(new Font("Segoe UI",Font.BOLD,13)); g2.setColor(ThemeManager.textMuted()); g2.drawString("MONTANT PAYE",px+28,y+18);
            String ps=String.format("%.2f DH",prix);
            g2.setFont(new Font("Segoe UI",Font.BOLD,28)); g2.setColor(GOLDc); fm=g2.getFontMetrics();
            g2.drawString(ps,px+pw-28-fm.stringWidth(ps),y+22); y+=46;
            // Badge paiement confirme
            g2.setColor(new Color(0,150,136,200)); g2.fillRoundRect(px+pw/2-70,y,140,26,12,12);
            g2.setFont(new Font("Segoe UI",Font.BOLD,11)); g2.setColor(Color.WHITE); fm=g2.getFontMetrics();
            String conf="PAIEMENT CONFIRME"; g2.drawString(conf,px+pw/2-fm.stringWidth(conf)/2,y+17); y+=40;
            // Pied
            g2.setPaint(new GradientPaint(px,py+ph-38,T_BGc,px+pw,py+ph-38,new Color(0,80,70)));
            g2.fillRoundRect(px,py+ph-38,pw,38,18,18); g2.fillRect(px,py+ph-58,pw,22);
            g2.setFont(new Font("Segoe UI",Font.ITALIC,10)); g2.setColor(new Color(T2c.getRed(),T2c.getGreen(),T2c.getBlue(),180));
            fm=g2.getFontMetrics(); String merci="Merci de votre confiance — Smart Parking System";
            g2.drawString(merci,px+(pw-fm.stringWidth(merci))/2,py+ph-15);
            // QR code
            drawQr(g2,px+pw-76,py+ph-100,T1c);
            g2.dispose();
        }

        private void ligne(Graphics2D g2,String lbl,String val,int px,int y,int pw,Color vc){
            g2.setFont(new Font("Segoe UI",Font.PLAIN,11)); g2.setColor(ThemeManager.textMuted()); g2.drawString(lbl,px+28,y);
            g2.setFont(new Font("Segoe UI",Font.BOLD,12)); g2.setColor(vc); FontMetrics fm=g2.getFontMetrics();
            g2.drawString(val,px+pw-28-fm.stringWidth(val),y);
            g2.setColor(new Color(ThemeManager.border().getRed(),ThemeManager.border().getGreen(),ThemeManager.border().getBlue(),70));
            g2.setStroke(new BasicStroke(0.5f)); g2.drawLine(px+28,y+4,px+pw-28,y+4);
        }
        private void drawQr(Graphics2D g2,int x,int y,Color c){
            int s=56; g2.setColor(ThemeManager.card()); g2.fillRect(x,y,s,s);
            g2.setColor(ThemeManager.text()); g2.setStroke(new BasicStroke(0.5f)); g2.drawRect(x,y,s,s);
            int[] p={0b11100111,0b10100101,0b11100111,0b00011000,0b11100111,0b10100101,0b11100111};
            int cell=s/8; for(int r=0;r<7;r++) for(int col=0;col<8;col++) if(((p[r]>>(7-col))&1)==1){g2.setColor(ThemeManager.text());g2.fillRect(x+col*cell,y+r*cell,cell,cell);}
            // Petit logo centre
            g2.setColor(c); g2.fillRoundRect(x+20,y+20,16,16,4,4);
            g2.setFont(new Font("Segoe UI",Font.BOLD,8)); g2.setColor(Color.WHITE); g2.drawString("P",x+25,y+31);
        }
        private Color cfColor(String f){if(f.contains("1 AN"))return new Color(180,100,255);if(f.contains("3"))return new Color(255,180,0);if(f.contains("2"))return new Color(30,160,230);return T2c;}
        void imprimer(Frame parent){
            PrinterJob job=PrinterJob.getPrinterJob(); job.setJobName("Recu-Abonnement-"+mat); final RecuPanel self=this;
            job.setPrintable((g,pf,pi)->{if(pi>0)return Printable.NO_SUCH_PAGE; Graphics2D g2=(Graphics2D)g; g2.translate(pf.getImageableX(),pf.getImageableY()); g2.scale(1.5,1.5); self.paint(g2); return Printable.PAGE_EXISTS;});
            if(job.printDialog()){try{job.print();}catch(PrinterException ex){JOptionPane.showMessageDialog(parent,"Erreur : "+ex.getMessage());}}
        }
    }
}