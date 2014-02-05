package rails.game;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.util.Util;


public class StopType {

    /**
     * StopType defines the characteristics of access to a stop
     */

    public static enum RunThrough {
        YES,
        NO,
        TOKENONLY
    }

    public static enum RunTo {
        YES,
        NO,
        TOKENONLY
    }

    public static enum Loop {
        YES,
        NO
    }

    public static enum Score {
        MAJOR,
        MINOR
    }

    public static enum Defaults {

        CITY (RunTo.YES, RunThrough.YES, Loop.YES, Score.MAJOR),
        TOWN (RunTo.YES, RunThrough.YES, Loop.YES, Score.MINOR),
        OFFMAP (RunTo.YES, RunThrough.NO, Loop.NO, Score.MAJOR),
        NULL (null, null, null, null);

        private StopType stopType;
        
        private Defaults (RunTo runTo,
                RunThrough runThrough,
                Loop loop,
                Score scoreType) {
            this.stopType = new StopType(this.name(), runTo, runThrough, loop, scoreType);
        }

        public StopType getStopType() {
            return stopType;
        }
    }

    private final String id;
    private final RunTo runToAllowed;
    private final RunThrough runThroughAllowed;
    private final Loop loopAllowed;
    private final Score scoreType;
    
    private StopType(String id, RunTo runToAllowed, RunThrough runThroughAllowed, Loop loopAllowed, Score scoreType) {
        this.id = id;
        this.runToAllowed = runToAllowed;
        this.runThroughAllowed = runThroughAllowed;
        this.loopAllowed = loopAllowed;
        this.scoreType = scoreType;
    }

    private StopType(String id, RunTo runToAllowed, RunThrough runThroughAllowed, Loop loopAllowed, Score scoreType, StopType defaultType) {
        this.id = id;
        
        if (defaultType == null) { // CITY is the ultimate default
            defaultType = Defaults.CITY.getStopType();
        }
        
        this.runToAllowed = (runToAllowed == null) ? defaultType.getRunToAllowed() : runToAllowed;
        this.runThroughAllowed = (runThroughAllowed == null) ? defaultType.getRunThroughAllowed() : runThroughAllowed;
        this.loopAllowed = (loopAllowed == null) ? defaultType.getLoopAllowed() : loopAllowed;
        this.scoreType = (scoreType == null) ? defaultType.getScoreType() : scoreType;
    }

    public String getId() {
        return id;
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
    public RunTo getRunToAllowed() {
        return runToAllowed;
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
    public RunThrough getRunThroughAllowed() {
        return runThroughAllowed;
    }

    /** 
     * Loop: may one train touch this hex twice or more? 
     * 
     * */
    public Loop getLoopAllowed() {
        return loopAllowed;
    }

    /**
     * Score type: do stops on this hex count as major or minor stops with respect to n+m trains?
     */
    public Score getScoreType() {
        return scoreType;
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StopType)) return false;
        return id.equals(((StopType) other).id);
    }

    private static StopType parseAccessTag(RailsItem owner, String id, Tag accessTag, StopType defaultType) 
            throws ConfigurationException {

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

        String loopString = accessTag.getAttributeAsString("loop");
        Loop loop = null;
        if (Util.hasValue(loopString)) {
            try {
                loop = Loop.valueOf(loopString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for "
                        + owner +" loop property: "+loopString, e);
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

        return new StopType(id, runTo, runThrough, loop, score, defaultType);
    }

    private static StopType parseDefault(RailsItem owner, Tag accessTag, StopType defaultType) 
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
        return parseAccessTag(owner, type, accessTag, defaultType);
    }
    
    public static ImmutableMap<String, StopType> parseDefaults(RailsItem owner, List<Tag> accessTags)
        throws ConfigurationException {

        // Parse default stop types, cannot use builder here as defaults might get overwritten
        Map<String, StopType> stopTypeBuilder = Maps.newHashMap();
        // Initialize with system defaults
        for (StopType.Defaults defType: StopType.Defaults.values()) {
            stopTypeBuilder.put(defType.name(), defType.getStopType());
        }

        StopType defaultDefault = StopType.Defaults.NULL.getStopType();
        for (Tag accessTag : accessTags)  {
            StopType newDefault = StopType.parseDefault(owner, accessTag, defaultDefault);
            if (newDefault == null) continue;
            if (newDefault.getId() == null) {
                // id set to null is the default default
                defaultDefault = newDefault;
            } else {
                stopTypeBuilder.put(newDefault.getId(), newDefault);
            }
        }
        return ImmutableMap.copyOf(stopTypeBuilder);
    }
    
    public static StopType parseStop(RailsItem owner, Tag accessTag, Map<String, StopType> defaultTypes)
        throws ConfigurationException {
        
        if (accessTag == null) { 
            return StopType.Defaults.NULL.getStopType();
        }
        
        String typeString = accessTag.getAttributeAsString("type");

        StopType type = null;
        if (Util.hasValue(typeString)) {
            try {
                type = defaultTypes.get(typeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for "
                        + owner +" stop type property: "+typeString, e);
            }
        }
        
        if (type == null) type = StopType.Defaults.NULL.getStopType();
        return parseAccessTag(owner, owner.getId(), accessTag, type);
    }
}
        
