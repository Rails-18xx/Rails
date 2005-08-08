/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/elements/Attic/Select.java,v 1.1 2005/08/08 20:08:26 evos Exp $
 * 
 * Created on 06-Aug-2005
 * Change Log:
 */
package ui.elements;

import java.awt.Color;

import javax.swing.JComboBox;


public class Select extends JComboBox {
       
       private Color buttonColour = new Color (255, 220, 150);

       public Select (int[] values) {
           super();
           for (int i=0; i<values.length; i++) {
               this.addItem (""+values[i]);
           }
           this.setBackground(buttonColour);
           this.setOpaque(true);
           this.setVisible(false);
       }
       
       public Select (Object[] values) {
           super(values);
           this.setBackground(buttonColour);
           this.setOpaque(true);
           this.setVisible(false);
       }
  }