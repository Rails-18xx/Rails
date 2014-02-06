package net.sf.rails.game;

import java.util.*;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;


public class PlayerManager extends RailsManager implements Configurable {

    private static Logger log =
            LoggerFactory.getLogger(PlayerManager.class);

    // static data
    private int numberOfPlayers;
    private List<Player> players;
    private List<String> playerNames;
    private Map<String, Player> playerMap;
    
    // configure data
    private int maxPlayers;
    private int minPlayers;
    private final Map<Integer, Integer> playerStartCash = 
            Maps.newHashMap();
    private final Map<Integer, Integer> playerCertificateLimits = 
            Maps.newHashMap();
    
    // dynamic data
    private final GenericState<Player> currentPlayer = GenericState.create(this, "currentPlayer");
    private final GenericState<Player> priorityPlayer = GenericState.create(this, "priorityPlayer");
    private final IntegerState playerCertificateLimit = IntegerState.create(this, "playerCertificateLimit");

    /**
     * nextPlayerMessages collects all messages to be displayed to the next player
     */
    protected final ArrayListState<String> nextPlayerMessages = ArrayListState.create(this, "nextPlayerMessages");
    
    /**
     * Used by Configure (via reflection) only
     */
    public PlayerManager(RailsRoot parent, String id) {
        super(parent, id);
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {

        int number, startCash, certLimit;

        List<Tag> playerTags = tag.getChildren("Players");
        minPlayers = 99;
        maxPlayers = 0;
        for (Tag playerTag : playerTags) {
            number = playerTag.getAttributeAsInteger("number");
            startCash = playerTag.getAttributeAsInteger("cash");
            playerStartCash.put(number, startCash);
            certLimit = playerTag.getAttributeAsInteger("certLimit");
            playerCertificateLimits.put(number, certLimit);

            minPlayers = Math.min(minPlayers, number);
            maxPlayers = Math.max(maxPlayers, number);
        }
    }

    public void finishConfiguration (RailsRoot root) {
        for (Player player:players) {
            player.finishConfiguration();
        }
    }
    
    public void setPlayers (List<String> playerNames, Bank bank) {

        Player player;

        this.playerNames = playerNames;
        numberOfPlayers = playerNames.size();

        players = new ArrayList<Player>(numberOfPlayers);
        playerMap = new HashMap<String, Player>(numberOfPlayers);

        int playerIndex = 0;
        int startCash = getStartCash();
        String cashText = null;
        for (String playerName : playerNames) {
            player = Player.create(this, playerName, playerIndex++);
            players.add(player);
            playerMap.put(playerName, player);
            cashText = Currency.fromBank(startCash, player);
            ReportBuffer.add(this, LocalText.getText("PlayerIs",
                    playerIndex,
                    player.getId() ));
        }
        ReportBuffer.add(this, LocalText.getText("PlayerCash", cashText));
        ReportBuffer.add(this, LocalText.getText("BankHas", bank.getWallet().formattedValue()));
    }
    
    // sets initial priority player and certificate limits
    public void init() {
        priorityPlayer.set(players.get(0));
        setPlayerCertificateLimit(getInitialPlayerCertificateLimit());
    }
    
    public int getMinPlayers() {
        return minPlayers;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * @return Returns an array of all players.
     */
    public List<Player> getPlayers() {
        return players;
    }

    public Player getPlayerByName(String name) {
        return playerMap.get(name);
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }
    
    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    protected int getStartCash () {
        return playerStartCash.get(numberOfPlayers);
    }

    /** Only to be called at initialisation time.
     * Does not reflect later changes to this limit.
     */
    public int getInitialPlayerCertificateLimit() {
        return playerCertificateLimits.get(numberOfPlayers);
    }

    // dynamic getter/setters
    public GenericState<Player> getCurrentPlayerModel() {
        return currentPlayer;
    }
    
    public Player getCurrentPlayer() {
        return currentPlayer.value();
    }
    
    public Player getPriorityPlayer() {
        return priorityPlayer.value();
    }
    
    public void setCurrentPlayer(Player player) {
        // transfer messages for the next player to the display buffer
        // TODO: refactor nextPlayerMessages inside DisplayBuffer
        if (currentPlayer.value() != player && !nextPlayerMessages.isEmpty()) {
            DisplayBuffer.add(this, 
                    LocalText.getText("NextPlayerMessage", getCurrentPlayer().getId()));
            for (String s:nextPlayerMessages.view())
                DisplayBuffer.add(this, s);
            nextPlayerMessages.clear();
        }
        currentPlayer.set(player);
    }
    
    public void setPriorityPlayer(Player player) {
        priorityPlayer.set(player);
        log.debug("Priority player set to " + player.getIndex() + " "
                + player.getId());
      }
    public int getPlayerCertificateLimit(Player player) {
        return playerCertificateLimit.value();
    }

    public void setPlayerCertificateLimit(int newLimit) {
        playerCertificateLimit.set (newLimit);
    }

    public IntegerState getPlayerCertificateLimitModel () {
        return playerCertificateLimit;
    }
    
    // further dynamic methods
    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        currentPlayerIndex = currentPlayerIndex % numberOfPlayers;
        setCurrentPlayer(players.get(currentPlayerIndex));
    }
    
    public void setNextPlayer() {
        int currentPlayerIndex = currentPlayer.value().getIndex();
        do {
            currentPlayerIndex = ++currentPlayerIndex % numberOfPlayers;
        } while (players.get(currentPlayerIndex).isBankrupt());
        setCurrentPlayer(players.get(currentPlayerIndex));
    }
    
    public void setPriorityPlayer() {
        int priorityPlayerIndex =
                (getCurrentPlayer().getIndex() + 1) % numberOfPlayers;
            setPriorityPlayer(players.get(priorityPlayerIndex));
    }
    
    public int getCurrentPlayerIndex() {
        return getCurrentPlayer().getIndex();
    }
    
    public Player getPlayerByIndex(int index) {
        return players.get(index % numberOfPlayers);
    }

    // FIXME: This is not undo proof!
    public Player reorderPlayersByCash (boolean ascending) {

        final boolean _ascending = ascending;
        Collections.sort (players, new Comparator<Player>() {
            public int compare (Player p1, Player p2) {
                return _ascending ? p1.getCash() - p2.getCash() : p2.getCash() - p1.getCash();
            }
        });

        Player player;
        for (int i=0; i<players.size(); i++) {
            player = players.get(i);
            player.setIndex (i);
            playerNames.set (i, player.getId());
            log.debug("New player "+i+" is "+player.getId() +" (cash="+Currency.format(this, player.getCash())+")");
        }

        return players.get(0);
    }


}
