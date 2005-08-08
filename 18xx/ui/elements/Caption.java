/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/elements/Attic/Caption.java,v 1.1 2005/08/08 20:08:26 evos Exp $
 * 
 * Created on 06-Aug-2005
 * Change Log:
 */
package ui.elements;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;


public class Caption extends JLabel {
       
       private Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);
       private Color captionColour = new Color (240, 240, 240);
       private Color highlightColour = new Color (255, 255, 80);
 	
       public Caption (String text) {
           super (text);
           this.setBackground(captionColour);
           this.setHorizontalAlignment(SwingConstants.CENTER);
           this.setBorder (labelBorder);
           this.setOpaque(true);
       }
       
       public void setHighlight (boolean highlight) {
           this.setBackground (highlight ? highlightColour : captionColour);
       }
   }