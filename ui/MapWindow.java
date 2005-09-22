/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow.java,v 1.6 2005/09/22 21:48:51 evos Exp $
 * 
 * Created on 08-Aug-2005
 * Change Log:
 */
package ui;

import game.*;
import javax.swing.*;
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
        
        getContentPane().add(map);
        addMouseListener(map);
        
        setTitle ("Rails: Game Map");
        setSize(1000, 600);
        setLocation(25, 25);
        setContentPane (map);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
