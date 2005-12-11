/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/elements/Attic/Field.java,v 1.5 2005/12/11 21:06:49 evos Exp $
 * 
 * Created on 06-Aug-2005
 * Change Log:
 */
package ui.elements;

import game.model.ModelObject;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import ui.StatusWindow;


public class Field extends JLabel implements ViewObject {
 
       private Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);
       private Color normalColour = Color.WHITE;
       private Color highlightColour = new Color (255, 255, 80);
       private ModelObject modelObject;
       private boolean pull = false;

      public Field (String text) {
           super(text.equals("0%") ? "" : text);
           this.setBackground(normalColour);
           this.setHorizontalAlignment(SwingConstants.CENTER);
           this.setBorder (labelBorder);
           this.setOpaque(true);
      }
      
      public Field (ModelObject modelObject) {
          this (modelObject.toString());
          this.modelObject = modelObject;
          if (StatusWindow.useObserver) modelObject.addObserver(this);
      }
      
      public Field (ModelObject modelObject, boolean pull) {
          this (modelObject);
          this.pull = pull;
      }
      
      public ModelObject getModel () {
          return modelObject;
      }
      
      public void setHighlight (boolean highlight) {
          setBackground (highlight ? highlightColour : normalColour);
      }
      
      /** This method can be omitted whenever we start using the Observer pattern, 
       * which is already prepared in the second constructor and the next two methods */
      /* WARNING: setText also calls repaint(), so we must take care to avoid a loop! */
      //private boolean repaintCalled = false;
      public void paintComponent(Graphics g) {
          if (modelObject != null && (pull || !StatusWindow.useObserver)) {
	          setText (modelObject.toString());
          }
          super.paintComponent(g);
       }
      
      /** Needed to satisfy the Observer interface. Currently not used. */
      public void update (Observable o1, Object o2) {
          if (StatusWindow.useObserver) setText(modelObject.toString());
      }
      
      /** Needed to saitsfy the ViewObject interface. Currently not used. */
      public void deRegister () {
          if (StatusWindow.useObserver) modelObject.deleteObserver(this);
      }
      
   }