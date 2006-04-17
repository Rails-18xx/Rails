/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/elements/Attic/Caption.java,v 1.2 2006/04/17 14:17:06 evos Exp $
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
       private static final Color NORMAL_BG_COLOR = new Color (240, 240, 240);
       private static final Color HIGHLIGHT_BG_COLOUR = new Color (255, 255, 80);
 	
       public Caption (String text) {
           super (text);
           this.setBackground(NORMAL_BG_COLOR);
           this.setHorizontalAlignment(SwingConstants.CENTER);
           this.setBorder (labelBorder);
           this.setOpaque(true);
       }
       
       public void setHighlight (boolean highlight) {
           this.setBackground (highlight ? HIGHLIGHT_BG_COLOUR : NORMAL_BG_COLOR);
       }
   }