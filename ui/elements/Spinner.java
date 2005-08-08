/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/elements/Attic/Spinner.java,v 1.1 2005/08/08 20:08:25 evos Exp $
 * 
 * Created on 06-Aug-2005
 * Change Log:
 */
package ui.elements;

import java.awt.Color;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;


public class Spinner extends JSpinner {
       
       private Color buttonColour = new Color (255, 220, 150);

       public Spinner (int value, int from, int to, int step) {
           super(new SpinnerNumberModel (new Integer(value), new Integer(from),
                   to > 0 ? new Integer(to) : null, new Integer (step)));
           this.setBackground(buttonColour);
           this.setOpaque(true);
           this.setVisible(false);
       }
      
   }