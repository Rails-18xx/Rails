 /* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/Attic/MapWindow.java,v 1.36 2005/11/16 23:07:56 evos Exp $
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
 * MapWindow class displays the Map Window. It's shocking, I know.
 * 
 * @author Erik Vos
 * @author Brett
 */
public class MapWindow extends JFrame implements WindowListener
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
		map.addMouseMotionListener(map);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
		
		scrollPane = new JScrollPane(map);
		scrollPane.setSize(map.getPreferredSize());
		
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		// Add area to show upgrade tiles
		upgradePanel = new UpgradesPanel(map);
		contentPane.add (upgradePanel, BorderLayout.WEST);
		map.setUpgradesPanel(upgradePanel);
		
		setSize(map.getPreferredSize().width+100,
		        map.getPreferredSize().height+40);
		setLocation(25, 25);
		setTitle("Rails: Game Map");
		
		addWindowListener(this);		
		pack();
	}
	
	public void enableTileLaying (boolean enabled) {
	    map.enableTileLaying(enabled);
	}
	

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		StatusWindow.uncheckMenuItemBox(StatusWindow.mapString);
		dispose();		
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowOpened(WindowEvent e)
	{
	}
}
