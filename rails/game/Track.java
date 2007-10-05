/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Track.java,v 1.2 2007/10/05 22:02:27 evos Exp $ */
package rails.game;

/**
 * Represents a piece of track on one tile.
 * <p>
 * Endpoints can be: <br>- a hex side (number >= 0), or <br>- a station
 * (number < 0)
 */
public class Track
{

	int startPoint;
	int endPoint;

	public Track(int startPoint, int endPoint)
	{
		this.startPoint = startPoint;
		this.endPoint = endPoint;
	}

	public boolean hasPoint(int point)
	{
		return startPoint == point || endPoint == point;
	}

	public int[] points()
	{
		return new int[] { startPoint, endPoint };
	}

}
