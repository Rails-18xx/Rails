/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Station.java,v 1.4 2005/12/27 20:34:22 wakko666 Exp $
 * 
 * Created on 30-Oct-2005
 * Change Log:
 */
package game;

import java.util.*;

/**
 * @author Erik Vos
 */
public class Station implements TokenHolderI, Cloneable
{

	private String id;
	private String type;
	private int value;
	private int baseSlots;
	private Track[] tracks;
	private ArrayList tokens;
	private boolean hasTokens = false;

	public Station(String id, String type, int value)
	{
		this(id, type, value, 0);
	}

	public Station(String id, String type, int value, int slots)
	{
		this.id = id;
		this.type = type;
		this.value = value;
		this.baseSlots = slots;
		
		tokens = new ArrayList();
	}

	/**
	 * @return Returns the id.
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * @return Returns the type.
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * @return Returns the baseSlots.
	 */
	public int getBaseSlots()
	{
		return baseSlots;
	}

	/**
	 * @return Returns the tracks.
	 */
	public Track[] getTracks()
	{
		return tracks;
	}

	/**
	 * @return Returns the value.
	 */
	public int getValue()
	{
		return value;
	}

	public boolean addToken(CompanyI company)
	{
		tokens.add(company);
		hasTokens = true;
		return true;
	}

	public List getTokens()
	{
		return tokens;
	}

	public boolean hasTokens()
	{
		return hasTokens;
	}

	public boolean removeToken(CompanyI company)
	{
		int index = tokens.indexOf(company);
		if (index >= 0)
		{
			tokens.remove(index);

			if (tokens.size() < 1)
				hasTokens = false;

			return true;
		}
		else
			return false;
	}
}
