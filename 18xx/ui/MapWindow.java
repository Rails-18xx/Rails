 /* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow.java,v 1.24 2005/10/21 20:47:46 wakko666 Exp $
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
 * MapWindow class displays the Map Window. It's shocking, I know.
 * 
 * @author Erik Vos
 * @author Brett
 */
public class MapWindow extends JFrame
{
	private MapManager mmgr;
	private HexMap map;
	private ScrollPane scrollPane;

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

		scrollPane = new ScrollPane();
		scrollPane.add(map);
		scrollPane.setSize(map.getPreferredSize());
		//scrollPane.setLocation(100,100);
		
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		setSize(map.getPreferredSize());
		setLocation(25, 25);
		setTitle("Rails: Game Map");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
}
