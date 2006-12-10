/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/TileMove.java,v 1.1 2006/12/10 20:42:00 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.move;

import game.*;

/**
 * @author Erik Vos
 */
public class TileMove extends Move {
    
    MapHex hex;
    TileI oldTile;
    int oldTileOrientation;
    TileI newTile;
    int newTileOrientation;
    
    public TileMove (MapHex hex, TileI oldTile, int oldTileOrientation,
            TileI newTile, int newTileOrientation) {
        
        this.hex = hex;
        this.oldTile = oldTile;
        this.oldTileOrientation = oldTileOrientation;
        this.newTile = newTile;
        this.newTileOrientation = newTileOrientation;
        System.out.println("TileMove hex "+hex.getName()
                +" from "+oldTile.getName()+"/"+oldTileOrientation
                +" to "+newTile.getName()+"/"+newTileOrientation);
    }


    public boolean execute() {

        hex.replaceTile (oldTile, newTile, newTileOrientation);
        return true;
    }

    public boolean undo() {
        
        hex.replaceTile (newTile, oldTile, oldTileOrientation);
        return true;
    }
    
    public String toString() {
        return "TileMove: "+hex.getName()
        	+ ", old tile:#" + oldTile.getName()
        	+ ", new tile:#" + newTile.getName();
   }

}
