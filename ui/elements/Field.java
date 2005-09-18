/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/elements/Attic/Field.java,v 1.2 2005/09/18 21:36:25 evos Exp $
 * 
 * Created on 06-Aug-2005
 * Change Log:
 */
package ui.elements;

import java.awt.Color;
import java.util.EventListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;


public class Field extends JLabel {
 
       private Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);
       private Color normalColour = Color.WHITE;
       private Color highlightColour = new Color (255, 255, 80);

      public Field (String text) {
           super(text.equals("0%")?"":text);
           this.setBackground(normalColour);
           this.setHorizontalAlignment(SwingConstants.CENTER);
           this.setBorder (labelBorder);
           this.setOpaque(true);
       }
      
      public void setHighlight (boolean highlight) {
          
          setBackground (highlight ? highlightColour : normalColour);
          
      }
      
   }