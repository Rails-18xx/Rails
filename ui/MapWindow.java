/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow.java,v 1.2 2005/08/11 20:46:28 evos Exp $
 * 
 * Created on 08-Aug-2005
 * Change Log:
 */
package ui;

import game.*;

import java.awt.event.*;

import javax.swing.*;

import ui.hexmap.*;

/**
 * @author Erik Vos
 */
public class MapWindow extends JFrame implements ActionListener {
    
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
        
        setTitle ("Game Map");
        setSize(1000, 500);
        setLocation(25, 25);
        setContentPane (map);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent actor) {
        
    }

}
