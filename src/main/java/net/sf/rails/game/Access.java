package net.sf.rails.game;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.util.Util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;



public class Access {

    public enum RunThrough {
        YES,
        NO,
        TOKENONLY
    }

    public enum RunTo {
        YES,
        NO,
        TOKENONLY
    }

    public enum Score {
        MAJOR,
        MINOR,
        NO
    }

    private String id;
    private RunTo runToAllowed;
    private RunThrough runThroughAllowed;
    private Score scoreType;
    private String mutexId;
    private String typeName;
    private Stop.Type type;
    
    private Access(String id, Stop.Type type, RunTo runToAllowed, RunThrough runThroughAllowed,
                   String mutexId, Score scoreType, String typeName) {
        this.id = id;
        this.runToAllowed = runToAllowed;
        this.runThroughAllowed = runThroughAllowed;
        this.scoreType = scoreType;
        this.mutexId = mutexId;
        this.typeName = typeName;
        this.type = type;

    }

    private Access(RunTo runToAllowed, RunThrough runThroughAllowed,
                   String mutexId, Score scoreType) {
        this.runToAllowed = runToAllowed;
        this.runThroughAllowed = runThroughAllowed;
        this.scoreType = scoreType;
        this.mutexId = mutexId;

        this.id=null;
        this.typeName=null;
        this.type=null;


    }

    public static EnumMap<Stop.Type, Access> defaults = new EnumMap<>(Stop.Type.class);

    /*
     * Generic defaults.
     * Different per-game defaults can best be set in Map.xml.
     */
    static {
        defaults.put(Stop.Type.CITY, new Access (RunTo.YES, RunThrough.YES, null,Score.MAJOR));
        defaults.put(Stop.Type.TOWN, new Access (RunTo.YES, RunThrough.YES, null,Score.MINOR));
        defaults.put(Stop.Type.OFFMAP, new Access (RunTo.YES, RunThrough.NO, null, Score.MAJOR));
        defaults.put(Stop.Type.MINE, new Access (RunTo.NO, RunThrough.NO, null, Score.MINOR));
        defaults.put(Stop.Type.PORT, new Access (RunTo.YES, RunThrough.NO, null, Score.MINOR));
        defaults.put(Stop.Type.PASS, new Access (RunTo.YES, RunThrough.YES, null, Score.NO));
    }

    public static Access getDefault(Stop.Type type) {
        return defaults.get (type);
    }

    public Stop.Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    /** Run-to status of any stops on the hex (whether visible or not).
     * Indicates whether or not a single train can run from or to such stops, i.e. either enter or leave it.
     * Has no meaning if no stops exist on this hex.
     * <p>Values (see RunTo below for definitions):
     * <br>- "yes" (default) means that trains of all companies may run to/from this station.
     * <br>- "tokenOnly" means that trains may only access the station if it contains a base token
     * of the operating company. Applies to the 18Scan off-map hexes.
     * <br>- "no" would mean that the hex is inaccessible (like 1851 Birmingham in the early game),
     * but this option is not yet useful as there is no provision yet to change this setting
     * in an undoable way (no state variable).
     */
    public RunTo getRunToAllowed() {
        return runToAllowed;
    }

    /** Run-through status of any stops on the hex (whether visible or not).
     * Indicates whether or not a single train can run through such stops, i.e. both enter and leave it.
     * Has no meaning if no stops exist on this hex.
     * <p>Values (see RunThrough below for definitions):
     * <br>- "yes" (default for all except off-map hexes) means that trains of all companies
     * may run through this station, unless it is completely filled with foreign base tokens.
     * <br>- "tokenOnly" means that trains may only run through the station if it contains a base token
     * of the operating company (applies to the 1830 PRR base).
     * <br>- "no" (default for off-map hexes) means that no train may run through this hex.
     */
    public RunThrough getRunThroughAllowed() {
        return runThroughAllowed;
    }

    /**
     * Score type: do stops on this hex count as major or minor stops with respect to n+m trains?
     * Major usually refers to cities and offmap areas
     * Minor usually refers to towns and ports
     * >>> Note: added "no", e.g. for 1841 pass, but perhaps redundant as revenue=0 does the same thing.
     */
    public Score getScoreType() {
        return scoreType;
    }

    /** MutexId: a train may never hit two stops with the same mutexId value.
     * Useful for offmap areas with multiple entrances,
     * and to prevent looping to different stations on one tile (e.g. Berlin in 1835 and 18EU).
     * Default is null, except for offmap hexes, where the city name is still used, but deprecated.
     * @return The mutexId
     */
    public String getMutexId() {
        return mutexId;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Access)) return false;
        return id.equals(((Access) other).id);
    }

    public static Access parseAccessTag(RailsItem owner, String id, Tag accessTag)
            throws ConfigurationException {

        String typeName = accessTag.getAttributeAsString("type");
        if ("OffMapCity".equalsIgnoreCase(typeName)) typeName = "OffMap"; //Can also be a town
        Stop.Type type = null;
        if (Util.hasValue(typeName)) {
            try {
                type = Stop.Type.valueOf(typeName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for "
                        + owner +" typeName property: "+typeName, e);
            }
        }


        String runThroughString = accessTag.getAttributeAsString("runThrough");
        RunThrough runThrough = null;
        if (Util.hasValue(runThroughString)) {
            try {
                runThrough = RunThrough.valueOf(runThroughString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for "
                        + owner +" runThrough property: "+runThroughString, e);
            }
        }

        String runToString = accessTag.getAttributeAsString("runTo");
        RunTo runTo = null;
        if (Util.hasValue(runToString)) {
            try {
                runTo = RunTo.valueOf(runToString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for "
                        + owner +" runTo property: "+runToString, e);
            }
        }

        String scoreTypeString = accessTag.getAttributeAsString("score");
        Score score = null;
        if (Util.hasValue(scoreTypeString)) {
            try {
                score = Score.valueOf(scoreTypeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for "
                        + owner +" score type property: "+scoreTypeString, e);
            }
        }

        String mutexId = accessTag.getAttributeAsString("mutexId", null);

        return new Access(id, type, runTo, runThrough, mutexId, score, typeName);
    }

    public static Access parseDefault(RailsItem owner, Tag accessTag)
        throws ConfigurationException {
        
        String typeString = accessTag.getAttributeAsString("type");
        String type = null; // If type is not defined the "default default" is defined
        if (Util.hasValue(typeString)) {
            try {
                type = typeString.toUpperCase();
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for "
                        + owner +" stop type property: "+typeString, e);
            }
        }
        return parseAccessTag(owner, type, accessTag);
    }
    
    public static EnumMap<Stop.Type, Access> parseDefaults(RailsItem owner, List<Tag> accessTags)
        throws ConfigurationException {

        EnumMap<Stop.Type, Access> defaults = new EnumMap<>(Stop.Type.class);
        for (Tag accessTag : accessTags)  {
            Access newDefault = Access.parseDefault(owner, accessTag);
            defaults.put (newDefault.getType(), newDefault);
        }
        return defaults;
    }
    
    public String toString() {
        return "StopType id="+id+" runTo="+runToAllowed+" runThrough="+runThroughAllowed
                +" mutexId="+mutexId+" scoreType="+scoreType;
    }

}
        
