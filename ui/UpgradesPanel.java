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
	private HexMap map;
	private Dimension preferredSize = new Dimension(75, 200);
	private Border border = new EtchedBorder();

	public UpgradesPanel(HexMap map)
	{
		super(BoxLayout.Y_AXIS);
		
		this.map = map;
		
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
				BufferedImage hexImage = getHexImage(tile.getId());
				ImageIcon hexIcon = new ImageIcon(hexImage);
				
				//Cheap n' Easy rescaling.
				hexIcon.setImage(hexIcon.getImage().getScaledInstance(
						(int)(hexIcon.getIconHeight() * 0.3),
						(int)(hexIcon.getIconWidth() * 0.3), 
						Image.SCALE_FAST));
				
				JLabel hexLabel = new JLabel(hexIcon);
				hexLabel.setName(tile.getName());
				hexLabel.setText("" + tile.getId());
				hexLabel.setOpaque(true);
				hexLabel.setVisible(true);
				hexLabel.setBorder(border);
				hexLabel.addMouseListener(this);
				
				upgradePanel.add(hexLabel);
			}
		}
		
		invalidate();
		repaint();
	}
	
	private BufferedImage getHexImage(int tileId)
	{
		ImageLoader il = new ImageLoader();		
		return il.getTile(tileId);
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
		HexMap.selectedHex.setTileImage(getHexImage(Integer.parseInt(((JLabel)e.getSource()).getText())));
		HexMap.selectedHex.setTileId(Integer.parseInt(((JLabel)e.getSource()).getText()));
		HexMap.selectedHex.getHexModel().getCurrentTile().setId(Integer.parseInt(((JLabel)e.getSource()).getText()));

		map.repaint();
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
