/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow.java,v 1.7 2005/09/23 19:56:54 wakko666 Exp $
 * 
 * Created on 08-Aug-2005
 * Change Log:
 */
package ui;

import game.*;
import javax.swing.*;
import java.awt.*;
import ui.hexmap.*;

/**
 * @author Erik Vos
 */
public class MapWindow extends JFrame {
    
    private MapManager mmgr;
    private HexMap map;
    
    public MapWindow () {
        
        mmgr = MapManager.getInstance();
        try {
            map = (HexMap) Class.forName(mmgr.getMapUIClassName()).newInstance();
        } catch (Exception e) {
            System.out.println("Map class instantiation error:\n");
            e.printStackTrace();
            return;
        }       
                     
        addMouseListener(map);
        getContentPane().add(map);
        setSize(900, 600);
        setLocation(25, 25);
        setVisible(true);
        setTitle ("Rails: Game Map");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
