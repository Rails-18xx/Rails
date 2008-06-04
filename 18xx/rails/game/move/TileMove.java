/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/TileMove.java,v 1.8 2008/06/04 19:00:33 evos Exp $
 *
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.List;

import rails.game.*;

/**
 * @author Erik Vos
 */
public class TileMove extends Move {

    MapHex hex;
    TileI oldTile;
    int oldTileOrientation;
    List<City> oldStations;
    TileI newTile;
    int newTileOrientation;
    List<City> newStations;

    public TileMove(MapHex hex, TileI oldTile, int oldTileOrientation,
            List<City> oldStations, TileI newTile, int newTileOrientation,
            List<City> newStations) {

        this.hex = hex;
        this.oldTile = oldTile;
        this.oldTileOrientation = oldTileOrientation;
        this.oldStations = oldStations;
        this.newTile = newTile;
        this.newTileOrientation = newTileOrientation;
        this.newStations = newStations;

        MoveSet.add(this);
    }

    @Override
    public boolean execute() {

        hex.replaceTile(oldTile, newTile, newTileOrientation, newStations);
        return true;
    }

    @Override
    public boolean undo() {

        hex.replaceTile(newTile, oldTile, oldTileOrientation, oldStations);
        log.debug("-Undone: " + toString());
        return true;
    }

    @Override
    public String toString() {
        return "TileMove: hex " + hex.getName() + " from #" + oldTile.getId()
               + "/" + oldTileOrientation + " to #" + newTile.getId() + "/"
               + newTileOrientation;
    }

}
