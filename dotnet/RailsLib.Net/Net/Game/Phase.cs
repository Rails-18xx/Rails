using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

namespace GameLib.Net.Game
{
    public class Phase : RailsModel, IConfigurable
    {
        private static Logger<Phase> log = new Logger<Phase>();

        // static data
        private int index;

        private string name;
        private IReadOnlyList<string> tileColors;
        private Dictionary<string, int> tileLaysPerColor;

        /** For how many turns can extra tiles be laid (per company type and color)?
         * Default: infinite.
         * <p>This attribute is only used during configuration. It is finally passed to CompanyType.
         * NOT CLONED from previous phase.*/
        private Dictionary<string, int> tileLaysPerColourTurns;
        private bool privateSellingAllowed = false;
        private bool privatesClose = false;
        private int numberOfOperatingRounds = 1;
        private int offBoardRevenueStep = 1;

        /** New style train limit configuration.
         */
        private int trainLimitStep = 1;

        private int privatesRevenueStep = 1; // sfy 1889

        private bool trainTradingAllowed = false;

        /** May company buy more than one Train from the Bank per turn? */
        private bool oneTrainPerTurn = false;

        /** May company buy more than one Train of each type from the Bank per turn? */
        private bool oneTrainPerTypePerTurn = false;

        /** Is loan taking allowed */
        private bool loanTakingAllowed = false;

        /** Previous phase, defining the current one's defaults */
        private Phase defaults = null;

        /** Items to close if a phase gets activated */
        private List<ICloseable> closedObjects;

        /** Train types to rust or obsolete if a phase gets activated */
        private IReadOnlyList<TrainCertificateType> rustedTrains;
        private string rustedTrainNames;

        /** Train types to release (make available for buying) if a phase gets activated */
        private IReadOnlyList<TrainCertificateType> releasedTrains;
        private string releasedTrainNames;

        /** Actions for this phase.
         * When this phase is activated, the GameManager method phaseAction() will be called,
         * which in turn will call the current Round, which is responsible to handle the action.
         * <p>
         * Set actions have a name and may have a value. 
         * TODO: Replace this by triggers
         * */
        private Dictionary<string, string> actions;

        private string extraInfo = "";

        /** A HashMap to contain phase-dependent parameters
         * by name and value.
         */
        private Dictionary<string, string> parameters = null;

        // dynamic information
        // is this really dynamic, is it used over time?
        private GenericState<IOwner> lastTrainBuyer;

        public Phase(PhaseManager parent, string id, int index, Phase previousPhase) : base(parent, id)
        {
            lastTrainBuyer = GenericState<IOwner>.Create(this, "lastTrainBuyer");
            this.index = index;
            this.defaults = previousPhase;
        }

        public void ConfigureFromXML(Tag tag)
        {
            if (defaults != null)
            {
                tileColors = defaults.tileColors;
                tileLaysPerColor = defaults.tileLaysPerColor;
                privateSellingAllowed = defaults.privateSellingAllowed;
                numberOfOperatingRounds = defaults.numberOfOperatingRounds;
                offBoardRevenueStep = defaults.offBoardRevenueStep;
                trainLimitStep = defaults.trainLimitStep;
                privatesRevenueStep = defaults.privatesRevenueStep;
                trainTradingAllowed = defaults.trainTradingAllowed;
                oneTrainPerTurn = defaults.oneTrainPerTurn;
                oneTrainPerTypePerTurn = defaults.oneTrainPerTypePerTurn;
                loanTakingAllowed = defaults.loanTakingAllowed;
                if (defaults.parameters != null)
                {
                    parameters = new Dictionary<string, string>(defaults.parameters); //ImmutableMap.copyOf(defaults.parameters);
                }
            }

            // Real name (as in the printed game)
            name = tag.GetAttributeAsString("realName", null);

            // Allowed tile colors
            Tag tilesTag = tag.GetChild("Tiles");
            if (tilesTag != null)
            {
                string colorList = tilesTag.GetAttributeAsString("color", null);
                if (!string.IsNullOrEmpty(colorList))
                {
                    tileColors = colorList.Split(',');
                }

                List<Tag> laysTag = tilesTag.GetChildren("Lays");
                if (laysTag != null && (laysTag.Count > 0))
                {
                    // First create a copy of the previous map, if it exists, otherwise create the map.
                    Dictionary<string, int> newTileLaysPerColor;
                    if (tileLaysPerColor == null || (tileLaysPerColor.Count == 0))
                    {
                        newTileLaysPerColor = new Dictionary<string, int>();
                    }
                    else
                    {
                        newTileLaysPerColor = new Dictionary<string, int>(tileLaysPerColor);
                    }

                    Dictionary<string, int> newTileLaysPerColourTurns = null;
                    foreach (Tag layTag in laysTag)
                    {
                        string colorString = layTag.GetAttributeAsString("color");
                        if (string.IsNullOrEmpty(colorString))
                        {
                            throw new ConfigurationException("No color entry for number of tile lays");
                        }
                        string typeString = layTag.GetAttributeAsString("companyType");
                        if (string.IsNullOrEmpty(typeString))
                        {
                            throw new ConfigurationException("No company type entry for number of tile lays");
                        }
                        int number = layTag.GetAttributeAsInteger("number", 1);
                        int validForTurns = layTag.GetAttributeAsInteger("occurrences", 0);

                        string key = typeString + "~" + colorString;
                        if (number == 1)
                        {
                            newTileLaysPerColor.Remove(key);
                        }
                        else
                        {
                            newTileLaysPerColor[key] = number;
                        }

                        if (validForTurns != 0)
                        {
                            if (newTileLaysPerColourTurns == null)
                            {
                                newTileLaysPerColourTurns = new Dictionary<string, int>();
                            }
                            newTileLaysPerColourTurns[key] = validForTurns;
                        }
                    }
                    tileLaysPerColor = newTileLaysPerColor;
                    if (newTileLaysPerColourTurns != null)
                    {
                        tileLaysPerColourTurns = newTileLaysPerColourTurns;
                    }
                }
            }

            // Private-related properties
            Tag privatesTag = tag.GetChild("Privates");
            if (privatesTag != null)
            {
                privateSellingAllowed =
                    privatesTag.GetAttributeAsBoolean("sellingAllowed", privateSellingAllowed);
                privatesClose = privatesTag.GetAttributeAsBoolean("close", false);
                privatesRevenueStep = privatesTag.GetAttributeAsInteger("revenueStep", privatesRevenueStep); // sfy 1889
            }

            // Operating rounds
            Tag orTag = tag.GetChild("OperatingRounds");
            if (orTag != null)
            {
                numberOfOperatingRounds = orTag.GetAttributeAsInteger("number", numberOfOperatingRounds);
            }

            // Off-board revenue steps (starts at 1)
            Tag offBoardTag = tag.GetChild("OffBoardRevenue");
            if (offBoardTag != null)
            {
                offBoardRevenueStep = offBoardTag.GetAttributeAsInteger("step", offBoardRevenueStep);
            }

            Tag trainsTag = tag.GetChild("Trains");
            if (trainsTag != null)
            {
                trainLimitStep = trainsTag.GetAttributeAsInteger("limitStep", trainLimitStep);
                rustedTrainNames = trainsTag.GetAttributeAsString("rusted", null);
                releasedTrainNames = trainsTag.GetAttributeAsString("released", null);
                trainTradingAllowed = trainsTag.GetAttributeAsBoolean("tradingAllowed",
                            trainTradingAllowed);
                oneTrainPerTurn = trainsTag.GetAttributeAsBoolean("onePerTurn", oneTrainPerTurn);
                oneTrainPerTypePerTurn = trainsTag.GetAttributeAsBoolean("onePerTypePerTurn", oneTrainPerTypePerTurn);
            }

            Tag loansTag = tag.GetChild("Loans");
            if (loansTag != null)
            {
                loanTakingAllowed = loansTag.GetAttributeAsBoolean("allowed", loanTakingAllowed);
            }

            Tag parameterTag = tag.GetChild("Parameters");
            if (parameterTag != null)
            {
                if (parameters == null) parameters = new Dictionary<string, string>();
                Dictionary<string, string> attributes = parameterTag.GetAttributes();
                foreach (string key in attributes.Keys)
                {
                    parameters[key] = attributes[key];
                }
            }

            Tag setTag = tag.GetChild("Action");
            if (setTag != null)
            {
                if (actions == null) actions = new Dictionary<string, string>();
                string key = setTag.GetAttributeAsString("name");
                if (string.IsNullOrEmpty(key))
                {
                    throw new ConfigurationException("Phase " + name + ": <Set> without action name");
                }
                string value = setTag.GetAttributeAsString("value", null);
                actions[key] = value;
            }

            // Extra info text(usually related to extra-share special properties)
            Tag infoTag = tag.GetChild("Info");
            if (infoTag != null)
            {
                string infoKey = infoTag.GetAttributeAsString("key");
                string[] infoParms = infoTag.GetAttributeAsString("parm", "").Split(',');
                extraInfo += "<br>" + LocalText.GetText(infoKey, (object[])infoParms);
            }

        }

        public void FinishConfiguration(RailsRoot root)
        {

            TrainManager trainManager = GetRoot.TrainManager;
            TrainCertificateType type;

            if (rustedTrainNames != null)
            {
                List<TrainCertificateType> newRustedTrains = new List<TrainCertificateType>();
                foreach (string typeName in rustedTrainNames.Split(','))
                {
                    type = trainManager.GetCertTypeByName(typeName);
                    if (type == null)
                    {
                        throw new ConfigurationException(" Unknown rusted train type '" + typeName + "' for phase '" + Id + "'");
                    }
                    newRustedTrains.Add(type);
                    type.IsPermanent = false;
                }
                rustedTrains = newRustedTrains;
            }

            if (releasedTrainNames != null)
            {
                List<TrainCertificateType> newReleasedTrains = new List<TrainCertificateType>();
                foreach (string typeName in releasedTrainNames.Split(','))
                {
                    type = trainManager.GetCertTypeByName(typeName);
                    if (type == null)
                    {
                        throw new ConfigurationException(" Unknown released train type '" + typeName + "' for phase '" + Id + "'");
                    }
                    newReleasedTrains.Add(type);
                }
                releasedTrains = newReleasedTrains;
            }

            // Push any extra tile lay turns to the appropriate company type.
            if (tileLaysPerColourTurns != null)
            {
                CompanyManager companyManager = GetRoot.CompanyManager;
                companyManager.AddExtraTileLayTurnsInfo(tileLaysPerColourTurns);
            }
            tileLaysPerColourTurns = null;  // We no longer need it.
        }

        /** Called when a phase gets activated */
        public void Activate()
        {
            ReportBuffer.Add(this, LocalText.GetText("StartOfPhase", Id));

            // Report any extra info
            if (!string.IsNullOrEmpty(extraInfo))
            {
                Regex r = new Regex("^<[Bb][Rr]>");
                Regex r2 = new Regex("<[Bb][Rr]>");
                string s = r.Replace(extraInfo, "", 1);
                s = r2.Replace(s, "\n");
                ReportBuffer.Add(this, s);//extraInfo.replaceFirst("^<[Bb][Rr]>", "").replaceAll("<[Bb][Rr]>", "\n"));
            }

            if (closedObjects != null && (closedObjects.Count > 0))
            {
                foreach (ICloseable o in closedObjects)
                {
                    log.Debug("Closing object " + o.ToString());
                    o.Close();
                }
            }

            TrainManager trainManager = GetRoot.TrainManager;

            if (rustedTrains != null && (rustedTrains.Count > 0))
            {
                foreach (TrainCertificateType type in rustedTrains)
                {
                    trainManager.RustTrainType(type, lastTrainBuyer.Value);
                }
            }

            if (releasedTrains != null && (releasedTrains.Count > 0))
            {
                foreach (TrainCertificateType type in releasedTrains)
                {
                    trainManager.MakeTrainAvailable(type);
                }
            }

            if (actions != null && (actions.Count > 0))
            {
                foreach (string actionName in actions.Keys)
                {
                    GetRoot.GameManager.ProcessPhaseAction(actionName, actions[actionName]);
                }
            }

            if (DoPrivatesClose)
            {
                GetRoot.CompanyManager.CloseAllPrivates();
            }
        }

        public void SetLastTrainBuyer(IOwner lastTrainBuyer)
        {
            this.lastTrainBuyer.Set(lastTrainBuyer);
        }

        public string GetInfo()
        {
            return extraInfo;
        }

        [Obsolete]
        // FIXME: Replace this with TileColor object
        public bool IsTileColorAllowed(string tileColour)
        {
            return ((List<string>)tileColors).Contains(tileColour);
        }

        [Obsolete]
        // FIXME: Replace this with TileColor objects
        public List<string> GetTileColors()
        {
            return (List<string>)tileColors;
        }

        public string GetTileColorsString()
        {
            StringBuilder b = new StringBuilder();
            foreach (string color in tileColors)
            {
                if (b.Length > 0) b.Append(",");
                b.Append(color);
            }
            return b.ToString();
        }

        public int GetTileLaysPerColour(string companyTypeName, string colorName)
        {

            if (tileLaysPerColor == null) return 1;

            string key = companyTypeName + "~" + colorName;
            if (tileLaysPerColor.ContainsKey(key))
            {
                return tileLaysPerColor[key];
            }
            else
            {
                return 1;
            }
        }

        public int TrainLimitStep
        {
            get
            {
                return trainLimitStep;
            }
        }

        public int TrainLimitIndex
        {
            get
            {
                return trainLimitStep - 1;
            }
        }

        public int Index
        {
            get
            {
                return index;
            }
        }

        public string RealName
        {
            get
            {
                return (name != null) ? name : Id;
            }
        }

        /**
         * @return Returns the privatesClose.
         */
        public bool DoPrivatesClose
        {
            get
            {
                return privatesClose;
            }
        }

        /**
         * @return Returns the privateSellingAllowed.
         */
        public bool IsPrivateSellingAllowed
        {
            get
            {
                return privateSellingAllowed;
            }
        }
        // sfy 1889
        public int PrivatesRevenueStep
        {
            get
            {
                return privatesRevenueStep;
            }
        }
        public bool IsTrainTradingAllowed
        {
            get
            {
                return trainTradingAllowed;
            }
        }

        public bool CanBuyMoreTrainsPerTurn
        {
            get
            {
                return !oneTrainPerTurn;
            }
        }

        public bool CanBuyMoreTrainsPerTypePerTurn
        {
            get
            {
                return !oneTrainPerTypePerTurn;
            }
        }

        public bool IsLoanTakingAllowed
        {
            get
            {
                return loanTakingAllowed;
            }
        }

        public int NumberOfOperatingRounds
        {
            get
            {
                return numberOfOperatingRounds;
            }
        }

        public IReadOnlyList<TrainCertificateType> RustedTrains
        {
            get
            {
                return rustedTrains;
            }
        }

        public IReadOnlyList<TrainCertificateType> ReleasedTrains
        {
            get
            {
                return releasedTrains;
            }
        }

        /**
         * @return Returns the offBoardRevenueStep.
         */
        public int OffBoardRevenueStep
        {
            get
            {
                return offBoardRevenueStep;
            }
        }

        public void AddObjectToClose(ICloseable o)
        {
            if (closedObjects == null)
            {
                closedObjects = new List<ICloseable>(4);
            }
            if (!closedObjects.Contains(o)) closedObjects.Add(o);
        }

        public string GetParameterAsString(string key)
        {
            if (parameters != null)
            {
                return parameters[key];
            }
            else
            {
                return null;
            }
        }

        public int GetParameterAsInteger(string key)
        {
            string stringValue = GetParameterAsString(key);
            if (stringValue == null)
            {
                return 0;
            }
            try
            {
                return int.Parse(stringValue);
            }
            catch (Exception)
            {
                log.Error("Error while parsing parameter " + key + " in phase " + Id);
                return 0;
            }

        }

        public List<ICloseable> ClosedObjects
        {
            get
            {
                return closedObjects;
            }
        }

        override public string ToText()
        {
            return RealName;
        }

        public static Phase GetCurrent(IRailsItem item)
        {
            return item.GetRoot.PhaseManager.GetCurrentPhase();
        }

    }
}
