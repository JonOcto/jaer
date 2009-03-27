/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ITDJFramePanel.java
 *
 * Created on 25.03.2009, 19:14:16
 */

package ch.unizh.ini.jaer.projects.holger;

/**
 *
 * @author Holger
 */
public class ITDJFramePanel extends javax.swing.JFrame {

    /** Creates new form ITDJFramePanel */
    public ITDJFramePanel() {
        initComponents();
        this.setSize(400,300);
        this.repaint();
    }

    public void setITD(double ITD){
        jLabelITD.setText(ITD+" #bin");
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        binsPanel1 = new ch.unizh.ini.jaer.projects.holger.BinsPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabelITD = new javax.swing.JLabel();

        setTitle("ITD-Bins");
        setBackground(new java.awt.Color(0, 0, 0));
        setBounds(new java.awt.Rectangle(0, 0, 2, 0));
        setForeground(java.awt.Color.black);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
        });

        binsPanel1.setBorder(null);

        jLabel1.setText("ITD:");

        jLabelITD.setText("0");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(180, 180, 180)
                .addComponent(jLabel1)
                .addGap(9, 9, 9)
                .addComponent(jLabelITD))
            .addComponent(binsPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(binsPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(50, 50, 50)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabelITD)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        this.repaint();
    }//GEN-LAST:event_formWindowActivated

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public ch.unizh.ini.jaer.projects.holger.BinsPanel binsPanel1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelITD;
    // End of variables declaration//GEN-END:variables

}
