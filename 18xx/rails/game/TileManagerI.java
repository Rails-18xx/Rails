/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Attic/TileManagerI.java,v 1.4 2008/06/04 19:00:30 evos Exp $ */
package rails.game;

import java.util.List;

/**
 * Interface for CompanyManager objects. A company manager is a factory which
 * vends Company objects.
 */
public interface TileManagerI {
    /**
     * This is the name by which the TileManager should be registered with the
     * ComponentManager.
     */
    static final String COMPONENT_NAME = "TileManager";

    public TileI getTile(int id);

    public List<Integer> getTileIds();

}
