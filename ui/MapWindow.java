 /* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow.java,v 1.29 2005/11/09 22:23:13 evos Exp $
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
	private JScrollPane scrollPane;
	protected UpgradesPanel upgradePanel;

	public MapWindow()
	{
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
		map.addMouseMotionListener(map);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
		
		scrollPane = new JScrollPane(map);
		//scrollPane.add(map);
		scrollPane.setSize(map.getPreferredSize());
		
		contentPane.add(scrollPane, BorderLayout.CENTER);
		//contentPane.add(map, BorderLayout.CENTER);
		
		// Add area to show upgrade tiles
		upgradePanel = new UpgradesPanel();
		contentPane.add (upgradePanel, BorderLayout.WEST);
		map.setUpgradesPanel(upgradePanel);
		
		setSize(map.getPreferredSize().width+100,
		        map.getPreferredSize().height+40);
		setLocation(25, 25);
		setTitle("Rails: Game Map");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
}
