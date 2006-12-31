/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/BaseTokenMove.java,v 1.1 2006/12/31 16:55:13 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.move;

import game.*;

/**
 * @author Erik Vos
 */
public class BaseTokenMove extends Move {
    
    MapHex hex;
    Station station;
    PublicCompanyI company;
    int newTileOrientation;
    
    public BaseTokenMove (MapHex hex, Station station, PublicCompanyI company) {
        
        this.hex = hex;
        this.station = station;
        this.company = company;
        System.out.println(toString());
    }


    public boolean execute() {

        //hex.replaceTile (oldTile, newTile, newTileOrientation);
        return true;
    }

    public boolean undo() {
        
       //hex.replaceTile (newTile, oldTile, oldTileOrientation);
        return true;
    }
    
    public String toString() {
        return "BaseTokenMove: "+hex.getName()
        	+ ", station:" + station.getId()
        	+ ", company:" + company.getName();
   }

}
