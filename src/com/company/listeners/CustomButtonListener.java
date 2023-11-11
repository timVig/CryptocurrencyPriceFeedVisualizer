package com.company.listeners;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class defines a listener which is linked to any button has a coin label, in order to update the search term
 * for the coin we want to look for to the one on the button.
 */
public class CustomButtonListener implements ActionListener {
    private String text;
    private JLabel label;
    public CustomButtonListener( JLabel label, String text ){
        this.label = label;
        this.text = text;
    }
    @Override public void actionPerformed(ActionEvent e) { this.label.setText( this.text ); }
}
