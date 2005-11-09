package ui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import game.*;

public class UpgradesPanel extends Box
{

	private ArrayList upgrades;
	private JPanel upgradePanel;

	private Dimension preferredSize = new Dimension(75, 800);
	private Border border = new EtchedBorder();

	public UpgradesPanel()
	{
		super(BoxLayout.Y_AXIS);
		setSize(preferredSize);
		setVisible(true);

		upgrades = new ArrayList();
		upgradePanel = new JPanel();

		JLabel label = new JLabel("<html>Select<br>an<br>upgrade:</html>");
		label.setOpaque(true);
		label.setBackground(Color.WHITE);
		add(label);

		upgradePanel.setOpaque(true);
		upgradePanel.setBackground(Color.DARK_GRAY);
		upgradePanel.setBorder(border);
		add(upgradePanel);
		
		showUpgrades();
	}

	public void showUpgrades()
	{
		upgradePanel.removeAll();

		if (upgrades != null)
		{
			Iterator it = upgrades.iterator();

			while (it.hasNext())
			{
				TileI tile = (TileI) it.next();
				JLabel hex = new JLabel("Tile #" + tile.getId());
				hex.setOpaque(true);
				hex.setVisible(true);
				hex.setBorder(border);

				upgradePanel.add(hex);
				System.out.println("Upgrade tile: " + tile.getId());
			}
		}
		
		invalidate();
		repaint();
	}
	
	public Dimension getPreferredSize()
	{
		return preferredSize;
	}

	public void setPreferredSize(Dimension preferredSize)
	{
		this.preferredSize = preferredSize;
	}

	public ArrayList getUpgrades()
	{
		return upgrades;
	}

	public void setUpgrades(ArrayList upgrades)
	{
		this.upgrades = upgrades;
	}
}
