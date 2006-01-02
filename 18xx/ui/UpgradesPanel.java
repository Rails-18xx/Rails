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
	private JLabel label;

	private static final String tileText = "<html><center>Select<br>a tile:</center></html>";
	private static final String noUpgrades = "<html><center>No valid upgrades!</center></html>";
	private static final String tokenText = "<html><center>Click city hex to lay a token</center></html>";
	private static final String doneText = "Done";
	public static final String cancelText = "Cancel";
	public static final String noTileText = "No tile";
	public static final String noTokenText = "No token";

	private String cancelButtonText = noTileText;
	private boolean doneEnabled = false;

	private boolean tileMode = false;
	private boolean tokenMode = false;

	private boolean lastEnabled = false;

	public UpgradesPanel(HexMap map)
	{
		super(BoxLayout.Y_AXIS);

		this.map = map;

		setSize(preferredSize);
		setVisible(true);

		upgrades = new ArrayList();
		upgradePanel = new JPanel();

		label = new JLabel(tileText);
		label.setOpaque(true);
		label.setBackground(Color.WHITE);
		label.setAlignmentX((float) 0.5);
		label.setAlignmentY((float) 0.5);
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

		if (upgrades != null && upgrades.size() > 0 && !tokenMode)
		{
			Iterator it = upgrades.iterator();

			while (it.hasNext())
			{
				TileI tile = (TileI) it.next();
				BufferedImage hexImage = getHexImage(tile.getId());
				ImageIcon hexIcon = new ImageIcon(hexImage);

				// Cheap n' Easy rescaling.
				hexIcon.setImage(hexIcon.getImage()
						.getScaledInstance((int) (hexIcon.getIconHeight() * 0.3),
								(int) (hexIcon.getIconWidth() * 0.3),
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
			label.setText(tileText);
		}
		else if (!tokenMode)
		{
			label.setText(noUpgrades);
		}

		done = new JButton(doneText);
		done.setActionCommand("Done");
		done.setMnemonic(KeyEvent.VK_D);
		done.addActionListener(this);
		done.setEnabled(doneEnabled);
		upgradePanel.add(done);

		cancel = new JButton(cancelButtonText);
		cancel.setActionCommand("Cancel");
		cancel.setMnemonic(KeyEvent.VK_C);
		cancel.addActionListener(this);
		cancel.setEnabled(true);
		upgradePanel.add(cancel);

		revalidate();
		repaint();

		lastEnabled = doneEnabled;
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

	public void initTileLaying(boolean tileMode)
	{
		this.tileMode = tileMode;
		label.setText(tileText);
		setUpgrades(null);
		setDoneEnabled(false);
		setCancelText(noTileText);
	}

	public void initBaseTokenLaying(boolean tokenMode)
	{
		this.tokenMode = tokenMode;
		label.setText(tokenMode ? tokenText : tileText);
		setUpgrades(null);
		setDoneEnabled(false);
		setCancelText(tokenMode ? noTokenText : noTileText);
		// showUpgrades ();
	}

	public void setCancelText(String text)
	{
		cancel.setText(cancelButtonText = text);
	}

	public void setDoneEnabled(boolean enabled)
	{
		done.setEnabled(doneEnabled = enabled);
	}

	public void actionPerformed(ActionEvent e)
	{

		String command = e.getActionCommand();

		if (command.equals("Cancel"))
		{
			map.processCancel();
		}
		else if (command.equals("Done"))
		{
			if (map.getSelectedHex() != null)
			{
				map.processDone();
			}
			else
			{
				map.processCancel();
			}

		}
		map.repaint();

		upgrades = null;
		setDoneEnabled(false);
		showUpgrades();
	}

	public void mouseClicked(MouseEvent e)
	{
		int id = Integer.parseInt(((JLabel) e.getSource()).getText());
		if (map.getSelectedHex().dropTile(id))
		{
			/* Accept tile */
			map.repaint();
			setDoneEnabled(true);
			setCancelText(cancelText);
		}
		else
		{
			/* Tile cannot be laid in a valid orientation: refuse it */
			JOptionPane.showMessageDialog(this,
					"This tile cannot be laid in a valid orientation.");
			upgrades.remove(TileManager.get().getTile(id));
			setDoneEnabled(false);
			showUpgrades();
			setCancelText(noTileText);
		}

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
