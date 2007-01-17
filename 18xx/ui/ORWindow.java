package ui;

import game.*;
import game.action.LayTile;
import game.action.LayToken;
import game.action.PossibleActions;
import game.special.*;
import ui.hexmap.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.apache.log4j.Logger;

import util.LocalText;

/**
 * This Window displays the available operations that may be performed during an
 * Operating Round. This window also contains the Game Map.
 */
public class ORWindow extends JFrame implements WindowListener
{

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

	public ORWindow()
	{
		super();
		getContentPane().setLayout(new BorderLayout());

		messagePanel = new MessagePanel();
		getContentPane().add(messagePanel, BorderLayout.NORTH);

		if (mapPanel == null)
			mapPanel = new MapPanel();
		else
			mapPanel = GameUILoader.getMapPanel();
		getContentPane().add(mapPanel, BorderLayout.CENTER);

		upgradePanel = new UpgradesPanel();
		getContentPane().add(upgradePanel, BorderLayout.WEST);
		addMouseListener(upgradePanel);

		orPanel = new ORPanel();
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
	    log.debug ("Calling updateMessage, subStep="+subStep);
	    if (subStep == INACTIVE) return;
	    
	    String message = LocalText.getText(messageKey[subStep]);
	    //List specialProperties = 
	    //    ((OperatingRound)GameManager.getInstance().getCurrentRound()).getSpecialProperties();
	    SpecialORProperty sp;
	    
	    /* Add any extra messages */
	    String extraMessage = "";
	    if (subStep == SELECT_HEX_FOR_TILE) {
		    /* Compose prompt for tile laying */
		    LayTile tileLay;
		    StringBuffer normalTileMessage = new StringBuffer(" ");
		    StringBuffer extraTileMessage = new StringBuffer(" ");
		    
		    List tileLays = possibleActions.get(LayTile.class);
		    log.debug ("There are "+tileLays.size()+" TileLay objects");
		    int ii=0;
		    for (Iterator it = tileLays.iterator(); it.hasNext(); ) {
		        Map tileColours;
		        MapHex hex;
		        //sp = (SpecialORProperty) it.next();
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
	        
		    /* Compose prompt for tile laying */
		    LayToken tokenLay;
		    MapHex location;
		    StringBuffer normalTokenMessage = new StringBuffer(" ");
		    StringBuffer extraTokenMessage = new StringBuffer(" ");
		    
		    List tokenLays = possibleActions.get(LayToken.class);
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
	        //message += " <font color=\"red\">" + extraMessage + "</font>";
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
		HexMap map = mapPanel.getMap();
		GUIHex selectedHex = map.getSelectedHex();
		if (baseTokenLayingEnabled)
		{
			if (selectedHex != null)
			{
			    LayToken allowance = map.getTokenAllowanceForHex(selectedHex.getHexModel());
				if (selectedHex.getHexModel().getStations().size() == 1)
				{
					if (selectedHex.fixToken(0, allowance)) {
						//setSubStep(INACTIVE);
					} else {
					    setSubStep (SELECT_HEX_FOR_TOKEN);
					}
					map.selectHex(null);
				}
				else
				{
					Object[] stations = selectedHex.getHexModel()
							.getStations()
							.toArray();
					Station station = (Station) JOptionPane.showInputDialog(this,
							"Which station to place the token in?",
							"Which station?",
							JOptionPane.PLAIN_MESSAGE,
							null,
							stations,
							stations[0]);

					try
					{
						if  (selectedHex.fixToken(selectedHex.getHexModel()
								.getStations()
								.indexOf(station), allowance)) {
						} else {
						    setSubStep (SELECT_HEX_FOR_TOKEN);
						}
					}
					catch(ArrayIndexOutOfBoundsException e)
					{
						//Clicked on a hex that doesn't have a tile or a station in it.
					    DisplayBuffer.add(LocalText.getText("NoStationNoToken"));
					}
				}
			}
		}
		else
		{
			if (selectedHex != null)
			{
			    LayTile allowance = map.getTileAllowanceForHex(selectedHex.getHexModel());
				if (!selectedHex.fixTile(tileLayingEnabled, allowance)) {
				    selectedHex.removeTile();
				    setSubStep (SELECT_HEX_FOR_TILE);
				} else {
					//setSubStep(INACTIVE);
				}
				map.selectHex(null);
			}
		}

		repaintUpgradePanel();
	}

	public void processCancel()
	{
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();
		setSubStep(INACTIVE);
		if (baseTokenLayingEnabled)
		{
			if (selectedHex != null)
				selectedHex.removeToken();
			orPanel.layBaseToken(null, 0);
		}
		else
		{
			if (selectedHex != null)
				selectedHex.removeTile();
			if (tileLayingEnabled)
				orPanel.layTile(null, null, 0);
		}

		repaintUpgradePanel();
	}

	public void enableTileLaying(boolean enabled)
	{
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();

		if (/*!tileLayingEnabled &&*/ enabled)
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
		orPanel.updateStatus();
		setVisible(true);
		requestFocus();
	}

	public static void updateORWindow()
	{
		if (GameManager.getInstance().getCurrentRound() instanceof StockRound)
		{
			GameUILoader.statusWindow.updateStatus();
		}
		else if (GameManager.getInstance().getCurrentRound() instanceof OperatingRound)
		{
			GameUILoader.orWindow.updateStatus();

		}
	}

	public void updateStatus()
	{
		orPanel.updateStatus();
	}

	/**
	 * Game-end settings
	 * 
	 */
	public void finish()
	{
		orPanel.finish();
		upgradePanel.finish();
	}
}
