/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PlayerManager.java,v 1.12 2009/10/31 17:08:26 evos Exp $ */
package rails.game;

import java.util.*;

import rails.util.LocalText;
import rails.util.Tag;

public class PlayerManager implements ConfigurableComponentI {

    private int numberOfPlayers;
    private List<Player> players;
    private List<String> playerNames;
    private Map<String, Player> playerMap;

    public int maxPlayers;

    public int minPlayers;

    private int[] playerStartCash = new int[Player.MAX_PLAYERS];

    private int[] playerCertificateLimits = new int[Player.MAX_PLAYERS];

    public PlayerManager() {

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

    public void finishConfiguration (GameManagerI gameManager) {}
    
    public void setPlayers (List<String> playerNames, Bank bank) {

        Player player;

        this.playerNames = playerNames;
        numberOfPlayers = playerNames.size();

        players = new ArrayList<Player>(numberOfPlayers);
        playerMap = new HashMap<String, Player>(numberOfPlayers);

        int playerIndex = 0;
        int startCash = getStartCash();
        for (String playerName : playerNames) {
            player = new Player(playerName, playerIndex++);
            players.add(player);
            playerMap.put(playerName, player);
            player.addCash(startCash);
            bank.addCash(-startCash);
            ReportBuffer.add(LocalText.getText("PlayerIs",
                    playerIndex,
                    player.getName() ));
        }
        ReportBuffer.add(LocalText.getText("PlayerCash", Bank.format(startCash)));
        ReportBuffer.add(LocalText.getText("BankHas",
                Bank.format(bank.getCash())));
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
