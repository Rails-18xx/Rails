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
	// private HexMap map;
	// private ORWindow parent;
	private Dimension preferredSize = new Dimension(75, 200);
	private Border border = new EtchedBorder();
	private JButton cancel;
	private JButton done;
	// private JLabel label;

	/*
	 * private static final String selectAHex = "<html>Select a hex or press
	 * the \"No Tile\" button</html>"; private static final String tileText = "<html>Select
	 * a tile, select another hex, or press the \"No Tile\" button</html>";
	 * private static final String noUpgrades = "<html>AT the moment there is
	 * no valid upgrade for this hex. Select another hex or press the \"No
	 * Tile\" button</html>"; private static final String tokenText = "<html>Select
	 * a city hex to lay a token on that hex, or press the \"No Toek\" button</html>";
	 * private static final String doneText = "Done"; public static final String
	 * cancelText = "Cancel"; public static final String noTileText = "No tile";
	 * public static final String noTokenText = "No token";
	 */

	private String cancelButtonKey = "NoTile";
	private String doneButtonKey = "LayTile";
	private boolean doneEnabled = false;

	private boolean tileMode = false;
	private boolean tokenMode = false;

	private boolean lastEnabled = false;

	public UpgradesPanel(HexMap map, ORWindow parent)
	{
		super(BoxLayout.Y_AXIS);

		// this.map = map;
		// this.parent = parent;

		setSize(preferredSize);
		setVisible(true);

		upgrades = null;
		upgradePanel = new JPanel();

		// label = new JLabel(tileText);
		// label.setOpaque(true);
		// label.setBackground(Color.WHITE);
		// label.setAlignmentX((float) 0.5);
		// label.setAlignmentY((float) 0.5);
		// label.setHorizontalAlignment(SwingConstants.CENTER);
		// add(label);
		parent.setMessage("SelectAHexForTile");

		upgradePanel.setOpaque(true);
		upgradePanel.setBackground(Color.DARK_GRAY);
		upgradePanel.setBorder(border);
		add(upgradePanel);

		showUpgrades();
	}
	
	public void repaint()
	{
		showUpgrades();
	}

	private void showUpgrades()
	{
		upgradePanel.removeAll();

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
		catch(NullPointerException e)
		{
			upgrades = null;
		}

		if (tokenMode)
		{
		}
		else if (upgrades == null)
		{
		}
		else if (upgrades.size() == 0)
		{
			// parent.setMessage("NoTiles");
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
			// label.setText(tileText);
			// parent.setMessage("SelectATile");
		}

		done = new JButton(doneButtonKey);
		done.setActionCommand("Done");
		done.setMnemonic(KeyEvent.VK_D);
		done.addActionListener(this);
		done.setEnabled(doneEnabled);
		upgradePanel.add(done);

		cancel = new JButton(cancelButtonKey);
		cancel.setActionCommand("Cancel");
		cancel.setMnemonic(KeyEvent.VK_C);
		cancel.addActionListener(this);
		cancel.setEnabled(true);
		upgradePanel.add(cancel);

		//revalidate();
		//repaint();

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
		// label.setText(tileText);
		// parent.setMessage("SelectAHexForTile");
		setUpgrades(null);
		// setDoneText("LayTile");
		// setDoneEnabled(false);
		// setCancelText("NoTile");
	}

	public void initBaseTokenLaying(boolean tokenMode)
	{
		this.tokenMode = tokenMode;
		// label.setText(tokenMode ? tokenText : tileText);
		// parent.setMessage(tokenMode ? "SelectAHexForToken" : "SelectATile");
		setUpgrades(null);
		// setDoneText(tokenMode ? "LayToken" : "LayTile");
		// setDoneEnabled(false);
		// setCancelText(tokenMode ? "NoToken" : "NoTile");
	}

	public void setCancelText(String text)
	{
		cancel.setText(cancelButtonKey = text);
	}

	public void setDoneText(String text)
	{
		done.setText(doneButtonKey = text);
		// try {
		// throw new Exception ("Set to "+text);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
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
			GameUILoader.orWindow.processCancel();
		}
		else if (command.equals("Done"))
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
		// setDoneEnabled(false);
		showUpgrades();
	}

	public void mouseClicked(MouseEvent e)
	{
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
