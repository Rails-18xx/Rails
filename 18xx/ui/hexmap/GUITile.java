package ui.hexmap;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import ui.ImageLoader;
import game.*;

/**
 * This class represents the GUI version of a tile.
 */
public class GUITile
{
	protected int tileId;
	protected TileI tile = null;
	protected BufferedImage tileImage = null;
	protected int rotation = 0;
	protected double tileScale = GUIHex.NORMAL_SCALE;
	protected static double baseRotation;
	protected static boolean initialised = false;
	protected MapHex hex = null;
	protected static ImageLoader imageLoader = new ImageLoader();
	protected AffineTransform af = new AffineTransform();
	public static final double DEG60 = Math.PI / 3;

	public GUITile(int tileId, MapHex hex)
	{
		this.tileId = tileId;
		this.hex = hex;
		tile = TileManager.get().getTile(tileId);
		tileImage = imageLoader.getTile(tileId);

		if (!initialised)
			initialise();

		// Find a correct initial orientation.
		//rotate(0);
		// Moved to GUIHex, as a constructor cannot return a boolean. 
	}

	private void initialise()
	{
		if (MapManager.getTileOrientation() == MapHex.EW)
		{
			baseRotation = 0.5 * DEG60;
		}
		else
		{
			baseRotation = 0.0;
		}
		initialised = true;
	}

	public void setRotation(int rotation)
	{
		this.rotation = rotation % 6;
	}

	/**
	 * Rotate right (clockwise) until a valid orientation is found. TODO:
	 * Currently only impassable hex sides are taken into account.
	 * 
	 * @param initial:
	 *            First rotation to try. Should be 0 for the initial tile drop,
	 *            and 1 at subsequent rotation attempts.
	 * @return <b>false</b> if no valid rotation exists (i.e. the tile cannot be laid).
	 */
	public boolean rotate(int initial, GUITile previousGUITile)
	{
		int i, j, tempRot;
		TileI prevTile = previousGUITile.getTile();
		int prevTileRotation = previousGUITile.getRotation();

		/* Loop through all possible rotations */
		for (i = initial; i < 6; i++)
		{
			tempRot = (rotation + i) % 6;
			/* Loop through all hex sides */
			for (j = 0; j < 6; j++)
			{
				/*
				 * If the tile has tracks against that side, but there is no
				 * neighbour, forbid this rotation.
				 */
				if (tile.hasTracks(j - tempRot)) {
				    if (!hex.hasNeighbour(j)) 	break;
				/*
				 * If the previous tile has tracks against that side, but the new
				 * one has not, forbid this rotation (not preserving existing track).
				 */
				} else {
				    if (prevTile.hasTracks(j - prevTileRotation)) break;
				    // TODO: Add a check for preserving station connections on multi-station tiles.
				}
			}
			if (j == 6)
			{
				/*
				 * If we have successfully checked all hex sides, we have found
				 * a valid rotation, so stop here.
				 */
				setRotation(tempRot);
				return true;
			}
		}
		if (i == 6) {
		    /* 
		     * If we have rotated six times, no valid rotation has been found. 
		     */
		    return false;
		} else {
		    return true;
		}
	}

	public int getRotation()
	{
		return rotation;
	}

	public void setScale(double scale)
	{
		tileScale = scale;
	}

	public void paintTile(Graphics2D g2, int x, int y)
	{
		if (tileImage != null)
		{
			double radians = baseRotation + rotation * DEG60;
			int xCenter = (int) Math.round(tileImage.getWidth() * 0.5
					* tileScale);
			int yCenter = (int) Math.round(tileImage.getHeight() * 0.5
					* tileScale);
			af = AffineTransform.getRotateInstance(radians, xCenter, yCenter);
			af.scale(tileScale, tileScale);

			// All adjustments to AffineTransform must be done before being
			// assigned to the ATOp here.
			AffineTransformOp aop = new AffineTransformOp(af,
					AffineTransformOp.TYPE_BILINEAR);

			g2.drawImage(tileImage, aop, x - xCenter, y - yCenter);
		}
	}

	public TileI getTile()
	{
		return tile;
	}

	public int getTileId()
	{
		return tileId;
	}

}
