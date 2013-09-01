package rails.game;

import rails.common.parser.*;
import rails.util.Util;

public class Access {

    /** An Access object represents a set of parameters that may affect how trains can run through stops
     * and what revenue is scored.
     * The final repository of an Access objects is the class Stop, where all train-run affecting aspects
     * come together.<p>
     * Stop objects collect all of its Access parameters from Access objects
     * in other classes, as listed below, in precedence sequence:<br>
     * 1. Specific MapHex Stop (defined in TileSet.xml with 'stop' value > 0)<br>
     * 2. Specific Tile Station (defined in TileSet.xml with 'station' value > 0). For preprinted tiles (id<=0) only.<br>
     * 3. MapHex (defined in Map.xml with 'stop' value absent or 0)<br>
     * 4. Tile (defined in TileSet.xml with 'station' value absent or 0).  For preprinted tiles (id<=0) only.<br>
     * 5. MapManager default per stop type (defined as default in Map.xml)<br>
     * 6. TileManager default per stop type (defined as default in TileSet.xml)<br>
     * 7. MapManager general default (defined in Map.xml)<br>
     * 8. TileManager general default (defined in TileSet.xml)<br>
     * 9. Generic stop-type default (defined in the StopType enum below)<p>
     * The precedence rule is, that a parameter that is not explicitly defined at a certain precedence level
     * falls through to the next lower level, or ultimately to the stop-type default, as defined in this class.
     * <p>For some properties, not all precedence levels apply.
     */

    /** The stop type (city, town, port, mine etc.).
     * Normally the type will be derived from the tile properties.
     * <p>The stop type can be configured on precedence levels 1-4.
     * If still undefined after level 4, it is derived from the tile Station type.
     */
    private StopType stopType = null;

    /** Run-to status of any stops on the hex (whether visible or not).  CURRENTLY UNUSED.
     * Indicates whether or not a single train can run from or to such stops, i.e. either enter or leave it.
     * Has no meaning if no stops exist on this hex.
     * <p>Values (see RunTo below for definitions):
     * <br>- "yes" (default) means that trains of all companies may run to/from this station.
     * <br>- "tokenOnly" means that trains may only access the station if it contains a base token
     * of the operating company. Applies to the 18Scan off-map hexes.
     * <br>- "no" would mean that the hex is inaccessible (like 1851 Birmingham in the early game),
     * but this option is not yet useful as there is no provision yet to change this setting
     * in an undoable way (no state variable).
     * <p>The run-to status can be configured on all precedence levels.
     */
    private RunTo runToAllowed = null;

    /** Run-through status of any stops on the hex (whether visible or not).  CURRENTLY UNUSED.
     * Indicates whether or not a single train can run through such stops, i.e. both enter and leave it.
     * Has no meaning if no stops exist on this hex.
     * <p>Values (see RunThrough below for definitions):
     * <br>- "yes" (default for all except off-map hexes) means that trains of all companies
     * may run through this station, unless it is completely filled with foreign base tokens.
     * <br>- "tokenOnly" means that trains may only run through the station if it contains a base token
     * of the operating company (applies to the 1830 PRR base).
     * <br>- "no" (default for off-map hexes) means that no train may run through this hex.
     * <p>The run-through status can be configured on all precedence levels.
     */
    private RunThrough runThroughAllowed = null;

    /** May trains return to this tile? UNUSED, and probably obsolete.  See trainMutexID */
    private Loop loopAllowed = null;

    /**
     * Score type indicates whether stops on this hex count as 'major' or 'minor' stops.
     * Many games don't make such a difference, but for instance 1835 and 18EU do.
     * <p>The stop type can be configured on precedence levels 1-6.
     */
    private ScoreType scoreType = null;

    /** An identifier of a train mutex group. Can be any non-empty string.  CURRENTLY UNUSED.<p>
     * One train may not access more than one stop with the same trainMutexID
     * (at least, that is the intention of this item. It is not yet used).
     * <p> The trainMutexID can be set on precedence levels 1-6. The default is null.
     * */
    private String trainMutexID = null;

    public Access () {}

    public Access (Access copy) {
        this.stopType = copy.stopType;
        this.runToAllowed = copy.runToAllowed;
        this.runThroughAllowed = copy.runThroughAllowed;
        this.loopAllowed = copy.loopAllowed;
        this.scoreType = copy.scoreType;
        this.trainMutexID = copy.trainMutexID;
    }

    public Access merge (Access lower) {

        if (lower != null) {
            if (stopType == null) stopType = lower.stopType;
            if (runToAllowed == null) runToAllowed = lower.runToAllowed;
            if (runThroughAllowed == null) runThroughAllowed = lower.runThroughAllowed;
            if (loopAllowed == null) loopAllowed = lower.loopAllowed;
            if (scoreType == null) scoreType = lower.scoreType;
            if (!Util.hasValue(trainMutexID)) trainMutexID = lower.trainMutexID;
        }

        return this;
    }

    public static StopType parseStopTypeString (String s, String sourceDescription)
    throws ConfigurationException {
        if (Util.hasValue(s)) {
            try {
                return StopType.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for " + sourceDescription
                        +" type property: '"+s+"'", e);
            }
        }
        return null;
    }

    public static RunThrough parseRunThroughString (String s, String sourceDescription)
    throws ConfigurationException {
        if (Util.hasValue(s)) {
            try {
                return RunThrough.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for " + sourceDescription
                        +" runThrough property: '"+s+"'", e);
            }
        }
        return null;
    }

    public static RunTo parseRunToString (String s, String sourceDescription)
    throws ConfigurationException {
        if (Util.hasValue(s)) {
            try {
                return RunTo.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for " + sourceDescription
                        +" runTo property: '"+s+"'", e);
            }
        }
        return null;
    }

    public static Loop parseLoopString (String s, String sourceDescription)
    throws ConfigurationException {
        if (Util.hasValue(s)) {
            try {
                return Loop.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for " + sourceDescription
                        +" loop property: "+s, e);
            }
        }
        return null;
    }

    public static ScoreType parseScoreTypeString (String s, String sourceDescription)
    throws ConfigurationException {
        if (Util.hasValue(s)) {
            try {
                return ScoreType.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for " + sourceDescription
                        +" score property: "+s, e);
            }
        }
        return null;
    }

    public StopType getStopType() {
        return stopType;
    }

    public void setStopType(StopType stopType) {
        this.stopType = stopType;
    }

    public RunTo getRunToAllowed() {
        return runToAllowed;
    }

    public void setRunToAllowed(RunTo runToAllowed) {
        this.runToAllowed = runToAllowed;
    }

    public RunThrough getRunThroughAllowed() {
        return runThroughAllowed;
    }

    public void setRunThroughAllowed(RunThrough runThroughAllowed) {
        this.runThroughAllowed = runThroughAllowed;
    }

    public Loop getLoopAllowed() {
        return loopAllowed;
    }

    public void setLoopAllowed(Loop loopAllowed) {
        this.loopAllowed = loopAllowed;
    }

    public ScoreType getScoreType() {
        return scoreType;
    }

    public void setScoreType(ScoreType scoreType) {
        this.scoreType = scoreType;
    }

    public String getTrainMutexID() {
        return trainMutexID;
    }

    public void setTrainMutexID(String trainMutexID) {
        this.trainMutexID = trainMutexID;
    }

    // --- Enumerations ---
    public enum RunTo {
        YES,
        NO,
        TOKENONLY
    }

    public enum RunThrough {
        YES,
        NO,
        TOKENONLY
    }

    public enum Loop {
        YES,
        NO
    }

    public enum StopType {

        CITY (RunTo.YES, RunThrough.YES, Loop.YES, ScoreType.MAJOR),
        TOWN (RunTo.YES, RunThrough.YES, Loop.YES, ScoreType.MINOR),
        HALT (RunTo.YES, RunThrough.YES, Loop.YES, ScoreType.MINOR),
        OFFMAP (RunTo.YES, RunThrough.NO, Loop.NO, ScoreType.MAJOR),
        PORT (RunTo.YES, RunThrough.NO, Loop.NO, ScoreType.MINOR),
        MINE (RunTo.YES, RunThrough.NO, Loop.NO, ScoreType.MINOR),
        PASS (RunTo.YES, RunThrough.YES, Loop.NO, ScoreType.MAJOR);

        private RunTo defaultRunToAllowed;
        private RunThrough defaultRunThroughAllowed;
        private Loop defaultLoopAllowed;
        private ScoreType defaultScoreType;
        private Access defaultAccessInfo;

        StopType (RunTo runTo,
                RunThrough runThrough,
                Loop loop,
                ScoreType scoreType) {

            defaultAccessInfo = new Access();

            defaultAccessInfo.setRunToAllowed(runTo);
            defaultAccessInfo.setRunThroughAllowed(runThrough);
            defaultAccessInfo.setLoopAllowed(loop);
            defaultAccessInfo.setScoreType(scoreType);
        }

        public RunTo getDefaultRunTo() { return defaultRunToAllowed; }
        public RunThrough getDefaultRunThrough() { return defaultRunThroughAllowed; }
        public Loop getDefaultLoop() { return defaultLoopAllowed; }
        public ScoreType getDefaultScoreType() { return defaultScoreType; }

        public Access getAccessInfoDefaults() { return defaultAccessInfo; }

    }

    public enum ScoreType {
        MAJOR,
        MINOR
    }
}
