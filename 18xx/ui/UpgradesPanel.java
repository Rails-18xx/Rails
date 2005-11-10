package ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import game.*;
import ui.hexmap.*;

public class UpgradesPanel extends Box implements MouseListener
{

	private ArrayList upgrades;
	private JPanel upgradePanel;

	private Dimension preferredSize = new Dimension(75, 200);
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
				BufferedImage hexImage = getHexImage(tile);
				ImageIcon hexIcon = new ImageIcon(hexImage);
				
				//Cheap n' Easy rescaling.
				hexIcon.setImage(hexIcon.getImage().getScaledInstance(
						(int)(hexIcon.getIconHeight() * 0.3),
						(int)(hexIcon.getIconWidth() * 0.3), 
						Image.SCALE_FAST));
				
				JLabel hexLabel = new JLabel(hexIcon);
				hexLabel.setText("" + tile.getId());
				hexLabel.setName(tile.getName());
				
				JPanel hexPanel = new JPanel();				
				hexPanel.setOpaque(true);
				hexPanel.setVisible(true);
				hexPanel.setBorder(border);
				hexPanel.addMouseListener(this);
				hexPanel.add(hexLabel);
				
				upgradePanel.add(hexPanel);
				System.out.println("Upgrade tile: " + tile.getId());
			}
		}
		
		invalidate();
		repaint();
	}
	
	private BufferedImage getHexImage(TileI tile)
	{
		ImageLoader il = new ImageLoader();		
		return il.getTile(tile.getId());
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

	public void mouseClicked(MouseEvent e)
	{
		System.out.println("Click.");
	}

	public void mouseEntered(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	public void mouseReleased(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}
}
