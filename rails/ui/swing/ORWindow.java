/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ORWindow.java,v 1.13 2008/01/18 19:58:15 evos Exp $*/
package rails.ui.swing;

import rails.game.*;
import rails.game.action.LayTile;
import rails.game.action.LayToken;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import rails.util.LocalText;

import java.util.ArrayList;
import java.util.List;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.apache.log4j.Logger;


/**
 * This Window displays the available operations that may be performed during an
 * Operating Round. This window also contains the Game Map.
 */
public class ORWindow extends JFrame implements WindowListener, ActionPerformer
{
    private GameUIManager gameUIManager;
    private ORUIManager orUIManager;
	private MapPanel mapPanel;
	private ORPanel orPanel;
	private UpgradesPanel upgradePanel;
	private MessagePanel messagePanel;

	protected PossibleActions possibleActions = PossibleActions.getInstance();
	
	List<LayTile> allowedTileLays = new ArrayList<LayTile>();
	List<LayToken> allowedTokenLays = new ArrayList<LayToken>();
	
	protected static Logger log = Logger.getLogger(ORWindow.class.getPackage().getName());

	public ORWindow(GameUIManager gameUIManager)
	{
		super();
        this.gameUIManager = gameUIManager;
        
        Class<? extends ORUIManager> orUIManagerClass = gameUIManager.getGameManager().getORUIManagerClass();
        try {
        	orUIManager = orUIManagerClass.newInstance();
        } catch (Exception e) {
        	log.fatal("Cannot instantiate class "+orUIManagerClass.getName());
        	System.exit(1);
        }
        
		getContentPane().setLayout(new BorderLayout());

		messagePanel = new MessagePanel();
		getContentPane().add(messagePanel, BorderLayout.NORTH);

		mapPanel = new MapPanel(orUIManager);
		getContentPane().add(mapPanel, BorderLayout.CENTER);

		upgradePanel = new UpgradesPanel(orUIManager);
		getContentPane().add(upgradePanel, BorderLayout.WEST);
		addMouseListener(upgradePanel);

		orPanel = new ORPanel(this, orUIManager);
		getContentPane().add(orPanel, BorderLayout.SOUTH);
		
        orUIManager.init(this);
        
		setTitle("Rails: Map");
		setLocation(10, 10);
		setVisible(false);
		setSize(800, 600);
		addWindowListener(this);

		ReportWindow.addLog();
	}

    public ORUIManager getORUIManager() {
        return orUIManager;
    }
    
    public GameUIManager getGameUIManager() {
        return gameUIManager;
    }

	public MapPanel getMapPanel()
	{
		return mapPanel;
	}

	public ORPanel getORPanel()
	{
		return orPanel;
	}

	public UpgradesPanel getUpgradePanel() {
		return upgradePanel;
	}
	
	public ORUIManager getOrUIManager() {
		return orUIManager;
	}
	
	public MessagePanel getMessagePanel () {
		return messagePanel;
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		StatusWindow.uncheckMenuItemBox(LocalText.getText("MAP"));
		dispose();
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowOpened(WindowEvent e)
	{
	}

    public boolean process (PossibleAction action) {

        // Add the actor for safety checking in the server 
        action.setPlayerName(orPanel.getORPlayer());
        // Process the action
        boolean result = gameUIManager.processOnServer (action);
        // Display any error message
        displayServerMessage();
        
        return result;
    }
    
    
    // Not yet used
    public boolean processImmediateAction () {
        return true;
    }

    public void displayServerMessage() {
        String[] message = DisplayBuffer.get();
        if (message != null) {
            JOptionPane.showMessageDialog(this, message);
        }
    }

    public void displayORUIMessage(String message) {
        if (message != null) {
            JOptionPane.showMessageDialog(this, message);
        }
    }

	public void repaintORPanel()
	{
		orPanel.revalidate();
	}

	public void activate(OperatingRound or)
	{
		orPanel.recreate(or);
		pack();
		setVisible(true);
		requestFocus();
	}

	public void updateStatus()
	{
		orUIManager.updateStatus();
		requestFocus();
	}
	
	/**
	 * Round-end settings
	 * 
	 */
	public void finish()
	{
		orPanel.finish();
		upgradePanel.finish();
	}
}
