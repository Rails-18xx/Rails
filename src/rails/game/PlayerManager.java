package rails.game;

import java.util.*;

import rails.common.LocalText;
import rails.common.parser.Configurable;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;

public class PlayerManager extends RailsManager implements Configurable {

    private int numberOfPlayers;
    private List<Player> players;
    private List<String> playerNames;
    private Map<String, Player> playerMap;

    public int maxPlayers;

    public int minPlayers;

    private int[] playerStartCash = new int[Player.MAX_PLAYERS];

    private int[] playerCertificateLimits = new int[Player.MAX_PLAYERS];

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
            playerStartCash[number] = startCash;
            certLimit = playerTag.getAttributeAsInteger("certLimit");
            playerCertificateLimits[number] = certLimit;

            minPlayers = Math.min(minPlayers, number);
            maxPlayers = Math.max(maxPlayers, number);
        }
    }

    public void finishConfiguration (GameManager gameManager) {
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

    protected int getStartCash () {
        return playerStartCash[numberOfPlayers];
    }

    /** Only to be called at initialisation time.
     * Does not reflect later changes to this limit.
     */
    public int getInitialPlayerCertificateLimit() {
        return playerCertificateLimits[numberOfPlayers];
    }

}
