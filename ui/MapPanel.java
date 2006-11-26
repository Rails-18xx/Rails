package ui;

import game.*;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import ui.hexmap.*;

/**
 * MapWindow class displays the Map Window. It's shocking, I know.
 */
public class MapPanel extends JPanel 
{

	private MapManager mmgr;
	private HexMap map;
	private JScrollPane scrollPane;

	public MapPanel()
	{
		Scale.set(15);
		setLayout(new BorderLayout());

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

		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

		scrollPane = new JScrollPane(map);
		scrollPane.setSize(map.getPreferredSize());

		add(scrollPane, BorderLayout.CENTER);

		setSize(map.getPreferredSize().width,
				map.getPreferredSize().height);
		setLocation(25, 25);
	}

	/*
	public void setSpecialTileLays(ArrayList specials)
	{
		map.setSpecials(specials);
	}
	*/
	public void setAllowedTileLays (List allowedTileLays) {
	    map.setAllowedTileLays (allowedTileLays);
	}

	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_F1)
		{
			HelpWindow.displayHelp(GameManager.getInstance().getHelp());
			e.consume();
		}
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public HexMap getMap()
	{
		return map;
	}
}
