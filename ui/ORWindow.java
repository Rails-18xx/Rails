/*
 * Created on Apr 29, 2005
 */
package ui;

import game.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * This Window displays the available operations that may be performed during an
 * Operating Round. This window also contains the Game Map.
 * 
 */
public class ORWindow extends JFrame implements WindowListener
{
	private MapPanel mapPanel;
	private ORPanel ORPanel;
	private UpgradesPanel upgradePanel;

	public ORWindow(OperatingRound round, StatusWindow parent)
	{
		super();
		getContentPane().setLayout(new BorderLayout());

		if(round != null)
		{
			mapPanel = GameUILoader.mapPanel;
			
			upgradePanel = new UpgradesPanel(mapPanel.getMap());
			getContentPane().add(upgradePanel, BorderLayout.WEST);
			mapPanel.setUpgradesPanel(upgradePanel);
			mapPanel.getMap().setUpgradesPanel(upgradePanel);
			
			ORPanel = new ORPanel(round, parent, this);
			getContentPane().add(ORPanel, BorderLayout.SOUTH);
			setSize(800, 750);
		}
		else if (OperatingRound.getLastORNumber() > 0)
		{
			mapPanel = GameUILoader.mapPanel;
			setSize(mapPanel.getSize());
			mapPanel.setVisible(true);
		}
		else
		{
			mapPanel = new MapPanel();
			setSize(mapPanel.getSize());
		}
		
		getContentPane().add(mapPanel, BorderLayout.CENTER);

		setTitle("Rails: Map");
		setLocation(10, 10);
		setVisible(true);
		addWindowListener(this);

		LogWindow.addLog();
	}
	
	public MapPanel getMapPanel()
	{
		return mapPanel;
	}

	
	public ORPanel getORPanel()
	{
		return ORPanel;
	}
	
	public void windowActivated(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		StatusWindow.uncheckMenuItemBox(StatusWindow.MAP);
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