using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    public class StopType
    {
        private static Logger<StopType> log = new Logger<StopType>();

        /**
          * StopType defines the characteristics of access to a stop
          */

        public enum RunThrough
        {
            YES,
            NO,
            TOKENONLY,
        }

        public enum RunTo
        {
            YES,
            NO,
            TOKENONLY,
        }

        public enum Loop
        {
            YES,
            NO,
        }

        public enum Score
        {
            MAJOR,
            MINOR,
        }

        public class Defaults
        {

            public static Defaults CITY = new Defaults("CITY", RunTo.YES, RunThrough.YES, Loop.YES, Score.MAJOR);
            public static Defaults TOWN = new Defaults("TOWN", RunTo.YES, RunThrough.YES, Loop.YES, Score.MINOR);
            public static Defaults OFFMAP = new Defaults("OFFMAP", RunTo.YES, RunThrough.NO, Loop.NO, Score.MAJOR);
            public static Defaults NULL = new Defaults("NULL", null, null, null, null);

            private StopType stopType;

            private Defaults(string name, RunTo? runTo,
                    RunThrough? runThrough,
                    Loop? loop,
                    Score? scoreType)
            {
                this.stopType = new StopType(name, runTo, runThrough, loop, scoreType);
            }

            public StopType StopType
            {
                get
                {
                    return stopType;
                }
            }
        }

        private string id;
        private RunTo? runToAllowed;
        private RunThrough? runThroughAllowed;
        private Loop? loopAllowed;
        private Score? scoreType;

        private StopType(string id, RunTo? runToAllowed, RunThrough? runThroughAllowed, Loop? loopAllowed, Score? scoreType)
        {
            this.id = id;
            this.runToAllowed = runToAllowed;
            this.runThroughAllowed = runThroughAllowed;
            this.loopAllowed = loopAllowed;
            this.scoreType = scoreType;
        }

        private StopType(string id, RunTo? runToAllowed, RunThrough? runThroughAllowed, Loop? loopAllowed, Score? scoreType, StopType defaultType)
        {
            this.id = id;

            if (defaultType == null)
            { // CITY is the ultimate default
                defaultType = Defaults.CITY.StopType;
            }
            if (defaultType.id == "NULL")
            {
                //throw new ArgumentException("StopType default cannot be NULL");
                log.Debug("StopType is NULL, assigning CITY type.");
                defaultType = Defaults.CITY.StopType;
            }

            //this.runToAllowed = (runToAllowed == RunTo.NULL) ? defaultType.RunToAllowed : runToAllowed;
            //this.runThroughAllowed = (runThroughAllowed == RunThrough.NULL) ? defaultType.RunThroughAllowed : runThroughAllowed;
            //this.loopAllowed = (loopAllowed == Loop.NULL) ? defaultType.LoopAllowed : loopAllowed;
            //this.scoreType = (scoreType == Score.NULL) ? defaultType.ScoreType : scoreType;
            this.runToAllowed = runToAllowed ?? defaultType.RunToAllowed;
            this.runThroughAllowed = runThroughAllowed ?? defaultType.RunThroughAllowed;
            this.loopAllowed = loopAllowed ?? defaultType.LoopAllowed;
            this.scoreType = scoreType ?? defaultType.ScoreType;
        }

        public string Id
        {
            get
            {
                return id;
            }
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
        public RunTo? RunToAllowed
        {
            get
            {
                return runToAllowed;
            }
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
        public RunThrough? RunThroughAllowed
        {
            get
            {
                return runThroughAllowed;
            }
        }

        /** 
         * Loop: may one train touch this hex twice or more? 
         * 
         * */
        public Loop? LoopAllowed
        {
            get
            {
                return loopAllowed;
            }
        }

        /**
         * Score type: do stops on this hex count as major or minor stops with respect to n+m trains?
         */
        public Score? ScoreType
        {
            get
            {
                return scoreType;
            }
        }

        override public int GetHashCode()
        {
            return id.GetHashCode();
        }

        override public bool Equals(object other)
        {
            if (!(other is StopType)) return false;
            return id.Equals(((StopType)other).id);
        }

        private static StopType ParseAccessTag(IRailsItem owner, string id, Tag accessTag, StopType defaultType)
        {

            string runThroughString = accessTag.GetAttributeAsString("runThrough");
            RunThrough runThrough = RunThrough.YES;
            if (!string.IsNullOrEmpty(runThroughString))
            {
                try
                {
                    runThrough = (RunThrough)Enum.Parse(typeof(RunThrough), runThroughString.ToUpper());
                }
                catch (ArgumentException e)
                {
                    throw new ConfigurationException("Illegal value for "
                            + owner + " runThrough property: " + runThroughString, e);
                }
            }

            string runToString = accessTag.GetAttributeAsString("runTo");
            RunTo runTo = RunTo.YES;
            if (!string.IsNullOrEmpty(runToString))
            {
                try
                {
                    runTo = (RunTo)Enum.Parse(typeof(RunTo), runToString.ToUpper());
                }
                catch (ArgumentException e)
                {
                    throw new ConfigurationException("Illegal value for "
                            + owner + " runTo property: " + runToString, e);
                }
            }

            string loopString = accessTag.GetAttributeAsString("loop");
            Loop loop = Loop.YES;
            if (!string.IsNullOrEmpty(loopString))
            {
                try
                {
                    loop = (Loop)Enum.Parse(typeof(Loop), loopString.ToUpper());
                }
                catch (ArgumentException e)
                {
                    throw new ConfigurationException("Illegal value for "
                            + owner + " loop property: " + loopString, e);
                }
            }

            string scoreTypeString = accessTag.GetAttributeAsString("score");
            Score score = Score.MAJOR;
            if (!string.IsNullOrEmpty(scoreTypeString))
            {
                try
                {
                    score = (Score)Enum.Parse(typeof(Score), scoreTypeString.ToUpper());
                }
                catch (ArgumentException e)
                {
                    throw new ConfigurationException("Illegal value for "
                            + owner + " score type property: " + scoreTypeString, e);
                }
            }

            return new StopType(id, runTo, runThrough, loop, score, defaultType);
        }

        private static StopType ParseDefault(IRailsItem owner, Tag accessTag, StopType defaultType)
        {
            string typeString = accessTag.GetAttributeAsString("type");
            string type = null; // If type is not defined the "default default" is defined
            if (!string.IsNullOrEmpty(typeString))
            {
                try
                {
                    type = typeString.ToUpper();
                }
                catch (ArgumentException e)
                {
                    throw new ConfigurationException("Illegal value for "
                            + owner + " stop type property: " + typeString, e);
                }
            }
            return ParseAccessTag(owner, type, accessTag, defaultType);
        }

        public static IReadOnlyDictionary<string, StopType> ParseDefaults(IRailsItem owner, List<Tag> accessTags)
        {

            // Parse default stop types, cannot use builder here as defaults might get overwritten
            Dictionary<string, StopType> stopTypeBuilder = new Dictionary<string, StopType>();
            // Initialize with system defaults
            //    foreach (StopType.Defaults defType in StopType.Defaults.Values) {
            //    stopTypeBuilder.put(defType.name(), defType.getStopType());
            //}

            // Initialize with system defaults
            stopTypeBuilder["CITY"] = Defaults.CITY.StopType;
            stopTypeBuilder["TOWN"] = Defaults.TOWN.StopType;
            stopTypeBuilder["OFFMAP"] = Defaults.OFFMAP.StopType;
            stopTypeBuilder["NULL"] = Defaults.NULL.StopType;

            StopType defaultDefault = StopType.Defaults.NULL.StopType;
            foreach (Tag accessTag in accessTags)
            {
                StopType newDefault = StopType.ParseDefault(owner, accessTag, defaultDefault);
                if (newDefault == null) continue;
                if (newDefault.Id == null)
                {
                    // id set to null is the default default
                    defaultDefault = newDefault;
                }
                else
                {
                    stopTypeBuilder[newDefault.Id] = newDefault;
                }
            }
            return stopTypeBuilder; //ImmutableMap.copyOf(stopTypeBuilder);
        }

        public static StopType ParseStop(IRailsItem owner, Tag accessTag, IReadOnlyDictionary<string, StopType> defaultTypes)
        {

            if (accessTag == null)
            {
                return StopType.Defaults.NULL.StopType;
            }

            string typeString = accessTag.GetAttributeAsString("type");

            StopType type = null;
            if (!string.IsNullOrEmpty(typeString))
            {
                try
                {
                    type = defaultTypes[typeString.ToUpper()];
                }
                catch (KeyNotFoundException e)
                {
                    throw new ConfigurationException("Illegal value for "
                            + owner + " stop type property: " + typeString, e);
                }
            }

            if (type == null) type = StopType.Defaults.NULL.StopType;
            return ParseAccessTag(owner, owner.Id, accessTag, type);
        }
    }
}
