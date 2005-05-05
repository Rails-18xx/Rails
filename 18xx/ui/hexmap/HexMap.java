/*
 * Created on May 4, 2005
 */
package ui.hexmap;

import javax.swing.*;
import java.awt.*;

/**
 * @author blentz
 */
public class HexMap extends JFrame
{
   public HexMap()
   {
      GUIMapHex h = new GUIMapHex(0,0);
      
      this.getContentPane().add(h);
   }
}
