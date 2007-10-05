/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ORWindow.java,v 1.9 2007/10/05 22:02:29 evos Exp $*/
package rails.ui.swing;

import rails.game.*;
import rails.game.action.LayTile;
import rails.game.action.LayToken;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import rails.game.special.*;
import rails.ui.swing.hexmap.*;
import rails.util.LocalText;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
	private MapPanel mapPanel;
	private ORPanel orPanel;
	private UpgradesPanel upgradePanel;
	private MessagePanel messagePanel;

	/* Substeps in tile and token laying */
	public static final int INACTIVE = 0;
	public static final int SELECT_HEX_FOR_TILE = 1;
	public static final int SELECT_TILE = 2;
	public static final int ROTATE_OR_CONFIRM_TILE = 3;
	public static final int SELECT_HEX_FOR_TOKEN = 4;
	public static final int CONFIRM_TOKEN = 5;

	/* Message key per substep */
	protected static final String[] messageKey = new String[] { "",
			"SelectAHexForTile", "SelectATile", "RotateTile",
			"SelectAHexForToken", "ConfirmToken" };

	protected static int subStep = INACTIVE;
	public static boolean baseTokenLayingEnabled = false;
	/**
	 * Is tile laying enabled? If not, one can play with tiles, but the "Done"
	 * button is disabled.
	 */
	public static boolean tileLayingEnabled = false;
	protected PossibleActions possibleActions = PossibleActions.getInstance();

	protected static Logger log = Logger.getLogger(ORWindow.class.getPackage().getName());

	public ORWindow(GameUIManager gameUIManager)
	{
		super();
        this.gameUIManager = gameUIManager;
        
		getContentPane().setLayout(new BorderLayout());

		messagePanel = new MessagePanel();
		getContentPane().add(messagePanel, BorderLayout.NORTH);

		if (mapPanel == null)
			mapPanel = new MapPanel();
		else
			mapPanel = GameUIManager.getMapPanel();
		getContentPane().add(mapPanel, BorderLayout.CENTER);

		upgradePanel = new UpgradesPanel(this);
		getContentPane().add(upgradePanel, BorderLayout.WEST);
		addMouseListener(upgradePanel);

		orPanel = new ORPanel(this);
		getContentPane().add(orPanel, BorderLayout.SOUTH);

		setTitle("Rails: Map");
		setLocation(10, 10);
		setVisible(true);
		setSize(800, 600);
		addWindowListener(this);

		ReportWindow.addLog();
	}

	public void setMessage(String messageKey)
	{
		messagePanel.setMessage(messageKey);
	}

	public MapPanel getMapPanel()
	{
		return mapPanel;
	}

	public ORPanel getORPanel()
	{
		return orPanel;
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

	public int getSubStep()
	{
		return subStep;
	}

	public void setSubStep(int subStep)
	{
	    log.debug ("Setting substep to "+subStep);
		ORWindow.subStep = subStep;
		
		updateMessage();
		updateUpgradesPanel();
	}
	
	public void updateMessage() {
	    
	    // For now, this only has an effect during tile and token laying.
	    // Perhaps we need to centralise message updating here in a later stage.
	    log.debug ("Calling updateMessage, subStep="+subStep/*, new Exception("TRACE")*/);
	    if (subStep == INACTIVE) return;
	    
	    String message = LocalText.getText(messageKey[subStep]);
	    SpecialProperty sp;
	    
	    /* Add any extra messages */
	    String extraMessage = "";
	    if (subStep == SELECT_HEX_FOR_TILE) {
		    /* Compose prompt for tile laying */
		    LayTile tileLay;
		    StringBuffer normalTileMessage = new StringBuffer(" ");
		    StringBuffer extraTileMessage = new StringBuffer(" ");
		    
		    List tileLays = possibleActions.getType(LayTile.class);
		    log.debug ("There are "+tileLays.size()+" TileLay objects");
		    int ii=0;
		    for (Iterator it = tileLays.iterator(); it.hasNext(); ) {
		        Map tileColours;
		        MapHex hex;
		        tileLay = (LayTile) it.next();
		        log.debug ("TileLay object "+(++ii)+": "+tileLay);
		        sp = tileLay.getSpecialProperty();
		        /* A LayTile object contais either:
		         * 1. a special property (specifying a location)
		         * 2. a location (perhaps a list of?) where a specified
		         * set of tiles may be laid, or
		         * 3. a map specifying how many tiles of any colour may be laid "anywhere".
		         * The last option is only a stopgap as we can't yet determine connectivity.  
		         */
		        if (sp != null && sp instanceof SpecialTileLay) {
		            hex = ((SpecialTileLay)sp).getLocation();
		            if (extraTileMessage.length() > 1) extraTileMessage.append(", ");
		            extraTileMessage.append (hex.getName())
		            	.append(" (") 
		            	.append(((SpecialTileLay)sp).isExtra() ? "" : "not ")
		            	.append("extra");
		            if (hex.getTileCost() > 0) {
		                extraTileMessage.append (", ")
		                	.append(((SpecialTileLay)sp).isFree()?"":"not ")
			            	.append(" free");
		            }
		            extraTileMessage.append(")");
		        } else if ((tileColours = tileLay.getTileColours()) != null) {
		            String colour;
		            int number;
		            for (Iterator it2 = tileColours.keySet().iterator(); it2.hasNext(); ) {
		                colour = (String) it2.next();
		                number = ((Integer)tileColours.get(colour)).intValue();
		                if (normalTileMessage.length() > 1) {
		                    normalTileMessage.append(" ")
		                    	.append(LocalText.getText("OR"))
		                    	        .append(" ");
		                }
		                normalTileMessage.append(number).append(" ").append(colour);
		            }
		        }
		    }
		    if (extraTileMessage.length() > 1) {
		        extraMessage += LocalText.getText("ExtraTile", extraTileMessage);
		    }
	        if (normalTileMessage.length() > 1) {
	            message += " "+LocalText.getText("TileColours", normalTileMessage);
	        }
	        
	    } else if (subStep == SELECT_HEX_FOR_TOKEN) {
	        
		    /* Compose prompt for token laying */
		    LayToken tokenLay;
		    MapHex location;
		    StringBuffer normalTokenMessage = new StringBuffer(" ");
		    StringBuffer extraTokenMessage = new StringBuffer(" ");
		    
		    List tokenLays = possibleActions.getType(LayToken.class);
		    log.debug ("There are "+tokenLays.size()+" TokenLay objects");
		    int ii=0;
		    for (Iterator it = tokenLays.iterator(); it.hasNext(); ) {

		        tokenLay = (LayToken) it.next();
		        log.debug ("TokenLay object "+(++ii)+": "+tokenLay);
		        sp = tokenLay.getSpecialProperty();
		        /* A LayToken object contais either:
		         * 1. a special property (specifying a location)
		         * 2. a location (perhaps a list of?) where a token of a specified
		         * company (the currently operating one) may be laid, or
		         * 3. null location and the currently operating company.
		         * The last option is only a stopgap as we can't yet determine connectivity.  
		         */
		        if (sp != null && sp instanceof SpecialTokenLay) {
		            if (extraTokenMessage.length() > 1) extraTokenMessage.append(", ");
		            extraTokenMessage.append (((SpecialTokenLay)sp).getLocation().getName())
	            	.append(" (") 
	            	.append(((SpecialTokenLay)sp).isExtra() ? "" : "not ")
	            	.append("extra, ")
	            	.append(((SpecialTokenLay)sp).isFree()?"":"not ")
	            	.append("free)");
			    } else if ((location = tokenLay.getLocation()) != null) {
	                if (normalTokenMessage.length() > 1) {
	                    normalTokenMessage.append(" ")
	                    	.append(LocalText.getText("OR"))
	                    	        .append(" ");
	                }
	                normalTokenMessage.append(location.getName());
		        }
		    }
		    if (extraTokenMessage.length() > 1) {
		        extraMessage += LocalText.getText("ExtraToken", extraTokenMessage);
		    }
	        if (normalTokenMessage.length() > 1) {
	            message += " "+LocalText.getText("NormalToken", normalTokenMessage);
	        }
	    }
	    if (extraMessage.length() > 0) {
	        message += "<br><font color=\"red\">" + extraMessage + "</font>";
	    }

		setMessage(message);
		
	}
	
	private void updateUpgradesPanel() {

		if (upgradePanel != null)
		{
			upgradePanel.setUpgrades(null);

			switch (subStep)
			{
				case INACTIVE:
					log.debug ("subStep = Inactive");
					upgradePanel.setDoneEnabled(false);
					upgradePanel.setCancelEnabled(false);
					break;
				case SELECT_HEX_FOR_TILE:
					log.debug ("subStep = Select hex for tile");
					upgradePanel.setDoneText("LayTile");
					upgradePanel.setCancelText("NoTile");
					upgradePanel.setDoneEnabled(false);
					upgradePanel.setCancelEnabled(true);
					break;
				case SELECT_TILE:
					log.debug ("subStep = Select Tile");
					if (tileLayingEnabled)
						upgradePanel.populate();
					upgradePanel.setDoneEnabled(false);
					break;
				case ROTATE_OR_CONFIRM_TILE:
					log.debug ("subStep = Rotate or Confirm Tile");
					upgradePanel.setDoneEnabled(true);
					break;
				case SELECT_HEX_FOR_TOKEN:
					log.debug ("subStep = Select hex for token");
					upgradePanel.setDoneEnabled(false);
					upgradePanel.setCancelEnabled(true);
					upgradePanel.setDoneText("LayToken");
					upgradePanel.setCancelText("NoToken");
					break;
				case CONFIRM_TOKEN:
				    log.debug ("subStep = Confirm Token");
					PublicCompany co = (PublicCompany) orPanel.getOperatingCompanies()[orPanel.getOrCompIndex()];
					
					if(co.getNumberOfFreeBaseTokens() > 0)
						upgradePanel.setDoneEnabled(true);
					else
						upgradePanel.setDoneEnabled(false);
					
					break;
				default:
				    log.debug ("subStep = default");
					upgradePanel.setDoneEnabled(false);
					upgradePanel.setCancelEnabled(false);
				break;
			}
		}

	}

	public void processDone()
	{
		if (baseTokenLayingEnabled)
		{
            layBaseToken();
		}
		else if (tileLayingEnabled)
		{
            layTile();
		}

		repaintUpgradePanel();
	}
    
    private void layTile () {
        
        HexMap map = mapPanel.getMap();
        GUIHex selectedHex = map.getSelectedHex();
        
        if (selectedHex != null && selectedHex.canFixTile())
        {
            LayTile allowance = map.getTileAllowanceForHex(selectedHex.getHexModel());
            allowance.setChosenHex(selectedHex.getHexModel());
            allowance.setOrientation(selectedHex.getProvisionalTileRotation());
            allowance.setLaidTile(selectedHex.getProvisionalTile());
            
            if (process(allowance)) {
                selectedHex.fixTile();
                updateStatus();
            } else {
                selectedHex.removeTile();
                setSubStep (SELECT_HEX_FOR_TILE);
            }
            map.selectHex(null);
        }
    }
    
    private void layBaseToken () {
        
        HexMap map = mapPanel.getMap();
        GUIHex selectedHex = map.getSelectedHex();
        
        if (selectedHex != null)
        {
            LayToken allowance = map.getTokenAllowanceForHex(selectedHex.getHexModel());
            int station;
            List<Station> stations = selectedHex.getHexModel().getStations();
            
            switch (stations.size()) {
            case 0: // No stations
                return;
                
            case 1:
                station = 0;
                break;
                
            default:
                Station stationObject = (Station) JOptionPane.showInputDialog(this,
                        "Which station to place the token in?",
                        "Which station?",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        stations.toArray(),
                        stations.get(0));
                station = stations.indexOf(stationObject);
            }
            
            allowance.setChosenHex(selectedHex.getHexModel());
            allowance.setChosenStation(station);

            if  (process(allowance)) {
                selectedHex.fixToken();
                updateStatus();
                enableBaseTokenLaying(false);
            } else {
                setSubStep (SELECT_HEX_FOR_TOKEN);
            }
        }
    }

	public void processCancel()
	{
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();
		setSubStep(INACTIVE);
		if (baseTokenLayingEnabled)
		{
			if (selectedHex != null)
				selectedHex.removeToken();
			//orPanel.layBaseToken(null, 0);
            process (new NullAction (NullAction.SKIP));
            enableBaseTokenLaying(false);
		}
		else if (tileLayingEnabled)
		{
			if (selectedHex != null)
				selectedHex.removeTile();
			process (new NullAction (NullAction.SKIP));
		}

        //updateStatus();
		repaintUpgradePanel();
	}
    
    public boolean process (PossibleAction action) {

        // Add the actor for safety checking in the server 
        action.setPlayerName(orPanel.getORPlayer());
        // Process the action
        boolean result = gameUIManager.processOnServer (action);
        // Display any error message
        displayMessage();
        
        return result;
    }
    
    
    // Not yet used
    public boolean processImmediateAction () {
        return true;
    }

    public void displayMessage() {
        String[] message = DisplayBuffer.get();
        if (message != null) {
            JOptionPane.showMessageDialog(this, message);
        }
    }


	public void enableTileLaying(boolean enabled)
	{
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();

		if (enabled)
		{
			/* Start tile laying step */
			setSubStep(SELECT_HEX_FOR_TILE);
		}
		else if (tileLayingEnabled && !enabled)
		{
			/* Finish tile laying step */
			if (selectedHex != null)
			{
				selectedHex.removeTile();
				selectedHex.setSelected(false);
				mapPanel.getMap().repaint(selectedHex.getBounds());
				selectedHex = null;
			}
			setSubStep(INACTIVE);
		}
		tileLayingEnabled = enabled;
		baseTokenLayingEnabled = false;
		upgradePanel.setTileMode(enabled);
	}

	public void enableBaseTokenLaying(boolean enabled)
	{
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();

		if (!baseTokenLayingEnabled && enabled)
		{
			/* Start token laying step */
			setSubStep(SELECT_HEX_FOR_TOKEN);
		}
		else if (baseTokenLayingEnabled && !enabled)
		{
			/* Finish token laying step */
			if (selectedHex != null)
			{
				selectedHex.removeToken();
				selectedHex.setSelected(false);
				mapPanel.getMap().repaint(selectedHex.getBounds());
				selectedHex = null;
			}
			setSubStep(INACTIVE);
		}
		baseTokenLayingEnabled = enabled;
		tileLayingEnabled = false;
		upgradePanel.setTileMode(enabled);
	}

	public void repaintUpgradePanel()
	{
		upgradePanel.setVisible(false);	
		upgradePanel.setVisible(true);
	}

	public void repaintORPanel()
	{
		orPanel.revalidate();
	}

	public void activate()
	{
		repaintUpgradePanel();
		orPanel.recreate();
		setVisible(true);
		requestFocus();
	}

	public void updateORWindow()
	{
		if (GameManager.getInstance().getCurrentRound() instanceof StockRound)
		{
			//GameUIManager.statusWindow.updateStatus("ORWindow.updateORWindow");
		}
		else if (GameManager.getInstance().getCurrentRound() instanceof OperatingRound)
		{
			updateStatus();

		}
	}

	public void updateStatus()
	{
		orPanel.updateStatus();
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
