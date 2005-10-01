 /* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow.java,v 1.17 2005/10/01 00:00:15 wakko666 Exp $
 * 
 * Created on 08-Aug-2005
 * Change Log:
 */
package ui;

import game.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import ui.hexmap.*;

/**
 * @author Erik Vos
 */
public class MapWindow extends JFrame
{
	private MapManager mmgr;
	private HexMap map;
	private ScrollPane scrollPane;

	public MapWindow()
	{
		GUIHex.setOverlay(true);
		
		mmgr = MapManager.getInstance();
		try
		{
			map = (HexMap) Class.forName(mmgr.getMapUIClassName())
					.newInstance();
		}
		catch (Exception e)
		{
			System.out.println("Map class instantiation error:\n");
			e.printStackTrace();
			return;
		}
		
		scrollPane = new ScrollPane();
		scrollPane.add(map);	
		
		map.addMouseListener(map);
		scrollPane.addMouseListener(map);
		addMouseListener(map);

		/* setPreferredSize does not compile in Java 1.4.2. */
        //scrollPane.setPreferredSize(map.getMinimumSize());
        scrollPane.setSize(map.getMinimumSize());

		//setPreferredSize(scrollPane.getPreferredSize());
		setSize(scrollPane.getPreferredSize());
		
		getContentPane().add(scrollPane);
		setLocation(25, 25);
		setTitle("Rails: Game Map");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		System.out.println("HexMap.isLightWeight? " + map.isLightweight());
	}
}
