package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import rails.game.*;
import rails.ui.swing.hexmap.*;
import rails.util.LocalText;


public class UpgradesPanel extends Box implements MouseListener, ActionListener
{

	private ArrayList upgrades;
	private JPanel upgradePanel;
	private JScrollPane scrollPane;
	private Dimension preferredSize = new Dimension(100, 200);
	private Border border = new EtchedBorder();
	
	private final String INIT_CANCEL_TEXT = "NoTile";
	private final String INIT_DONE_TEXT = "LayTile";
	
	private boolean cancelEnabled = false;
	private boolean doneEnabled = false;
	private boolean tileMode = false;
	private boolean tokenMode = false;
	private boolean lastEnabled = false;

	private JButton cancelButton = new JButton(LocalText.getText(INIT_CANCEL_TEXT));
	private JButton doneButton = new JButton(LocalText.getText(INIT_DONE_TEXT));
	
	private HexMap hexMap;

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
		
		doneButton.setActionCommand("Done");
		doneButton.setMnemonic(KeyEvent.VK_D);
		doneButton.addActionListener(this);
		cancelButton.setActionCommand("Cancel");
		cancelButton.setMnemonic(KeyEvent.VK_C);
		cancelButton.addActionListener(this);
		
		add(scrollPane);
		showUpgrades();
	}

	public void repaint()
	{
		showUpgrades();
	}

	public void populate()
	{
	    if (hexMap == null) hexMap = GameUILoader.getMapPanel().getMap();
	    
		try
		{
			upgrades = (ArrayList) hexMap
					.getSelectedHex()
					.getCurrentTile()
					.getValidUpgrades(hexMap
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
			GameUILoader.orWindow.setMessage(LocalText.getText("NoTiles"));
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

		upgradePanel.add(doneButton);
		upgradePanel.add(cancelButton);

		lastEnabled = doneEnabled;
	}

	private BufferedImage getHexImage(int tileId)
	{
		return GameUILoader.getImageLoader().getTile(tileId);
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
		cancelButton.setText(LocalText.getText(text));
	}

	public void setDoneText(String text)
	{
		doneButton.setText(LocalText.getText(text));
	}

	public void setDoneEnabled(boolean enabled)
	{
		doneButton.setEnabled(doneEnabled = enabled);
	}
	
	public void setCancelEnabled(boolean enabled)
	{
		cancelButton.setEnabled(cancelEnabled = enabled);
		//new Exception ("Cancel "+(cancelEnabled?"EN":"DIS")+"ABLED").printStackTrace(System.out);
	}

	public void actionPerformed(ActionEvent e)
	{

	    if (hexMap == null) hexMap = GameUILoader.getMapPanel().getMap();
		String command = e.getActionCommand();
		Object source = e.getSource();

		if (source == cancelButton /*command.equals("Cancel")*/)
		{
			GameUILoader.orWindow.processCancel();
		}
		else if (source == doneButton /*command.equals("Done")*/)
		{
			if (hexMap.getSelectedHex() != null)
			{
				GameUILoader.orWindow.processDone();
			}
			else
			{
				GameUILoader.orWindow.processCancel();
			}

		}
		upgrades = null; //???
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
	    setDoneEnabled(false);
	    setCancelEnabled(false);
	}
}
