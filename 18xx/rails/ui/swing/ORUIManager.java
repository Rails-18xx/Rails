package rails.ui.swing;

import java.util.*;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.game.action.*;
import rails.game.special.*;
import rails.ui.swing.hexmap.GUIHex;
import rails.ui.swing.hexmap.HexMap;
import rails.util.LocalText;
import rails.util.Util;

public class ORUIManager {
	
	private GameUIManager gameUIManager;
	private ORWindow orWindow;
	private ORPanel orPanel;
	private UpgradesPanel upgradePanel;
	private MapPanel mapPanel;
	private HexMap map;
	private MessagePanel messagePanel;
	
    private OperatingRound oRound;
    private PublicCompanyI[] companies;
    private PublicCompanyI orComp;
    private int orCompIndex;
    private int playerIndex;
    
    private boolean retrieveStep;
	private int orStep;
	private int localStep;

    protected PossibleActions possibleActions = PossibleActions.getInstance();
    private boolean privatesCanBeBought;
    private boolean privatesCanBeBoughtNow;
    private boolean bonusTokensExist;
    public List<PossibleAction> mapRelatedActions = new ArrayList<PossibleAction>();

    private boolean tileLayingEnabled = false; 
	public List<LayTile> allowedTileLays = new ArrayList<LayTile>();
	private int selectedTileId;
	public List<TileI> tileUpgrades;
	
	private boolean tokenLayingEnabled = false;
	public List<LayToken> allowedTokenLays = new ArrayList<LayToken>();
	private int selectedTokenIndex;
	private LayToken selectedTokenAllowance;
	
	/** Will be set true if a cancelled action does not need to be reported to the server,
	 * because it does not change the OR turn step.
	 * For instance, if a bonus token lay is locally initiated but cancelled. */ 
	protected boolean localAction = false;

	/* Local substeps */
	public static final int INACTIVE = 0;
	public static final int SELECT_HEX_FOR_TILE = 1;
	public static final int SELECT_TILE = 2;
	public static final int ROTATE_OR_CONFIRM_TILE = 3;
	public static final int SELECT_HEX_FOR_TOKEN = 4;
	public static final int SELECT_TOKEN = 5;
	public static final int CONFIRM_TOKEN = 6;
	public static final int SET_REVENUE = 7;
	public static final int SELECT_PAYOUT = 8;

	/* Message key per substep */
	protected static final String[] messageKey = new String[] { "Inactive",
			"SelectAHexForTile", "SelectATile", "RotateTile",
			"SelectAHexForToken", "SelectAToken", "ConfirmToken",
			"SetRevenue", "SelectPayout"};

	protected static Logger log = Logger.getLogger(ORUIManager.class.getPackage().getName());

	public ORUIManager (ORWindow orWindow) {
		
		this.orWindow = orWindow; 
        gameUIManager = orWindow.getGameUIManager();
    }
    
    public void init() {
        
	    orPanel = orWindow.getORPanel();
	    mapPanel = orWindow.getMapPanel();
	    upgradePanel = orWindow.getUpgradePanel();
	    map = mapPanel.getMap();
	    messagePanel = orWindow.getMessagePanel();
	    
        privatesCanBeBought = GameManager.getCompaniesCanBuyPrivates();
        bonusTokensExist = GameManager.doBonusTokensExist();
	}
	
	public void initOR (OperatingRound or) {
		oRound = or;
        companies = (oRound).getOperatingCompanies();
		orWindow.activate(oRound);
	}
	
	public void finish() {
        orWindow.finish();
        orWindow.setVisible(false);

	}
	
	public <T extends PossibleAction> void setMapRelatedActions (List<T> actions) {
		
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();
		int nextSubStep = ORUIManager.INACTIVE;
		
		allowedTileLays.clear();
		allowedTokenLays.clear();
		for (T action : actions) {
			if (action instanceof LayTile) {
				allowedTileLays.add ((LayTile) action);
			} else if (action instanceof LayToken) {
				allowedTokenLays.add((LayToken) action);
			}
		}
		
		if (allowedTileLays.size() > 0) {
			nextSubStep = ORUIManager.SELECT_HEX_FOR_TILE;
			mapPanel.setAllowedTileLays(allowedTileLays);
		} else {
			if (tileLayingEnabled) {
				/* Finish tile laying step */
				if (selectedHex != null)
				{
					selectedHex.removeTile();
					selectedHex.setSelected(false);
					mapPanel.getMap().repaint(selectedHex.getBounds());
					selectedHex = null;
				}
			}
		}
			
		if (allowedTokenLays.size() > 0) {
			nextSubStep = ORUIManager.SELECT_HEX_FOR_TOKEN;
			mapPanel.setAllowedTokenLays(allowedTokenLays);
		} else {
			if (tokenLayingEnabled) {
				/* Finish token laying step */
				if (selectedHex != null)
				{
					selectedHex.removeToken();
					selectedHex.setSelected(false);
					mapPanel.getMap().repaint(selectedHex.getBounds());
					selectedHex = null;
				}
			}
		}
		
		setLocalStep (nextSubStep);
		tileLayingEnabled = allowedTileLays.size() > 0;
		tokenLayingEnabled = allowedTokenLays.size() > 0;
		upgradePanel.setTileMode(tileLayingEnabled);
		upgradePanel.setTokenMode(tokenLayingEnabled);

		setLocalAction(false);
	}
	
    public void updateMessage() {
        
        // For now, this only has an effect during tile and token laying.
        // Perhaps we need to centralise message updating here in a later stage.
        log.debug ("Calling updateMessage, subStep="+localStep/*, new Exception("TRACE")*/);
        if (localStep == ORUIManager.INACTIVE) return;
        
        String message = LocalText.getText(ORUIManager.messageKey[localStep]);
        SpecialProperty sp;
        
        /* Add any extra messages */
        String extraMessage = "";
        if (localStep == ORUIManager.SELECT_HEX_FOR_TILE) {
            /* Compose prompt for tile laying */
            LayTile tileLay;
            int tileNumber;
            StringBuffer normalTileMessage = new StringBuffer(" ");
            StringBuffer extraTileMessage = new StringBuffer(" ");
            StringBuffer specialTiles = new StringBuffer("#");
            
            List tileLays = possibleActions.getType(LayTile.class);
            log.debug ("There are "+tileLays.size()+" TileLay objects");
            int ii=0;
            for (Iterator it = tileLays.iterator(); it.hasNext(); ) {
                Map tileColours;
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
                    SpecialTileLay stl = (SpecialTileLay) sp;
                    if (extraTileMessage.length() > 1) extraTileMessage.append(", ");
                    extraTileMessage.append (stl.getLocationNameString())
                        .append(" (") 
                        .append(stl.isExtra() ? "" : "not ")
                        .append("extra");
                    for (MapHex hex : stl.getLocations()) {
                        if (hex.getTileCost() > 0) {
                            extraTileMessage.append (", ")
                                .append(stl.isFree()?"":"not ")
                                .append(" free");
                            break;
                        }
                    }
                    extraTileMessage.append(")");
                    if ((tileNumber = stl.getTileNumber()) > 0) {
                        if (specialTiles.length() > 1) specialTiles.append(", ");
                        specialTiles.append(tileNumber);
                        if (Util.hasValue(stl.getName())) {
                            specialTiles.insert(0, stl.getName() + " ");
                        }
                    }
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
            if (specialTiles.length() > 1) {
                extraMessage += LocalText.getText("SpecialTile", new String[] {
                        specialTiles.toString(), extraTileMessage.toString()});
            } else if (extraTileMessage.length() > 1) {
                extraMessage += LocalText.getText("ExtraTile", extraTileMessage);
            }
            if (normalTileMessage.length() > 1) {
                message += " "+LocalText.getText("TileColours", normalTileMessage);
            }
            
        } else if (localStep == ORUIManager.SELECT_HEX_FOR_TOKEN) {
            
            /* Compose prompt for token laying */
            LayBaseToken tokenLay;
            String locations;
            StringBuffer normalTokenMessage = new StringBuffer(" ");
            StringBuffer extraTokenMessage = new StringBuffer(" ");
            
            List tokenLays = possibleActions.getType(LayBaseToken.class);
            log.debug ("There are "+tokenLays.size()+" TokenLay objects");
            int ii=0;
            for (Iterator it = tokenLays.iterator(); it.hasNext(); ) {

                tokenLay = (LayBaseToken) it.next();
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
                    extraTokenMessage.append (((SpecialTokenLay)sp).getLocationCodeString())
                    .append(" (") 
                    .append(((SpecialTokenLay)sp).isExtra() ? "" : "not ")
                    .append("extra, ")
                    .append(((SpecialTokenLay)sp).isFree()?"":"not ")
                    .append("free)");
                } else if ((locations = tokenLay.getLocationNameString()) != null) {
                    if (normalTokenMessage.length() > 1) {
                        normalTokenMessage.append(" ")
                            .append(LocalText.getText("OR"))
                                    .append(" ");
                    }
                    normalTokenMessage.append(locations);
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
    

    public void processAction (String command, List<PossibleAction> actions) {
    	
    	if (actions != null && actions.size() > 0) {
    		
    		Class actionType = actions.get(0).getClass();
    		
    		if (actionType == SetDividend.class) {
    			
    			setDividend (command, (SetDividend)actions.get(0));
    			
    		} else if (actionType == LayBonusToken.class) {
    			
    			prepareBonusToken ((LayBonusToken) actions.get(0));
    			
    		} else if (actionType == NullAction.class
                || actionType == GameAction.class) {
            
            	orWindow.process (actions.get(0));
    		}

    	} else if (command.equals(ORPanel.BUY_TRAIN_CMD)) {
            
            buyTrain();

        } else if (command.equals(ORPanel.BUY_PRIVATE_CMD)) {

            buyPrivate();
    	}
    	
        ReportWindow.addLog();
    }
    
    private void setDividend (String command, SetDividend action) {
    	
    	
        int amount;

            if (command.equals(ORPanel.SET_REVENUE_CMD)) {
                amount = orPanel.getRevenue(orCompIndex);
                log.debug ("Set revenue amount is "+amount);
                action.setActualRevenue(amount);
                if (action.getRevenueAllocation() != SetDividend.UNKNOWN) {
                    log.debug("Allocation is known: "+action.getRevenueAllocation());
                    orWindow.process (action);
                } else {
                    log.debug("Allocation is unknown, asking for it");
                    setLocalStep (SELECT_PAYOUT);
                    updateStatus(action);
                    
                    // Locally update revenue if we don't inform the server yet.
                    orPanel.setRevenue (orCompIndex, amount);
                }
            } else {
                // The revenue allocation has been selected
                orWindow.process (action);
            }
    }
    
    private void prepareBonusToken (LayBonusToken action) {
    	
        	
        	log.debug("+++ SpecialTokenLay "+action.toString());
            orWindow.requestFocus();

            List<LayToken> actions = new ArrayList<LayToken>();
            actions.add((LayToken) action);
            setMapRelatedActions (actions);
            allowedTokenLays = actions;
            setLocalAction (true);

            log.debug ("BonusTokens can be laid");

            mapPanel.setAllowedTokenLays (actions);

            orPanel.initTokenLayingStep();

    }
 
	
	public void hexClicked (GUIHex clickedHex, GUIHex selectedHex) {
		
		if (tokenLayingEnabled) {
			List<LayToken> allowances = map.getTokenAllowanceForHex(clickedHex.getHexModel());

			if (allowances.size() > 0) {
                log.debug("Hex "+clickedHex.getName()+" clicked, allowances:");
                for (LayToken allowance : allowances) {
                    log.debug (allowance.toString());
                }
				map.selectHex(clickedHex);
				setLocalStep(SELECT_TOKEN);
			} else {
				JOptionPane.showMessageDialog(map, LocalText
						.getText("NoTokenPossible", clickedHex.getName()));
				setLocalStep(ORUIManager.SELECT_HEX_FOR_TOKEN);
			}

		} else if (tileLayingEnabled) {
			if (localStep == ROTATE_OR_CONFIRM_TILE
					&& clickedHex == selectedHex) {
				selectedHex.rotateTile();
				map.repaint(selectedHex.getBounds());

				return;
				
			} else {

				if (selectedHex != null && clickedHex != selectedHex) {
					selectedHex.removeTile();
					map.selectHex(null);
				}
				if (clickedHex != null) {
					if (clickedHex.getHexModel().isUpgradeableNow())
					/*
					 * Direct call to Model to be replaced later by use of
					 * allowedTilesPerHex. Would not work yet.
					 */
					{
						map.selectHex(clickedHex);
						setLocalStep(SELECT_TILE);
					} else {
						JOptionPane.showMessageDialog(map,
								"This hex cannot be upgraded now");
					}
				}
			}
		}

		orWindow.repaintORPanel();
	}
	
	public void tileSelected (int tileId) {
		
        if (map.getSelectedHex().dropTile(tileId)) {
            /* Lay tile */
            map.repaint(map.getSelectedHex().getBounds());
            setLocalStep(ORUIManager.ROTATE_OR_CONFIRM_TILE);
        } else {
            /* Tile cannot be laid in a valid orientation: refuse it */
            JOptionPane.showMessageDialog(map,
                    "This tile cannot be laid in a valid orientation.");
            tileUpgrades.remove(TileManager.get().getTile(tileId));
            setLocalStep(ORUIManager.SELECT_TILE);
            upgradePanel.showUpgrades();
        }

	}
	
	
	
	public void tokenSelected (LayToken tokenAllowance) {
		
		if (tokenAllowance != null && allowedTokenLays.contains(tokenAllowance)) {
			selectedTokenAllowance = tokenAllowance;
			selectedTokenIndex = allowedTokenLays.indexOf(tokenAllowance);
		} else {
			selectedTokenAllowance = null;
			selectedTokenIndex = -1;
		}
		upgradePanel.setSelectedTokenIndex (selectedTokenIndex);
	}
	
    private void layTile () {
        
        GUIHex selectedHex = map.getSelectedHex();
        
        if (selectedHex != null && selectedHex.canFixTile())
        {
            List<LayTile> allowances = map.getTileAllowancesForHex(selectedHex.getHexModel());
            LayTile allowance = allowances.get(0); // TODO Wrong if we have an additional special property (18AL Lumber Terminal)
            allowance.setChosenHex(selectedHex.getHexModel());
            allowance.setOrientation(selectedHex.getProvisionalTileRotation());
            allowance.setLaidTile(selectedHex.getProvisionalTile());
            
            if (orWindow.process(allowance)) {
                selectedHex.fixTile();
                //updateStatus();
            } else {
                selectedHex.removeTile();
                setLocalStep (SELECT_HEX_FOR_TILE);
            }
            map.selectHex(null);
        }
    }
    
	
    public void layBaseToken () {
        
        GUIHex selectedHex = map.getSelectedHex();
        
        if (selectedHex != null)
        {
            List<LayBaseToken> allowances = map.getBaseTokenAllowanceForHex(selectedHex.getHexModel());
            // Pick the first one (unknown if we will ever need more than one)
            LayBaseToken allowance = allowances.get(0);
            int station;
            List<Station> stations = selectedHex.getHexModel().getStations();
            
            switch (stations.size()) {
            case 0: // No stations
                return;
                
            case 1:
                station = 0;
                break;
                
            default:
                Station stationObject = (Station) JOptionPane.showInputDialog(orWindow,
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

            if  (orWindow.process(allowance)) {
            	upgradePanel.clear();
                selectedHex.fixToken();
            } else {
            	setLocalStep (ORUIManager.SELECT_HEX_FOR_TOKEN);
            }
        }
    }

	/** Lay Token finished.	
	 * 
	 * @param action The LayBonusToken action object of the laid token.
	 */
	public void layBonusToken (PossibleAction action) {
		
		// Assumption for now: always BonusToken
		// We might use it later for BaseTokens too.
		
        HexMap map = mapPanel.getMap();
        GUIHex selectedHex = map.getSelectedHex();
        
        if (selectedHex != null)
        {
            LayToken executedAction = (LayToken) action;
            
            executedAction.setChosenHex(selectedHex.getHexModel());

            if  (orWindow.process(executedAction)) {
            	upgradePanel.clear();
            	map.selectHex(null);
				selectedHex = null;
            }
        }
	}
    
    public void buyTrain()
    {

        List<String> prompts = new ArrayList<String>();
        Map<String, BuyTrain> promptToTrain = new HashMap<String, BuyTrain>();
        TrainI train;

        BuyTrain selectedTrain;
        String prompt;
        StringBuffer b;
        int cost;
        Portfolio from;
        
        List<BuyTrain> buyableTrains = possibleActions.getType(BuyTrain.class);
        for (BuyTrain bTrain : buyableTrains)
        {
            train = bTrain.getTrain();
            cost = bTrain.getFixedCost();
            from = bTrain.getFromPortfolio();
            
            /* Create a prompt per buying option */
            b = new StringBuffer();
            
            b.append(LocalText.getText("BUY_TRAIN_FROM", new String[] {
                            train.getName(),
                            from.getName()}));
            if (bTrain.isForExchange())
            {
                b.append(" (").append(LocalText.getText("EXCHANGED")).append(")");
            }
            if (cost > 0)
            {
                b.append(" ").append(LocalText.getText("AT_PRICE",Bank.format(cost)));
            }
            if (bTrain.mustPresidentAddCash())
            {
                b.append(" ").append(LocalText.getText("YOU_MUST_ADD_CASH",
                        Bank.format(bTrain.getPresidentCashToAdd())));
            }
            else if (bTrain.mayPresidentAddCash())
            {
                b.append(" ").append(LocalText.getText("YOU_MAY_ADD_CASH",
                        Bank.format(bTrain.getPresidentCashToAdd())));
            }
            prompt = b.toString();
            prompts.add(prompt);
            promptToTrain.put(prompt, bTrain);
        }

        if (prompts.size() == 0) {
            JOptionPane.showMessageDialog(orWindow, 
                    LocalText.getText("CannotBuyAnyTrain"));
            return;
        }

        String boughtTrain;
        boughtTrain = (String) JOptionPane.showInputDialog(orWindow,
            LocalText.getText("BUY_WHICH_TRAIN"),
            LocalText.getText("WHICH_TRAIN"),
            JOptionPane.QUESTION_MESSAGE,
            null,
            prompts.toArray(),
            prompts.get(0));
        if (!Util.hasValue(boughtTrain))
            return;
        
        selectedTrain = (BuyTrain) promptToTrain.get(boughtTrain);
        if (selectedTrain == null)
            return;
        
        train = selectedTrain.getTrain();
        Portfolio seller = selectedTrain.getFromPortfolio();
        int price = selectedTrain.getFixedCost();

        if (price == 0 && seller.getOwner() instanceof PublicCompanyI) {
            prompt = LocalText.getText ("WHICH_TRAIN_PRICE",
                    new String [] {orPanel.getORComp().getName(), train.getName(), seller.getName()});
            String response;
            for (;;) {
                response = JOptionPane.showInputDialog(orWindow,
                    prompt, LocalText.getText("WHICH_PRICE"),
                    JOptionPane.QUESTION_MESSAGE);
                if (response == null) return; // Cancel
                try {
                    price = Integer.parseInt(response);
                } catch (NumberFormatException e) {
                    // Price stays 0, this is handled below
                }
                if (price > 0) break; // Got a good (or bad, but valid) price.
                
                if (!prompt.startsWith("Please")) {
                    prompt = LocalText.getText("ENTER_PRICE_OR_CANCEL")
                        + "\n" + prompt;
                }
            }
        }

        TrainI exchangedTrain = null;
        if (train != null && selectedTrain.isForExchange())
        {
            List<TrainI> oldTrains = selectedTrain.getTrainsForExchange();
            List<String> oldTrainOptions = new ArrayList<String>(oldTrains.size());
            String[] options = new String[oldTrains.size() + 1];
            options[0] = LocalText.getText("None");
            for (int j = 0; j < oldTrains.size(); j++)
            {
                options[j + 1] = LocalText.getText("N_Train", oldTrains.get(j).getName());
                oldTrainOptions.add(options[j+1]);
            }
            String exchangedTrainName = (String) JOptionPane.showInputDialog(orWindow,
                    LocalText.getText("WHICH_TRAIN_EXCHANGE_FOR",
                                    Bank.format(price)),
                    LocalText.getText("WHICH_TRAIN_EXCHANGE"),
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]);
            int index = oldTrainOptions.indexOf(exchangedTrainName);
            if (index >= 0)
            {
                exchangedTrain = oldTrains.get(index);
            }

        }

        if (train != null)
        {
            // Remember the old off-board revenue step
            int oldOffBoardRevenueStep = PhaseManager.getInstance().getCurrentPhase().getOffBoardRevenueStep();

            selectedTrain.setPricePaid(price);
            selectedTrain.setExchangedTrain(exchangedTrain);

            if (orWindow.process (selectedTrain)) {
                
                // Check if any trains must be discarded
                // Keep looping until all relevant companies have acted
                while (possibleActions.contains(DiscardTrain.class))
                {
                    // We expect one company at a time
                    DiscardTrain dt = (DiscardTrain)possibleActions.getType(DiscardTrain.class).get(0);
                        
                    PublicCompanyI c = dt.getCompany();
                    String playerName = dt.getPlayerName();
                    List<TrainI> trains = dt.getOwnedTrains();
                    List<String> trainOptions = new ArrayList<String>(trains.size());
                    String[] options = new String[trains.size()];

                    for (int i=0; i<options.length; i++) {
                        options[i] = LocalText.getText("N_Train", trains.get(i).getName());
                        trainOptions.add(options[i]);
                    }
                    String discardedTrainName = (String) JOptionPane.showInputDialog (orWindow,
                            LocalText.getText("HAS_TOO_MANY_TRAINS", new String[] {
                                    playerName,
                                    c.getName()
                            }),
                            LocalText.getText("WhichTrainToDiscard"),
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]);
                    if (discardedTrainName != null)
                    {
                        TrainI discardedTrain = trains.get(trainOptions.indexOf(discardedTrainName));
                        
                        dt.setDiscardedTrain(discardedTrain);
                        
                        orWindow.process (dt);
                    }
                }
            }
            
            int newOffBoardRevenueStep = PhaseManager.getInstance().getCurrentPhase().getOffBoardRevenueStep();
            if (newOffBoardRevenueStep != oldOffBoardRevenueStep) {
                HexMap.updateOffBoardToolTips();
            }

        }

    }    


 
    public void buyPrivate() {

        int amount, index;
        List<String> privatesForSale = new ArrayList<String>();
        List<BuyPrivate> privates =  possibleActions.getType(BuyPrivate.class);
        String chosenOption;
        BuyPrivate chosenAction = null;
        int minPrice = 0, maxPrice = 0;

        for (BuyPrivate action : privates) {
            privatesForSale.add(LocalText.getText("BuyPrivatePrompt", new String[] {
                    action.getPrivateCompany().getName(),
                    action.getPrivateCompany().getPortfolio().getName(),
                    Bank.format(action.getMinimumPrice()),
                    Bank.format(action.getMaximumPrice())
            }));
        }

        if (privatesForSale.size() > 0) {
            chosenOption = (String) JOptionPane.showInputDialog(orWindow, 
                    LocalText.getText("BUY_WHICH_PRIVATE"), 
                    LocalText.getText("WHICH_PRIVATE"),
                    JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    privatesForSale.toArray(), 
                    privatesForSale.get(0));
            if (chosenOption != null) {
                index = privatesForSale.indexOf(chosenOption);
                chosenAction = privates.get(index);
                minPrice = chosenAction.getMinimumPrice();
                maxPrice = chosenAction.getMaximumPrice();
                String price = (String) JOptionPane.showInputDialog(orWindow,
                        LocalText.getText("WHICH_PRIVATE_PRICE", new String[] {
                                chosenOption,
                                Bank.format(minPrice), 
                                Bank.format(maxPrice)}),
                        LocalText.getText("WHICH_PRICE"),
                        JOptionPane.QUESTION_MESSAGE);
                try {
                    amount = Integer.parseInt(price);
                } catch (NumberFormatException e) {
                    amount = 0; // This will generally be refused.
                }
                chosenAction.setPrice(amount);

                if (orWindow.process (chosenAction)) {
                    updateMessage();
                }
            }
        }

    }


	public void executeUpgrade()
	{
        GUIHex selectedHex = map.getSelectedHex();
        
        if (tileLayingEnabled)
		{
            if (selectedHex == null) {
                orWindow.displayORUIMessage(LocalText.getText("SelectAHexForToken"));
            } else if (selectedHex.getProvisionalTile() == null ) {
                orWindow.displayORUIMessage(LocalText.getText("SelectATile"));
            } else {
                layTile();
            }
		}
		else if (tokenLayingEnabled)
		{
            if (selectedHex == null) {
                orWindow.displayORUIMessage(LocalText.getText("SelectAHexForTile"));
            } else if (selectedTokenAllowance == null ) {
                orWindow.displayORUIMessage(LocalText.getText("SelectAToken"));
            } else if (selectedTokenAllowance instanceof LayBaseToken) {
                layBaseToken();
            } else {
                layBonusToken((LayBonusToken)selectedTokenAllowance);
            }
		}
	}
    
	public void cancelUpgrade()
	{
		GUIHex selectedHex = mapPanel.getMap().getSelectedHex();

		if (tokenLayingEnabled)
		{
			if (selectedHex != null)
				selectedHex.removeToken();
            if (!localAction) orWindow.process (new NullAction (NullAction.SKIP));
		}
		else if (tileLayingEnabled)
		{
			if (selectedHex != null)
				selectedHex.removeTile();
			if (!localAction) orWindow.process (new NullAction (NullAction.SKIP));
		}

	}
	
    public void updateStatus() {
        
        updateStatus (null);
        
    }
    
    public void updateStatus (PossibleAction actionToComplete) {
        
        mapRelatedActions.clear();
        
        orPanel.setHighlightsOff();

        if (actionToComplete != null) {
            log.debug("ExecutedAction: "+actionToComplete);
        }
        // End of possible action debug listing 

        orStep = oRound.getStep();
        log.debug("OR step="+orStep+" "+OperatingRound.stepNames[orStep]);
        
        if (oRound.getOperatingCompanyIndex() != orCompIndex) {
            if (orCompIndex >= 0) orPanel.finishORCompanyTurn(orCompIndex);
            setORCompanyTurn(oRound.getOperatingCompanyIndex());
        }
        
        orPanel.initORCompanyTurn(orCompIndex);
        
        privatesCanBeBoughtNow = possibleActions.contains(BuyPrivate.class);
        
        
        if (possibleActions.contains(LayTile.class)) {
            
            orPanel.initTileLayingStep();
            
            orWindow.requestFocus();
            
            log.debug ("Tiles can be laid");
            mapRelatedActions.addAll(possibleActions.getType(LayTile.class));
            
            orPanel.initPrivateBuying(privatesCanBeBoughtNow);
            
        } else if (possibleActions.contains(LayBaseToken.class)) {
            
            orWindow.requestFocus();
            
            // Include bonus tokens
            List<LayToken> possibleTokenLays = possibleActions.getType(LayToken.class);
            mapRelatedActions.addAll(possibleTokenLays);
            allowedTokenLays = possibleTokenLays;

            orPanel.initTokenLayingStep();

            log.debug ("BaseTokens can be laid");

        } else if (possibleActions.contains(SetDividend.class)
                && localStep == SELECT_PAYOUT) {
            
            SetDividend action;
            if (actionToComplete != null) {
                action = (SetDividend) actionToComplete;
            } else {
                action = (SetDividend) possibleActions.getType(SetDividend.class).get(0);
            }

            log.debug("Payout action before cloning: "+action);
            
            orPanel.initPayoutStep(orCompIndex,
                    action,
                    action.isAllocationAllowed(SetDividend.WITHHOLD),
                    action.isAllocationAllowed(SetDividend.SPLIT),
                    action.isAllocationAllowed(SetDividend.PAYOUT));

            setMessage(LocalText.getText("SelectPayout"));

        } else if (possibleActions.contains(SetDividend.class)) {
            
            SetDividend action = (SetDividend) possibleActions.getType(SetDividend.class).get(0);
            
            orPanel.initRevenueEntryStep(orCompIndex, action);
            setMessage(LocalText.getText("EnterRevenue"));
                
        } else if (possibleActions.contains(BuyTrain.class)) {
            
            orPanel.initTrainBuying(oRound.getOperatingCompany().mayBuyTrains());

            orPanel.initPrivateBuying(privatesCanBeBoughtNow);

            setMessage(LocalText.getText("BuyTrain"));

        } else if (possibleActions.contains(DiscardTrain.class)) {
            
        } else if (orStep == OperatingRound.STEP_FINAL) {

            orPanel.finishORCompanyTurn(orCompIndex);
        }
        
        setMapRelatedActions (mapRelatedActions);
        
        GameAction undoAction = null;
        GameAction redoAction = null;
        
        if (possibleActions.contains(NullAction.class)) {
            
            List<NullAction> actions = possibleActions.getType(NullAction.class);
            for (NullAction action : actions) {
                switch (action.getMode()) {
                case NullAction.DONE:
                    orPanel.enableDone (action);
                    break;
                }
            }
        }
        
        if (possibleActions.contains(GameAction.class)) {
            
            List<GameAction> actions = possibleActions.getType(GameAction.class);
            for (GameAction action : actions) {
                switch (action.getMode()) {
                case GameAction.UNDO:
                    undoAction = action;
                    break;
                case GameAction.REDO:
                    redoAction = action;
                    break;
                }
            }
        }
        orPanel.enableUndo(undoAction);
        orPanel.enableRedo(redoAction);
        
        // Bonus tokens can be laid anytime, so we must also handle
        // these outside the token laying step.
        if (possibleActions.contains(LayBonusToken.class)
                && !possibleActions.contains(LayBaseToken.class)) {
            
        	List<LayBonusToken> bonusTokenActions = possibleActions.getType(LayBonusToken.class);
        	orPanel.initSpecialActions(bonusTokenActions);
        } else {
            orPanel.initSpecialActions(null);
        }

        orPanel.redisplay();
    }
    
    public void setORCompanyTurn(int orCompIndex) {

        orPanel.resetORCompanyTurn(orCompIndex);

        this.orCompIndex = orCompIndex;
        orComp = orCompIndex >= 0 ? companies[orCompIndex] : null;

        if (orCompIndex >= 0) {
            // Give a new company the turn.
            this.playerIndex = companies[orCompIndex].getPresident().getIndex();
        }
    }
    

	
	public void setLocalStep(int localStep)
	{
        log.debug ("Setting upgrade step to "+localStep+" "+ORUIManager.messageKey[localStep]);
		this.localStep = localStep;
		
		updateMessage();
		updateUpgradesPanel();
	}

	public void updateUpgradesPanel() {

		if (upgradePanel != null)
		{
			log.debug ("Initial localStep is "+localStep+" "+ORUIManager.messageKey[localStep]);
			switch (localStep)
			{
				case INACTIVE:
					upgradePanel.setTileUpgrades(null);
					upgradePanel.setPossibleTokenLays(null);
					upgradePanel.setTokenMode(false);
					upgradePanel.setDoneEnabled(false);
					upgradePanel.setCancelEnabled(false);
					break;
				case SELECT_HEX_FOR_TILE:
					upgradePanel.setDoneText("LayTile");
					upgradePanel.setCancelText("NoTile");
					upgradePanel.setDoneEnabled(false);
					upgradePanel.setCancelEnabled(true);
					break;
				case SELECT_TILE:
					upgradePanel.populate();
					upgradePanel.setDoneEnabled(false);
					break;
				case ROTATE_OR_CONFIRM_TILE:
					upgradePanel.setDoneEnabled(true);
					break;
				case SELECT_HEX_FOR_TOKEN:
					upgradePanel.setDoneEnabled(false);
					upgradePanel.setCancelEnabled(true);
					upgradePanel.setDoneText("LayToken");
					upgradePanel.setCancelText("NoToken");
					break;
				case SELECT_TOKEN:
                    List<LayToken> allowances = map.getTokenAllowanceForHex(mapPanel.getMap().getSelectedHex().getHexModel());
                    log.debug("Allowed tokens for hex "+mapPanel.getMap().getSelectedHex().getName()+" are:");
                    for (LayToken allowance : allowances) {
                    	log.debug("  "+allowance.toString());
                    }
					upgradePanel.setPossibleTokenLays(allowances);
                    if (allowances.size() > 1) {
                        upgradePanel.setDoneEnabled(false);
                        break;
                    } else {
                        // Only one token possible: skip this step and fall through
                    	tokenSelected(allowances.get(0));
                        localStep = CONFIRM_TOKEN;
                    }
				case CONFIRM_TOKEN:
					upgradePanel.setDoneEnabled(true);
					break;
				default:
					upgradePanel.setDoneEnabled(false);
					upgradePanel.setCancelEnabled(false);
				break;
			}
		}
		log.debug ("Final localStep is "+localStep+" "+messageKey[localStep]);
        upgradePanel.showUpgrades(); //??

	}

	public void setMessage (String messageKey) {
		messagePanel.setMessage(messageKey);
	}
	
	public void setLocalAction (boolean value) {
		localAction = value;
	}
	
	// TEMPORARY
	public ORWindow getORWindow () {
		return orWindow;
	}
	
	// TEMPORARY
	public MapPanel getMapPanel() {
		return orWindow.getMapPanel();
	}
	
	// TEMPORARY
	public HexMap getMap() {
		return map;
	}

}
