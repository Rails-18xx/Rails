package ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import game.*;
import ui.hexmap.*;

public class UpgradesPanel extends Box implements MouseListener, ActionListener
{

	private ArrayList upgrades;
	private JPanel upgradePanel;
	private HexMap map;
	private Dimension preferredSize = new Dimension(75, 200);
	private Border border = new EtchedBorder();
	private JButton cancel;
	private JButton done;

	public UpgradesPanel(HexMap map)
	{
		super(BoxLayout.Y_AXIS);
		
		this.map = map;
		
		setSize(preferredSize);
		setVisible(true);

		upgrades = new ArrayList();
		upgradePanel = new JPanel();

		JLabel label = new JLabel("<html><center>Select an<br>upgrade:</center></html>");
		label.setOpaque(true);
		label.setBackground(Color.WHITE);
		label.setAlignmentX((float)0.5);
		label.setAlignmentY((float)0.5);
		label.setHorizontalAlignment(SwingConstants.CENTER);
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

		if (upgrades != null && upgrades.size() > 0)
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
			
			cancel = new JButton("Cancel");
			cancel.setActionCommand("Cancel");
			cancel.setMnemonic(KeyEvent.VK_C);
			cancel.addActionListener(this);
			cancel.setEnabled(true);
			upgradePanel.add(cancel);

			done = new JButton("Done");
			done.setActionCommand("Done");
			done.setMnemonic(KeyEvent.VK_D);
			done.addActionListener(this);
			done.setEnabled(true);
			upgradePanel.add(done);

		}
		
		revalidate();
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
	
	public void actionPerformed (ActionEvent e) {

		String command = e.getActionCommand();

		if (command.equals("Cancel")) {
		    map.getSelectedHex().removeTile();
		} else if (command.equals ("Done")) {
		    map.getSelectedHex().fixTile();
		}
		map.repaint();

		upgrades = null;
		showUpgrades();
	}

	public void mouseClicked(MouseEvent e)
	{
	    /*
		HexMap.selectedHex.setTileImage(getHexImage(Integer.parseInt(((JLabel)e.getSource()).getText())));
		HexMap.selectedHex.setTileIdInteger.parseInt(((JLabel)e.getSource()).getText())
		HexMap.selectedHex.getHexModel().getCurrentTile().setId(Integer.parseInt(((JLabel)e.getSource()).getText()));
		*/
	    map.getSelectedHex().dropTile(Integer.parseInt(((JLabel)e.getSource()).getText()));

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
