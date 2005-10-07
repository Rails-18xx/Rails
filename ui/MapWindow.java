 /* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow.java,v 1.20 2005/10/07 23:32:18 wakko666 Exp $
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
public class MapWindow extends JFrame
{
	private MapManager mmgr;
	private HexMap map;
	private JScrollPane scrollPane;

	public MapWindow()
	{
		GUIHex.setOverlay(true);
		Scale.set(15);
		
		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());

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
		
		map.addMouseListener(map);
		addMouseListener(map);
		addWindowListener(map);

		scrollPane = new JScrollPane(map);
		scrollPane.setSize(map.getPreferredSize());
		
		//XXX: I'm using this to smoke out bugs elsewhere in the drawing code.		
		//This mode uses the very simple method of redrawing 
		//the entire contents of the scrollpane each time it is scrolled.
		scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		setSize(map.getPreferredSize());
		setLocation(25, 25);
		setTitle("Rails: Game Map");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		System.out.println("HexMap.isLightWeight? " + map.isLightweight());
	}
}
