package game;

import java.util.*;

public class PlayerManager
{

	private static Player[] players;
	private ArrayList playerNames;
	private Player boughtStockLast;

	public static int getNumberOfPlayers()
	{
		return players.length;
	}

	private void initPlayers()
	{
		players = new Player[playerNames.size()];

		for (int i = 0; i < playerNames.size(); i++)
		{
			players[i] = new Player(playerNames.get(i).toString());
		}

		Player.initPlayers(players);
	}

	public PlayerManager(ArrayList playerNames)
	{
		this.playerNames = playerNames;

		initPlayers();
	}

	/**
	 * @return Returns the playerNames.
	 */
	public ArrayList getPlayerNames()
	{
		return playerNames;
	}

	/**
	 * @return Returns an array of all players.
	 */
	public Player[] getPlayersArray()
	{
		return players;
	}

	public ArrayList getPlayersArrayList()
	{
		ArrayList playersList = new ArrayList();

		for (int i = 0; i < players.length; i++)
		{
			playersList.add(players[i]);
		}

		return playersList;
	}

	public Player getPlayerByName(String name)
	{
		Player player;

		for (int i = 0; i < players.length; i++)
		{
			if (players[i].getName().equalsIgnoreCase(name))
			{
				return players[i];
			}
		}
		return null;
	}

	/**
	 * @return Returns the boughtStockLast.
	 */
	public Player getBoughtStockLast()
	{
		return boughtStockLast;
	}

	/**
	 * @param boughtStockLast
	 *            The boughtStockLast to set.
	 */
	public void setBoughtStockLast(Player boughtStockLast)
	{
		this.boughtStockLast = boughtStockLast;
	}
}
