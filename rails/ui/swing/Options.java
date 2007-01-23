package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.ui.swing.GameUILoader;
import rails.util.Config;
import rails.util.LocalText;
import rails.util.Util;


import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

/**
 * The Options dialog window displays the first window presented to the user.
 * This window contains all of the options available for starting a new rails.game.
 */
public class Options extends JDialog implements ActionListener
{

	GridBagConstraints gc;
	JPanel optionsPane, playersPane, buttonPane;
	JButton newButton, loadButton, quitButton;
	JComboBox[] playerBoxes;
	JComboBox gameNameBox;
	JTextField[] playerNameFields;
	BasicComboBoxRenderer renderer;
	Dimension size, optSize;
	
	protected static Logger log = Logger.getLogger(Options.class.getPackage().getName());

	private void initialize()
	{
		gc = new GridBagConstraints();

		optionsPane = new JPanel();
		playersPane = new JPanel();
		buttonPane = new JPanel();

		newButton = new JButton("New Game");
		loadButton = new JButton("Load Game");
		quitButton = new JButton("Quit");

		newButton.setMnemonic(KeyEvent.VK_N);
		loadButton.setMnemonic(KeyEvent.VK_L);
		quitButton.setMnemonic(KeyEvent.VK_Q);

		renderer = new BasicComboBoxRenderer();
		size = new Dimension(50, 30);
		optSize = new Dimension(50, 50);
		gameNameBox = new JComboBox();

		playerBoxes = new JComboBox[Player.MAX_PLAYERS];
		playerNameFields = new JTextField[Player.MAX_PLAYERS];

		this.getContentPane().setLayout(new GridBagLayout());
		this.getContentPane().setLayout(new GridBagLayout());
		this.setTitle("Rails: New Game");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		renderer.setPreferredSize(size);

		playersPane.add(new JLabel("Players:"));
		playersPane.add(new JLabel(""));
		playersPane.setLayout(new GridLayout(Player.MAX_PLAYERS + 1, 0));
		playersPane.setBorder(BorderFactory.createLoweredBevelBorder());

		for (int i = 0; i < playerBoxes.length; i++)
		{
			playerBoxes[i] = new JComboBox();
			playerBoxes[i].setRenderer(renderer);
			playerBoxes[i].addItem("None");
			playerBoxes[i].addItem("Human");
			playerBoxes[i].addItem("AI eventually goes here.");
			playerBoxes[i].setSelectedIndex(1);
			playersPane.add(playerBoxes[i]);
			playerBoxes[i].setPreferredSize(size);

			playerNameFields[i] = new JTextField();
			playerNameFields[i].setPreferredSize(size);
			playersPane.add(playerNameFields[i]);
		}

		//playerNameFields[0].setText("0");
		//playerNameFields[1].setText("1");
		//playerNameFields[2].setText("2");
		/* Prefill with any configured player names.
		 * This can be useful to speed up testing purposes.
		 */
		String testPlayerList = Config.get("default_players");
		if (Util.hasValue(testPlayerList)) {
			String[] testPlayers = testPlayerList.split(",");
			for (int i=0; i<testPlayers.length; i++) {
				playerNameFields[i].setText(testPlayers[i]);
			}
		}

		populateGameList(getGameList(), gameNameBox);

		optionsPane.add(new JLabel("Game Options"));
		optionsPane.add(new JLabel(""));
		
		optionsPane.add(new JLabel("Game:"));
		optionsPane.add(gameNameBox);
		optionsPane.setLayout(new GridLayout(5, 2));
		optionsPane.setBorder(BorderFactory.createLoweredBevelBorder());
		optionsPane.setPreferredSize(optSize);

		newButton.addActionListener(this);
		loadButton.addActionListener(this);
		quitButton.addActionListener(this);

		//XXX: Until we can load/save a rails.game, we'll set this to disabled to reduce confusion.
		loadButton.setEnabled(false);
		
		buttonPane.add(newButton);
		buttonPane.add(loadButton);
		buttonPane.add(quitButton);
		buttonPane.setBorder(BorderFactory.createLoweredBevelBorder());
	}

	private void populateGridBag()
	{
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		gc.gridwidth = 1;
		gc.fill = GridBagConstraints.BOTH;
		this.getContentPane().add(playersPane, gc);

		gc.gridx = 0;
		gc.gridy = 1;
		gc.fill = 1;
		gc.weightx = 0.5;
		gc.weighty = 0.5;
		gc.gridwidth = 1;
		// gc.ipadx = 50;
		gc.ipady = 50;
		this.getContentPane().add(optionsPane, gc);

		gc.gridx = 0;
		gc.gridy = 2;
		gc.weightx = 0.0;
		gc.weighty = 0.0;
		gc.gridwidth = 1;
		gc.ipady = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		this.getContentPane().add(buttonPane, gc);
	}

	private String[] getGameList()
	{
		File dataDir = new File("./data/");
		String[] files = dataDir.list();
		if (files == null || files.length == 0)
		{
			// Search in the jar
			File jarFile = new File("./Rails-" + Game.version + ".jar");
			try
			{
				JarFile jf = new JarFile(jarFile);
				JarInputStream jis = new JarInputStream(new FileInputStream(jarFile));
				String jeName;
				Pattern p = Pattern.compile("data/(\\w+)/Game.xml");
				Matcher m;
				List games = new ArrayList();
				for (JarEntry je = jis.getNextJarEntry(); je != null; je = jis.getNextJarEntry())
				{
					m = p.matcher(je.getName());
					if (m.matches())
					{
						// Found a rails.game
						games.add(m.group(1));
					}
				}
				files = (String[]) games.toArray(new String[0]);
			}
			catch (IOException e)
			{
			    log.debug ("While opening jar file: " + jarFile, e);
			}
		}
		return files;
	}

	private void populateGameList(String[] gameNames, JComboBox gameNameBox)
	{
		Arrays.sort(gameNames);
		for (int i = 0; i < gameNames.length; i++)
		{
			if (!gameNames[i].equalsIgnoreCase("CVS")
					&& !gameNames[i].startsWith("."))
				gameNameBox.addItem(gameNames[i]);
		}
	}

	public Options()
	{
		super();

		initialize();
		populateGridBag();

		this.pack();
		this.setVisible(true);
	}

	public void actionPerformed(ActionEvent arg0)
	{
		if (arg0.getSource().equals(newButton))
		{
			ArrayList playerNames = new ArrayList();

			for (int i = 0; i < playerBoxes.length; i++)
			{
				if (playerBoxes[i].getSelectedItem()
						.toString()
						.equalsIgnoreCase("Human")
						&& !playerNameFields[i].getText().equals(""))
				{
					playerNames.add(playerNameFields[i].getText());
				}
			}

			if (playerNames.size() < Player.MIN_PLAYERS
					|| playerNames.size() > Player.MAX_PLAYERS)
			{
				if (JOptionPane.showConfirmDialog(this,
						"Not enough players. Continue Anyway?",
						"Are you sure?",
						JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
				{
					return;
				}
			}

			try
			{
				this.setVisible(false);

				String gameName = gameNameBox.getSelectedItem().toString();
				Game.initialise(gameName);
				Game.getPlayerManager(playerNames);
				
				List variants = GameManager.getVariants();
				if (variants != null && variants.size() > 1) {
				    String variant = (String) JOptionPane.showInputDialog (
				            this,
				            LocalText.getText("WHICH_VARIANT", gameName),
				            "", 
				            JOptionPane.PLAIN_MESSAGE,
				            null,
				            (String[])variants.toArray(new String[0]),
				            (String)variants.get(0));
				    if (variant != null) GameManager.setVariant(variant);
				}
				GameManager.getInstance().startGame();

				GameUILoader.gameUIInit();
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
				JOptionPane.showMessageDialog(this,
						"Unable to load selected rails.game.");
			}

		}

		if (arg0.getSource().equals(loadButton))
		{
			// We're not going to actually DO anything with the selected file
			// until the infrastructure for saved games is built
			JFileChooser fc = new JFileChooser();
			int result = fc.showOpenDialog(this.getContentPane());
		}

		if (arg0.getSource().equals(quitButton))
		{
			System.exit(0);
		}

	}
}
