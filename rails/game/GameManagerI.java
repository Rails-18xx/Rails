package rails.game;

import java.util.List;
import java.util.Map;

import rails.algorithms.RevenueManager;
import rails.common.GuiDef;
import rails.common.GuiHints;
import rails.game.action.PossibleAction;
import rails.game.correct.CorrectionManagerI;
import rails.game.correct.CorrectionType;
import rails.game.model.ModelObject;
import rails.game.move.MoveStack;
import rails.game.move.MoveableHolder;
import rails.game.special.SpecialPropertyI;

public interface GameManagerI extends MoveableHolder, ConfigurableComponentI {

    /**
     * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public abstract void init(String gameName, PlayerManager playerManager,
            CompanyManagerI companyManager, PhaseManager phaseManager,
            TrainManager trainManager, StockMarketI stockMarket,
            MapManager mapManager, TileManager tileManager,
            RevenueManager revenueManager, Bank bank);
    public abstract void startGame(Map<String, String> gameOptions);

    public abstract CompanyManagerI getCompanyManager();

    /**
     * Should be called by each Round when it finishes.
     *
     * @param round The object that represents the finishing round.
     */
    public abstract void nextRound(RoundI round);

    public String getORId ();
    public abstract String getCompositeORNumber();
    public int getRelativeORNumber();
    public int getAbsoluteORNumber ();

    public abstract int getSRNumber();

    public abstract void startShareSellingRound(Player player, int cashToRaise,
            PublicCompanyI cashNeedingCompany, boolean checkDumpOtherCompanies);

    public abstract void startTreasuryShareTradingRound();

    /**
     * The central server-side method that takes a client-side initiated action
     * and processes it.
     *
     * @param action A PossibleAction subclass object sent by the client.
     * @return TRUE is the action was valid.
     */
    public abstract boolean process(PossibleAction action);

    public abstract boolean processOnReload(PossibleAction action)
    throws Exception;

    public void finishLoading ();

    public abstract void finishShareSellingRound();

    public abstract void finishTreasuryShareRound();

    public abstract void registerBankruptcy();

    public abstract void registerBrokenBank();
    public void registerMaxedSharePrice(PublicCompanyI company, StockSpaceI space);
    
    public boolean isDynamicOperatingOrder();

    /**
     * To be called by the UI to check if the rails.game is over.
     *
     * @return
     */
    public abstract boolean isGameOver();

    public void setGameOverReportedUI(boolean b);

    public boolean getGameOverReportedUI();

    /**
     * Create a HTML-formatted rails.game status report.
     *
     * @return
     */
    public abstract List<String> getGameReport();

    /**
     * Should be called whenever a Phase changes. The effect on the number of
     * ORs is delayed until a StockRound finishes.
     *
     */
    public abstract RoundI getCurrentRound();

    /**
     * @return Returns the currentPlayerIndex.
     */
    public abstract int getCurrentPlayerIndex();

    /**
     * @param currentPlayerIndex The currentPlayerIndex to set.
     */
    public abstract void setCurrentPlayerIndex(int currentPlayerIndex);

    public abstract void setCurrentPlayer(Player player);

    /**
     * Set priority deal to the player after the current player.
     *
     */
    public abstract void setPriorityPlayer();

    public abstract void setPriorityPlayer(Player player);

    /**
     * @return Returns the priorityPlayer.
     */
    public abstract Player getPriorityPlayer();

    /**
     * @return Returns the currentPlayer.
     */
    public abstract Player getCurrentPlayer();

    /**
     * @return Returns the players.
     */
    public abstract List<Player> getPlayers();

    public abstract int getNumberOfPlayers();

    public abstract List<String> getPlayerNames();

    public abstract List<PublicCompanyI> getAllPublicCompanies();

    public abstract List<PrivateCompanyI> getAllPrivateCompanies();

    /**
     * Return a player by its index in the list, modulo the number of players.
     *
     * @param index The player index.
     * @return A player object.
     */
    public abstract Player getPlayerByIndex(int index);

    public abstract void setNextPlayer();

    public void addPortfolio (Portfolio portfolio);
    public Portfolio getPortfolioByName (String name);
    public Portfolio getPortfolioByUniqueName (String name);

    /**
     * @return the StartPacket
     */
    public abstract StartPacket getStartPacket();

    /**
     * @return Current phase
     */
    public abstract PhaseI getCurrentPhase();

    public abstract PhaseManager getPhaseManager();
    public void initialiseNewPhase(PhaseI phase);

    public abstract TrainManager getTrainManager ();
    public PlayerManager getPlayerManager();
    public TileManager getTileManager();
    public StockMarketI getStockMarket();
    public MapManager getMapManager();
    public RevenueManager getRevenueManager();
    public Bank getBank ();

    public String getGameName ();
    public String getGameOption (String key);

    public int getPlayerCertificateLimit(Player player);
    public void setPlayerCertificateLimit(int newLimit);
    public ModelObject getPlayerCertificateLimitModel ();

    public abstract String getHelp();

    public abstract boolean canAnyCompanyHoldShares();

    public abstract String getClassName(GuiDef.ClassName key);

    public abstract Object getGuiParameter(GuiDef.Parm key);
    public Object getGameParameter (GameDef.Parm key);
    public void setGameParameter (GameDef.Parm key, Object value);

    public RoundI getInterruptedRound();

    public List<SpecialPropertyI> getCommonSpecialProperties ();
    public <T extends SpecialPropertyI> List<T> getSpecialProperties(
            Class<T> clazz, boolean includeExercised);

    public String getGMKey ();
    public MoveStack getMoveStack ();
    public DisplayBuffer getDisplayBuffer();
    public void addToNextPlayerMessages(String s, boolean undoable);
    public ReportBuffer getReportBuffer();
    public GuiHints getUIHints();

    public CorrectionManagerI getCorrectionManager(CorrectionType ct);
    public List<PublicCompanyI> getCompaniesInRunningOrder ();
	public boolean isReloading();
	public void setReloading(boolean reloading);
	public void setSkipDone (GameDef.OrStep step);
	
	public Player reorderPlayersByCash(boolean high);
    //public void reorderPlayersByCash(boolean high);
}