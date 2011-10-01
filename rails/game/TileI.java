/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TileI.java,v 1.21 2010/05/29 09:38:58 stefanfrey Exp $ */
package rails.game;

import java.util.List;
import java.util.Map;

import rails.algorithms.RevenueBonusTemplate;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.Stop.Loop;
import rails.game.Stop.RunThrough;
import rails.game.Stop.RunTo;
import rails.game.Stop.Score;
import rails.game.Stop.Type;
import rails.game.model.Model;

public interface TileI extends Model<String> {

    public void configureFromXML(Tag se, Tag te) throws ConfigurationException;

    public void finishConfiguration (TileManager tileManager)
    throws ConfigurationException;

    public String getColourName();

    public int getColourNumber();

    /**
     * @return Returns the id.
     */
    public int getNb();

    public String getExternalId();

    public int getPictureId();

    /**
     * @return Returns the name.
     */
    public String getId();

    public boolean hasTracks(int sideNumber);

    public List<Track> getTracks();

    public List<Track> getTracksPerSide(int sideNumber);

    public Map<Integer, List<Track>> getTracksPerStationMap();

    public List<Track> getTracksPerStation(int stationNumber);

    public boolean isUpgradeable();

    public boolean allowsMultipleBasesOfOneCompany();

    public List<TileI> getUpgrades(MapHex hex, Phase phase);

    public List<TileI> getAllUpgrades(MapHex hex);

    public List<TileI> getValidUpgrades(MapHex hex, Phase phase);

    public String getUpgradesString(MapHex hex);

    public boolean relayBaseTokensOnUpgrade();

    public boolean hasStations();

    public List<Station> getStations();

    public int getNumStations();

    public boolean add(MapHex hex);

    public boolean remove(MapHex hex);

    public int countFreeTiles();

    public int getQuantity();
    public int getFixedOrientation ();

    public List<RevenueBonusTemplate> getRevenueBonuses();

    public Type getStopType();
    public RunTo isRunToAllowed();
    public RunThrough isRunThroughAllowed();
    public Loop isLoopAllowed();
    public Score getScoreType();

    public TileManager getTileManager();

}
