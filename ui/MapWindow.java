/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow.java,v 1.1 2005/08/08 20:08:29 evos Exp $
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
        
        mmgr = MapManager.get();
        try {
            map = (HexMap) Class.forName(mmgr.getMapClassName()).newInstance();
        } catch (Exception e) {
            System.out.println("Map class instantiation error:\n");
            e.printStackTrace();
            return;
        }
        
        setTitle ("Game Map");
        setSize(800, 300);
        setLocation(25, 450);
        setContentPane (map);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent actor) {
        
    }

}
