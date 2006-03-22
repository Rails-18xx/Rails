/*
 * Created on Apr 29, 2005
 */
package ui;

import game.*;
import ui.hexmap.*;

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
	private MessagePanel messagePanel;

	/* Substeps in tile and token laying */
	public static final int INACTIVE = 0;
	public static final int SELECT_HEX_FOR_TILE = 1;
	public static final int SELECT_TILE = 2;
	public static final int ROTATE_OR_CONFIRM_TILE = 3;
	public static final int SELECT_HEX_FOR_TOKEN = 4;
	public static final int CONFIRM_TOKEN = 5;

	/* Message key per substep */
	protected static final String[] messageKey = new String[] { "",
			"SelectAHexForTile", "SelectATile", "RotateTile",
			"SelectAHexForToken", "ConfirmToken" };

	protected static int subStep = INACTIVE;
	public static boolean baseTokenLayingEnabled = false;
	/**
	 * Is tile laying enabled? If not, one can play with tiles, but the "Done"
	 * button is disabled.
	 */
	public static boolean tileLayingEnabled = false;

	public ORWindow()
	{
		super();
		getContentPane().setLayout(new BorderLayout());

		messagePanel = new MessagePanel();
		getContentPane().add(messagePanel, BorderLayout.NORTH);

		if (mapPanel == null)
			mapPanel = new MapPanel();
		else
			mapPanel = GameUILoader.getMapPanel();
		getContentPane().add(mapPanel, BorderLayout.CENTER);
		addMouseListener(mapPanel.getMap());

		upgradePanel = new UpgradesPanel();
		getContentPane().add(upgradePanel, BorderLayout.WEST);
		addMouseListener(upgradePanel);

		ORPanel = new ORPanel();
		getContentPane().add(ORPanel, BorderLayout.SOUTH);

		setTitle("Rails: Map");
		setLocation(10, 10);
		setVisible(true);
		setSize(800, 600);
		addWindowListener(this);

		LogWindow.addLog();
	}

	public void setMessage(String messageKey)
	{
		messagePanel.setMessage(messageKey);
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
		// parent.setOrWindow(null);
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

	public int getSubStep()
	{
		return subStep;
	}

	public void setSubStep(int subStep)
	{
		ORWindow.subStep = subStep;
		if (this != null)
		{
			setMessage(messageKey[subStep]);
		}
		if (upgradePanel != null)
		{
			upgradePanel.setDoneText(Game.getText(subStep < 4 ? "LayTile"
					: "LayToken"));
			upgradePanel.setCancelText(Game.getText(subStep < 4 ? "NoTile"
					: "NoToken"));
			upgradePanel.setDoneEnabled(subStep == 3 || subStep == 5);
		}

	}

	public void processDone()
	{
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();
		setSubStep(INACTIVE);
		if (baseTokenLayingEnabled)
		{
			if (selectedHex != null)
			{
				if (selectedHex.getHexModel().getStations().size() == 1)
					selectedHex.fixToken(0);
				else
				{
					Object[] stations = selectedHex.getHexModel()
							.getStations()
							.toArray();
					Station station = (Station) JOptionPane.showInputDialog(this,
							"Which station to place the token in?",
							"Which station?",
							JOptionPane.PLAIN_MESSAGE,
							null,
							stations,
							stations[0]);

					selectedHex.fixToken(selectedHex.getHexModel()
							.getStations()
							.indexOf(station));
				}
			}
		}
		else
		{
			if (selectedHex != null)
				selectedHex.fixTile(tileLayingEnabled);
		}
		
		updateUpgradePanel();
	}

	public void processCancel()
	{
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();
		setSubStep(INACTIVE);
		if (baseTokenLayingEnabled)
		{
			if (selectedHex != null)
				selectedHex.removeToken();
			ORPanel.layBaseToken(null, 0);
		}
		else
		{
			if (selectedHex != null)
				selectedHex.removeTile();
			if (tileLayingEnabled)
				ORPanel.layTile(null, null, 0);
		}
		
		updateUpgradePanel();
	}

	public void enableTileLaying(boolean enabled)
	{
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();

		if (!tileLayingEnabled && enabled)
		{
			/* Start tile laying step */
			setSubStep(SELECT_HEX_FOR_TILE);
		}
		else if (tileLayingEnabled && !enabled)
		{
			/* Finish tile laying step */
			if (selectedHex != null)
			{
				selectedHex.removeTile();
				selectedHex.setSelected(false);
				mapPanel.getMap().repaint(selectedHex.getBounds());
				selectedHex = null;
			}
			setSubStep(INACTIVE);
		}
		tileLayingEnabled = enabled;
	}

	public void enableBaseTokenLaying(boolean enabled)
	{
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();

		if (!baseTokenLayingEnabled && enabled)
		{
			/* Start token laying step */
			setSubStep(SELECT_HEX_FOR_TOKEN);
		}
		else if (baseTokenLayingEnabled && !enabled)
		{
			/* Finish token laying step */
			if (selectedHex != null)
			{
				selectedHex.removeToken();
				selectedHex.setSelected(false);
				mapPanel.getMap().repaint(selectedHex.getBounds());
				selectedHex = null;
			}
			setSubStep(INACTIVE);
		}
		baseTokenLayingEnabled = enabled;
	}
	
	public void updateUpgradePanel()
	{
		upgradePanel.setVisible(false);
		upgradePanel.setVisible(true);
	}
	
	public void updateORPanel()
	{
		ORPanel.setVisible(false);
		ORPanel.setVisible(true);
	}
	
	public static void updateORWindow()
	{
		if(GameManager.getInstance().getCurrentRound() instanceof StockRound)
		{
			GameUILoader.statusWindow.updateStatus();
		}
		else
		{
			GameUILoader.orWindow.repaint();
		}
	}
}
