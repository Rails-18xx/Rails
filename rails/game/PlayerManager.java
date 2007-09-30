package rails.game;

import java.util.*;

public class PlayerManager
{

	int numberOfPlayers;
	private List<Player> players;
	private List<String> playerNames;
	private Map <String, Player> playerMap;

	public PlayerManager(List<String> playerNames)
	{
		Player player;
		
		this.playerNames = playerNames;
		numberOfPlayers = playerNames.size();

		players = new ArrayList<Player>(numberOfPlayers);
		playerMap = new HashMap<String, Player>(numberOfPlayers);

		for (String playerName : playerNames)
		{
			player = new Player (playerName);
			players.add (player);
			playerMap.put(playerName, player);
		}

	}

	/**
	 * @return Returns an array of all players.
	 */
	public List<Player> getPlayers()
	{
		return players;
	}

	public Player getPlayerByName(String name)
	{
		return playerMap.get(name);
	}
	
	public List<String> getPlayerNames() {
		return playerNames;
	}
	
	public Player getPlayerByIndex (int index) {
		return players.get(index);
	}

}
