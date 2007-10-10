/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/GameSetupWindow.java,v 1.4 2007/10/10 18:43:42 wakko666 Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.ui.swing.GameUIManager;
import rails.util.*;

import java.util.*;
import java.util.List;

/**
 * The Options dialog window displays the first window presented to the user.
 * This window contains all of the options available for starting a new
 * rails.game.
 */
public class GameSetupWindow extends JDialog implements ActionListener {

	GridBagConstraints gc;
	JPanel gameListPane, playersPane, buttonPane, optionsPane;
	JButton newButton, loadButton, quitButton, optionButton;
	
	JComboBox gameNameBox = new JComboBox();
	JComboBox[] playerBoxes = new JComboBox[Player.MAX_PLAYERS];
	JTextField[] playerNameFields = new JTextField[Player.MAX_PLAYERS];

	GameUIManager gameUIManager;

	Map<String, String> gameNotes;
	Map<String, String> gameDescs;
	Map<String, String> selectedOptions = new HashMap<String, String>();

	List<String> playerNames = new ArrayList<String>();
	List<JComponent> optionComponents = new ArrayList<JComponent>();
	List<GameOption> availableOptions = new ArrayList<GameOption>();

	String gameName;

	protected static Logger log = Logger.getLogger(GameSetupWindow.class
			.getPackage().getName());

	public GameSetupWindow(GameUIManager gameUIManager) {
		super();

		this.gameUIManager = gameUIManager;

		initialize();
		populateGridBag();

		this.pack();
		this.setVisible(true);
	}

	private void initialize() {
		gameListPane = new JPanel();
		playersPane = new JPanel();
		buttonPane = new JPanel();
		optionsPane = new JPanel();

		newButton = new JButton(LocalText.getText("NewGame"));
		loadButton = new JButton(LocalText.getText("LoadGame"));
		quitButton = new JButton(LocalText.getText("QUIT"));
		optionButton = new JButton(LocalText.getText("OPTIONS"));

		newButton.setMnemonic(KeyEvent.VK_N);
		loadButton.setMnemonic(KeyEvent.VK_L);
		quitButton.setMnemonic(KeyEvent.VK_Q);
		optionButton.setMnemonic(KeyEvent.VK_O);

		this.getContentPane().setLayout(new GridBagLayout());
		this.setTitle("Rails: New Game");
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		populateGameList(GamesInfo.getGameNames(), gameNameBox);

		gameListPane.add(new JLabel("Available Games:"));
		gameListPane.add(gameNameBox);
		gameListPane.setLayout(new GridLayout(2, 2));
		gameListPane.setBorder(BorderFactory.createLoweredBevelBorder());

		newButton.addActionListener(this);
		loadButton.addActionListener(this);
		quitButton.addActionListener(this);
		optionButton.addActionListener(this);
		gameNameBox.addActionListener(this);

		buttonPane.add(newButton);
		buttonPane.add(loadButton);
		buttonPane.add(optionButton);
		buttonPane.add(quitButton);
		buttonPane.setBorder(BorderFactory.createLoweredBevelBorder());

		optionsPane.setLayout(new FlowLayout());
		optionsPane.setVisible(false);

		// Assure that these values are sensibly defaulted;
		gameName = gameNameBox.getSelectedItem().toString().split(" ")[0];
		availableOptions = GamesInfo.getOptions(gameName);

		// This needs to happen after we have a valid gameName.
		fillPlayersPane();
	}

	private void populateGridBag() {
		gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 0;
		gc.weighty = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.ipadx = 0;		
		gc.ipady = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.insets = new Insets(0,0,0,0);
		this.getContentPane().add(playersPane, gc);

		gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 1;
		gc.weightx = 0;
		gc.weighty = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.ipadx = 0;
		gc.ipady = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.fill = GridBagConstraints.BOTH;
		gc.insets = new Insets(0,0,0,0);
		this.getContentPane().add(gameListPane, gc);

		gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 2;
		gc.weightx = 0;
		gc.weighty = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.ipadx = 0;
		gc.ipady = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.fill = GridBagConstraints.BOTH;
		gc.insets = new Insets(0,0,0,0);
		this.getContentPane().add(optionsPane, gc);

		gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 3;
		gc.weightx = 0;
		gc.weighty = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.ipadx = 0;
		gc.ipady = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.insets = new Insets(0,0,0,0);
		this.getContentPane().add(buttonPane, gc);
	}

	private void populateGameList(List<String> gameNames, JComboBox gameNameBox) {
		for (String gameName : gameNames) {
			String gameText = gameName + " - " + GamesInfo.getNote(gameName);
			gameNameBox.addItem(gameText);
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource().equals(newButton)) {
			startNewGame();
		} else if (arg0.getSource().equals(optionButton)) {
			toggleOptions();
			this.pack();
		} else if (arg0.getSource().equals(loadButton)
				&& gameUIManager.loadGame()) {
			setVisible(false);
		} else if (arg0.getSource().equals(quitButton)) {
			System.exit(0);
		} else if (arg0.getSource().equals(gameNameBox)) {
			//Game has changed, update the name variable.
			gameName = gameNameBox.getSelectedItem().toString().split(" ")[0];
			
			fillPlayersPane();
			
			if (optionsPane.isVisible()) {
				// XXX: Kludgy and slightly inefficient.
				toggleOptions();
				toggleOptions();
			}
			
			this.pack();
		}
	}

	private void toggleOptions() {
		if (optionsPane.isVisible()) {
			optionsPane.setVisible(false);
			optionsPane.removeAll();
			optionComponents.clear();
		} else {
			availableOptions = GamesInfo.getOptions(gameName);
			
			if (availableOptions != null && !availableOptions.isEmpty()) {
				optionsPane.setLayout(new GridLayout((availableOptions.size()),1));
				
				for (GameOption option : availableOptions) {
					if (option.isBoolean()) {
						JCheckBox checkbox = new JCheckBox(LocalText
								.getText(option.getName()));
						if (option.getDefaultValue().equalsIgnoreCase("yes")) {
							checkbox.setSelected(true);
						}
						optionsPane.add(checkbox);
						optionComponents.add(checkbox);
					} else {
						optionsPane.setLayout(new GridLayout((availableOptions.size()+1),1));
						optionsPane.add(new JLabel(LocalText.getText("Select",
								LocalText.getText(option.getName()))));
						JComboBox dropdown = new JComboBox();
						for (String value : option.getAllowedValues()) {
							dropdown.addItem(value);
						}
						optionsPane.add(dropdown);
						optionComponents.add(dropdown);
					}
				}
			} else {
				JLabel label = new JLabel(LocalText.getText("NoGameOptions"));
				optionsPane.add(label);
			}

			optionsPane.setVisible(true);
		}
	}

	private void startNewGame() {
		try {

			for (int i = 0; i < playerBoxes.length; i++) {
				if (playerBoxes[i].getSelectedItem().toString()
						.equalsIgnoreCase("Human")
						&& !playerNameFields[i].getText().equals("")) {
					playerNames.add(playerNameFields[i].getText());
				}
			}

			if (playerNames.size() < Player.MIN_PLAYERS
					|| playerNames.size() > Player.MAX_PLAYERS) {
				if (JOptionPane.showConfirmDialog(this,
						"Not enough players. Continue Anyway?",
						"Are you sure?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
					return;
				}
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					"Unable to load selected rails.game. Exiting...");
			System.exit(-1);
		}

		if (optionsPane.isVisible()) {
			GameOption option;
			JCheckBox checkbox;
			JComboBox dropdown;
			String value;

			for (int i = 0; i < availableOptions.size(); i++) {
				option = availableOptions.get(i);
				if (option.isBoolean()) {
					checkbox = (JCheckBox) optionComponents.get(i);
					value = checkbox.isSelected() ? "yes" : "no";
				} else {
					dropdown = (JComboBox) optionComponents.get(i);
					value = (String) dropdown.getSelectedItem();
				}
				selectedOptions.put(option.getName(), value);
				log
						.info("Game option " + option.getName() + " set to "
								+ value);
			}
		} else {
			// No options selected: take the defaults
			GameOption option;
			String value;

			for (int i = 0; i < availableOptions.size(); i++) {
				option = availableOptions.get(i);
				value = option.getDefaultValue();
				selectedOptions.put(option.getName(), value);
				log
						.info("Game option " + option.getName() + " set to "
								+ value);
			}
		}

		Game game = new Game(gameName, playerNames, selectedOptions);
		if (!game.setup()) {
			JOptionPane.showMessageDialog(this, DisplayBuffer.get(), "",
					JOptionPane.ERROR_MESSAGE);

			// Should want to return false and continue,
			// but as of now the game engine cannot be restarted
			// once we have passed setup(), so we can only quit.
			System.exit(-1);
		} else {
			game.start();
			gameUIManager.gameUIInit();
		}

		this.setVisible(false); // XXX: At some point we should destroy this
		// XXX: object rather than just making it invisible
	}

	private void fillPlayersPane() {
		playersPane.setVisible(false);
		
		int maxPlayers = GamesInfo.getMaxPlayers(gameName);

		String[] playerList = new String[maxPlayers];
		String[] testPlayers = Config.get("default_players").split(",");
		
		//Remember names that have already been filled-in...
		for (int i = 0; i < playerNameFields.length; i++) {
			if (playerNameFields[i] != null && playerNameFields[i].getText().length() > 0) {
				playerList[i] = playerNameFields[i].getText();
			} else if (i < testPlayers.length && testPlayers[i].length() > 0) {
				playerList[i] = testPlayers[i];
			}
		}
		
		playersPane.removeAll();

		playersPane.setLayout(new GridLayout(maxPlayers + 1, 0));
		playersPane.setBorder(BorderFactory.createLoweredBevelBorder());
		playersPane.add(new JLabel("Players:"));
		playersPane.add(new JLabel(""));
		
		for (int i = 0; i < GamesInfo.getMaxPlayers(gameName); i++) {
			playerBoxes[i] = new JComboBox();
			playerBoxes[i].addItem("None");
			playerBoxes[i].addItem("Human");
			
			/*
			 * Prefill with any configured player names. This can be useful to speed
			 * up testing purposes.
			 */
			if (testPlayers.length > 0 && i < playerList.length) {
				playerNameFields[i] = new JTextField(playerList[i]);
			} else {
				playerNameFields[i] = new JTextField();
			}
			
			if (playerNameFields[i].getText().length() > 0) { 
				playerBoxes[i].setSelectedIndex(1);
			} else {
				playerBoxes[i].setSelectedIndex(0);
			}
			
			playersPane.add(playerBoxes[i]);
			playersPane.add(playerNameFields[i]);
		}
		playersPane.setVisible(true);
	}
}
