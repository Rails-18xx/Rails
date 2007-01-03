/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/TileMove.java,v 1.2 2007/01/03 22:34:18 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.move;

import java.util.List;

import game.*;

/**
 * @author Erik Vos
 */
public class TileMove extends Move {
    
    MapHex hex;
    TileI oldTile;
    int oldTileOrientation;
    List oldStations;
    TileI newTile;
    int newTileOrientation;
    List newStations;
    
    public TileMove (MapHex hex, TileI oldTile, int oldTileOrientation, List oldStations,
            TileI newTile, int newTileOrientation, List newStations) {
        
        this.hex = hex;
        this.oldTile = oldTile;
        this.oldTileOrientation = oldTileOrientation;
        this.oldStations = oldStations;
        this.newTile = newTile;
        this.newTileOrientation = newTileOrientation;
        this.newStations = newStations;
        System.out.println("TileMove hex "+hex.getName()
                +" from "+oldTile.getName()+"/"+oldTileOrientation
                +" to "+newTile.getName()+"/"+newTileOrientation);
    }


    public boolean execute() {

        hex.replaceTile (oldTile, newTile, newTileOrientation, newStations);
        return true;
    }

    public boolean undo() {
        
        hex.replaceTile (newTile, oldTile, oldTileOrientation, oldStations);
        return true;
    }
    
    public String toString() {
        return "TileMove: "+hex.getName()
        	+ ", old tile:#" + oldTile.getName()
        	+ ", new tile:#" + newTile.getName();
   }

}
