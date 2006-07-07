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
	private JScrollPane scrollPane;
	private Dimension preferredSize = new Dimension(100, 200);
	private Border border = new EtchedBorder();
	private JButton cancel;
	private JButton done;

	private String cancelButtonKey = "NoTile";
	private String doneButtonKey = "LayTile";
	private boolean doneEnabled = false;

	private boolean tileMode = false;
	private boolean tokenMode = false;

	private boolean lastEnabled = false;

	public static final String DONE = "Done";
	public static final String CANCEL = "Cancel";

	public UpgradesPanel()
	{
		super(BoxLayout.Y_AXIS);

		setSize(preferredSize);
		setVisible(true);

		upgrades = null;
		upgradePanel = new JPanel();

		upgradePanel.setOpaque(true);
		upgradePanel.setBackground(Color.DARK_GRAY);
		upgradePanel.setBorder(border);
		upgradePanel.setLayout(new GridLayout(15, 1));

		scrollPane = new JScrollPane(upgradePanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setSize(getPreferredSize());
		
		add(scrollPane);
		showUpgrades();
	}

	public void repaint()
	{
		showUpgrades();
	}

	public void populate()
	{
		try
		{
			upgrades = (ArrayList) GameUILoader.getMapPanel()
					.getMap()
					.getSelectedHex()
					.getCurrentTile()
					.getValidUpgrades(GameUILoader.getMapPanel()
							.getMap()
							.getSelectedHex()
							.getHexModel(),
							GameManager.getCurrentPhase());
		}
		catch (NullPointerException e)
		{
			upgrades = null;
		}
	}

	private void showUpgrades()
	{
		upgradePanel.removeAll();

		if (tokenMode)
		{
		}
		else if (upgrades == null)
		{
		}
		else if (upgrades.size() == 0)
		{
			GameUILoader.orWindow.setMessage("NoTiles");
		}
		else
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
		}

		done = new JButton(doneButtonKey);
		done.setActionCommand(DONE);
		done.setMnemonic(KeyEvent.VK_D);
		done.addActionListener(this);
		done.setEnabled(doneEnabled);
		upgradePanel.add(done);

		cancel = new JButton(cancelButtonKey);
		cancel.setActionCommand(CANCEL);
		cancel.setMnemonic(KeyEvent.VK_C);
		cancel.addActionListener(this);
		cancel.setEnabled(true);
		upgradePanel.add(cancel);

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

	public void setTileMode(boolean tileMode)
	{
		this.tileMode = tileMode;
		setUpgrades(null);
	}

	public void setBaseTokenMode(boolean tokenMode)
	{
		this.tokenMode = tokenMode;
		setUpgrades(null);
	}

	public void setCancelText(String text)
	{
		cancel.setText(cancelButtonKey = text);
	}

	public void setDoneText(String text)
	{
		done.setText(doneButtonKey = text);
	}

	public void setDoneEnabled(boolean enabled)
	{
		done.setEnabled(doneEnabled = enabled);
	}

	public void actionPerformed(ActionEvent e)
	{

		String command = e.getActionCommand();

		if (command.equals(CANCEL))
		{
			GameUILoader.orWindow.processCancel();
		}
		else if (command.equals(DONE))
		{
			if (GameUILoader.getMapPanel().getMap().getSelectedHex() != null)
			{
				GameUILoader.orWindow.processDone();
			}
			else
			{
				GameUILoader.orWindow.processCancel();
			}

		}
		upgrades = null;
		showUpgrades();
	}

	public void mouseClicked(MouseEvent e)
	{
		if (!(e.getSource() instanceof JLabel))
			return;

		HexMap map = GameUILoader.getMapPanel().getMap();

		int id = Integer.parseInt(((JLabel) e.getSource()).getText());
		if (map.getSelectedHex().dropTile(id))
		{
			/* Lay tile */
			map.repaint(map.getSelectedHex().getBounds());
			GameUILoader.orWindow.setSubStep(ORWindow.ROTATE_OR_CONFIRM_TILE);
		}
		else
		{
			/* Tile cannot be laid in a valid orientation: refuse it */
			JOptionPane.showMessageDialog(this,
					"This tile cannot be laid in a valid orientation.");
			upgrades.remove(TileManager.get().getTile(id));
			GameUILoader.orWindow.setSubStep(ORWindow.SELECT_TILE);
			showUpgrades();
		}

	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}
	
	public void finish() {
	    cancel.setEnabled(false);
	    done.setEnabled(false);
	}
}
