/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2016 Aaron Madlon-Kay
               Home page: https://www.omegat.org/
               Support center: https://omegat.org/support

 This file is part of OmegaT.

 OmegaT is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 OmegaT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **************************************************************************/

package org.omegat.gui.issues;

/**
 * A simple GUI component for displaying source and target text side-by-side to
 * illustrate an "issue" (a problem with the translation).
 *
 * @author Aaron Madlon-Kay <aaron@madlon-kay.com>
 */
@SuppressWarnings("serial")
public class TripleSplitButtonPanel extends javax.swing.JPanel {

    /**
     * Creates new form IssueDetailSplitPanel
     */
    public TripleSplitButtonPanel() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        firstPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        firstTextPane = new javax.swing.JTextPane();
        firstButton = new javax.swing.JButton();
        middlePanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        middleTextPane = new javax.swing.JTextPane();
        middleButton = new javax.swing.JButton();
        lastPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        lastTextPane = new javax.swing.JTextPane();
        lastButton = new javax.swing.JButton();

        setLayout(new java.awt.GridLayout(1, 0));

        firstPanel.setLayout(new java.awt.BorderLayout());

        firstTextPane.setEditable(false);
        jScrollPane1.setViewportView(firstTextPane);

        firstPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        firstButton.setContentAreaFilled(false);
        firstPanel.add(firstButton, java.awt.BorderLayout.SOUTH);

        add(firstPanel);

        middlePanel.setLayout(new java.awt.BorderLayout());

        middleTextPane.setEditable(false);
        jScrollPane2.setViewportView(middleTextPane);

        middlePanel.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        middleButton.setContentAreaFilled(false);
        middlePanel.add(middleButton, java.awt.BorderLayout.SOUTH);

        add(middlePanel);

        lastPanel.setLayout(new java.awt.BorderLayout());

        lastTextPane.setEditable(false);
        jScrollPane3.setViewportView(lastTextPane);

        lastPanel.add(jScrollPane3, java.awt.BorderLayout.CENTER);
        lastPanel.add(lastButton, java.awt.BorderLayout.SOUTH);

        add(lastPanel);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JButton firstButton;
    javax.swing.JPanel firstPanel;
    javax.swing.JTextPane firstTextPane;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    javax.swing.JButton lastButton;
    javax.swing.JPanel lastPanel;
    javax.swing.JTextPane lastTextPane;
    javax.swing.JButton middleButton;
    javax.swing.JPanel middlePanel;
    javax.swing.JTextPane middleTextPane;
    // End of variables declaration//GEN-END:variables
}
