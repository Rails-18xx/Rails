package rails.game;

import java.util.List;

import rails.common.Defs;
import rails.game.action.PossibleAction;
import rails.util.Tag;

public interface GameManagerI {

    /**
     * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public abstract void configureFromXML(Tag tag)
            throws ConfigurationException;

    public abstract void startGame(PlayerManager playerManager,
            CompanyManagerI companyManager, PhaseManager phaseManager,
            TrainManagerI trainManager);

    public abstract CompanyManagerI getCompanyManager();

    /**
     * Should be called by each Round when it finishes.
     *
     * @param round The object that represents the finishing round.
     */
    public abstract void nextRound(RoundI round);

    public abstract String getCompositeORNumber();

    public abstract int getSRNumber();

    public abstract void startShareSellingRound(OperatingRound or,
            PublicCompanyI companyNeedingTrain, int cashToRaise);

    public abstract void startTreasuryShareTradingRound();

    /**
     * The central server-side method that takes a client-side initiated action
     * and processes it.
     *
     * @param action A PossibleAction subclass object sent by the client.
     * @return TRUE is the action was valid.
     */
    public abstract boolean process(PossibleAction action);

    public abstract void processOnReload(List<PossibleAction> actions)
            throws Exception;

    public abstract void finishShareSellingRound();

    public abstract void finishTreasuryShareRound();

    public abstract void registerBankruptcy();

    /**
     * To be called by the UI to check if the rails.game is over.
     *
     * @return
     */
    public abstract boolean isGameOver();

    public abstract void logGameReport();

    /**
     * Create a HTML-formatted rails.game status report.
     *
     * @return
     */
    public abstract String getGameReport();

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

    /**
     * @return the StartPacket
     */
    public abstract StartPacket getStartPacket();

    /**
     * @return Current phase
     */
    public abstract PhaseI getCurrentPhase();

    public abstract PhaseManager getPhaseManager();
    
    public abstract TrainManagerI getTrainManager ();
    public PlayerManager getPlayerManager();


    public abstract String getHelp();

    public abstract boolean canAnyCompanyHoldShares();

    public abstract String getClassName(Defs.ClassName key);

    public abstract int getStockRoundSequenceRule();

    public abstract int getTreasuryShareLimit();

    public abstract Object getCommonParameter(Defs.Parm key);
    
    public RoundI getInterruptedRound();
}