/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TileI.java,v 1.13 2008/11/20 21:49:38 evos Exp $ */
package rails.game;

import java.util.List;
import java.util.Map;

import rails.util.Tag;

public interface TileI {

    public void configureFromXML(Tag se, Tag te) throws ConfigurationException;

    public String getColourName();

    public int getColourNumber();

    /**
     * @return Returns the id.
     */
    public int getId();

    public int getExternalId();

    public int getPictureId();

    /**
     * @return Returns the name.
     */
    public String getName();

    public boolean hasTracks(int sideNumber);

    public List<Track> getTracksPerSide(int sideNumber);

    public Map<Integer, List<Track>> getTracksPerStationMap();

    public List<Track> getTracksPerStation(int stationNumber);

    public boolean isUpgradeable();

    public List<TileI> getUpgrades(MapHex hex, PhaseI phase);

    public List<TileI> getValidUpgrades(MapHex hex, PhaseI phase);

    public String getUpgradesString(MapHex hex);

    public boolean hasStations();

    public List<Station> getStations();

    public int getNumStations();

    public boolean lay(MapHex hex);

    public boolean remove(MapHex hex);

    public int countFreeTiles();

    public int getQuantity();
}
