using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    public class TrainManager : RailsManager, IConfigurable
    {
        // Static attributes
        protected List<TrainType> lTrainTypes = new List<TrainType>();
        protected Dictionary<string, TrainType> mTrainTypes = new Dictionary<string, TrainType>();
        protected List<TrainCertificateType> trainCertTypes = new List<TrainCertificateType>();
        protected Dictionary<string, TrainCertificateType> trainCertTypeMap = new Dictionary<string, TrainCertificateType>();
        protected Dictionary<string, Train> trainMap = new Dictionary<string, Train>();
        protected Dictionary<TrainCertificateType, List<Train>> trainsPerCertType = new Dictionary<TrainCertificateType, List<Train>>();

        private bool removeTrain = false;

        protected string discardToString = "pool";
        protected BankPortfolio discardTo;

        // defines obsolescence
        public enum ObsoleteTrainForType { ALL, EXCEPT_TRIGGERING }
        protected ObsoleteTrainForType obsoleteTrainFor = ObsoleteTrainForType.EXCEPT_TRIGGERING; // default is ALL

        // Dynamic attributes
        protected IntegerState newTypeIndex;
        protected DictionaryState<string, int> lastIndexPerType;
        protected BooleanState phaseHasChanged;
        protected BooleanState trainAvailabilityChanged;

        /** Required for the sell-train-to-foreigners feature of some games */
        protected BooleanState anyTrainBought;

        // Triggered phase changes
        protected Dictionary<TrainCertificateType, Dictionary<int, Phase>> newPhases = new Dictionary<TrainCertificateType, Dictionary<int, Phase>>();

        // For initialization only
        bool trainPriceAtFaceValueIfDifferentPresidents = false;

        protected static Logger<TrainManager> log = new Logger<TrainManager>();

        /**
         * Used by Configure (via reflection) only
         */
        public TrainManager(RailsRoot parent, string id) : base(parent, id)
        {
            newTypeIndex = IntegerState.Create(this, "newTypeIndex", 0);
            lastIndexPerType = DictionaryState<string, int>.Create(this, "lastIndexPerType");
            phaseHasChanged = BooleanState.Create(this, "phaseHasChanged");
            trainAvailabilityChanged = BooleanState.Create(this, "trainAvailablityChanged");
            anyTrainBought = BooleanState.Create(this, "anyTrainBought");
        }

        /**
         * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
         */
        public void ConfigureFromXML(Tag tag)
        {
            TrainType newType;

            Tag defaultsTag = tag.GetChild("Defaults");
            // We will use this tag later, to preconfigure TrainCertType and TrainType.

            List<Tag> typeTags;

            // Choice train types (new style)
            List<Tag> trainTypeTags = tag.GetChildren("TrainType");

            if (trainTypeTags != null)
            {
                int trainTypeIndex = 0;
                foreach (Tag trainTypeTag in trainTypeTags)
                {
                    // FIXME: Creation of Type to be rewritten
                    string trainTypeId = trainTypeTag.GetAttributeAsString("name");
                    TrainCertificateType certType = TrainCertificateType.Create(this, trainTypeId, trainTypeIndex++);
                    if (defaultsTag != null) certType.ConfigureFromXML(defaultsTag);
                    certType.ConfigureFromXML(trainTypeTag);
                    trainCertTypes.Add(certType);
                    trainCertTypeMap[certType.Id] = certType;

                    // The potential train types
                    typeTags = trainTypeTag.GetChildren("Train");
                    if (typeTags == null)
                    {
                        // That's OK, all properties are in TrainType, to let's reuse that tag
                        typeTags = new List<Tag>() { trainTypeTag }; // Arrays.asList(trainTypeTag);
                    }
                    foreach (Tag typeTag in typeTags)
                    {
                        newType = new TrainType();
                        if (defaultsTag != null) newType.ConfigureFromXML(defaultsTag);
                        newType.ConfigureFromXML(trainTypeTag);
                        newType.ConfigureFromXML(typeTag);
                        lTrainTypes.Add(newType);
                        mTrainTypes[newType.Name] = newType;
                        certType.AddPotentialTrainType(newType);
                    }
                }
            }


            // Special train buying rules
            Tag rulesTag = tag.GetChild("TrainBuyingRules");
            if (rulesTag != null)
            {
                // A 1851 special
                trainPriceAtFaceValueIfDifferentPresidents = rulesTag.GetChild("FaceValueIfDifferentPresidents") != null;
            }

            // Train obsolescence
            string obsoleteAttribute = tag.GetAttributeAsString("ObsoleteTrainFor");
            if (!string.IsNullOrEmpty(obsoleteAttribute))
            {
                try
                {
                    obsoleteTrainFor = (ObsoleteTrainForType)Enum.Parse(typeof(ObsoleteTrainForType), obsoleteAttribute);
                }
                catch (Exception e)
                {
                    throw new ConfigurationException(e);
                }
            }

            // Trains discard
            Tag discardTag = tag.GetChild("DiscardTrain");
            if (discardTag != null)
            {
                discardToString = discardTag.GetAttributeAsString("to");
            }

            // Are trains sold to foreigners?
            Tag removeTrainTag = tag.GetChild("RemoveTrainBeforeSR");
            if (removeTrainTag != null)
            {
                // Trains "bought by foreigners" (1844, 1824)
                removeTrain = true; // completed in finishConfiguration()
            }

        }

        public void FinishConfiguration(RailsRoot root)
        {
            Dictionary<int, string> newPhaseNames;
            Phase phase;
            string phaseName;
            PhaseManager phaseManager = root.PhaseManager;

            foreach (TrainCertificateType certType in trainCertTypes)
            {
                certType.FinishConfiguration(root);

                List<TrainType> types = certType.GetPotentialTrainTypes();
                foreach (TrainType type in types)
                {
                    type.FinishConfiguration(root, certType);
                }

                // Now create the trains of this type
                Train train;
                // Multi-train certificates cannot yet be assigned a type
                TrainType initialType = types.Count == 1 ? types[0] : null;

                /* If the amount is infinite, only one train is created.
                 * Each time this train is bought, another one is created.
                 */
                for (int i = 0; i < (certType.HasInfiniteQuantity ? 1 : certType.Quantity); i++)
                {
                    train = Train.Create(this, GetNewUniqueId(certType.Id), certType, initialType);
                    AddTrain(train);
                    Bank.GetUnavailable(this).PortfolioModel.AddTrain(train);
                }

                // Register any phase changes
                newPhaseNames = certType.GetNewPhaseNames();
                if (newPhaseNames != null && newPhaseNames.Count > 0)
                {
                    foreach (int index in newPhaseNames.Keys)
                    {
                        phaseName = newPhaseNames[index];
                        phase = (Phase)phaseManager.GetPhaseByName(phaseName);
                        if (phase == null)
                        {
                            throw new ConfigurationException("New phase '" + phaseName + "' does not exist");
                        }
                        if (!newPhases.ContainsKey(certType)) newPhases[certType] = new Dictionary<int, Phase>();
                        newPhases[certType][index] = phase;
                    }
                }
            }

            // By default, set the first train type to "available".
            newTypeIndex.Set(0);
            MakeTrainAvailable(trainCertTypes[newTypeIndex.Value]);

            // Discard Trains To where?
            if (discardToString.Equals("pool", StringComparison.OrdinalIgnoreCase))
            {
                discardTo = root.Bank.Pool;
            }
            else if (discardToString.Equals("scrapheap", StringComparison.OrdinalIgnoreCase))
            {
                discardTo = root.Bank.ScrapHeap;
            }
            else
            {
                throw new ConfigurationException("Discard to only allow to pool or scrapheap");
            }

            // Trains "bought by foreigners" (1844, 1824)
            if (removeTrain)
            {
                root.GameManager.SetGameParameter(GameDef.Parm.REMOVE_TRAIN_BEFORE_SR, true);
            }

            // Train trading between different players at face value only (1851)
            root.GameManager.SetGameParameter(GameDef.Parm.FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS,
                    trainPriceAtFaceValueIfDifferentPresidents);
        }

        /** Create train without throwing exceptions.
         * To be used <b>after</b> completing initialization,
         * i.e. in cloning infinitely available trains.
         */

        public Train CloneTrain(TrainCertificateType certType)
        {
            Train train = null;
            List<TrainType> types = certType.GetPotentialTrainTypes();
            TrainType initialType = types.Count == 1 ? types[0] : null;
            try
            {
                train = Train.Create(this, GetNewUniqueId(certType.Id), certType, initialType);
            }
            catch (ConfigurationException e)
            {
                log.Warn("Unexpected exception " + e.Message);
            }
            AddTrain(train);
            return train;
        }

        public void AddTrain(Train train)
        {
            trainMap[train.Id] = train;

            TrainCertificateType type = train.CertType;
            if (!trainsPerCertType.ContainsKey(type))
            {
                trainsPerCertType[type] = new List<Train>();
            }
            trainsPerCertType[type].Add(train);
        }

        public Train GetTrainByUniqueId(string id)
        {
            return trainMap[id];
        }

        public int GetNewUniqueId(string typeName)
        {
            int newUniqueId = lastIndexPerType.ContainsKey(typeName) ? lastIndexPerType.Get(typeName) + 1 : 0;
            lastIndexPerType.Put(typeName, newUniqueId);
            return newUniqueId;
        }

        /**
         * This method handles any consequences of new train buying (from the IPO),
         * such as rusting and phase changes. It must be called <b>after</b> the
         * train has been transferred.
         *
         */
        public void CheckTrainAvailability(Train train, IOwner from)
        {
            phaseHasChanged.Set(false);
            if (from != Bank.GetIpo(this)) return;

            TrainCertificateType boughtType, nextType;
            boughtType = train.CertType;
            if (boughtType == (trainCertTypes[newTypeIndex.Value])
                && Bank.GetIpo(this).PortfolioModel.GetTrainOfType(boughtType) == null)
            {
                // Last train bought, make a new type available.
                newTypeIndex.Add(1);
                if (newTypeIndex.Value < lTrainTypes.Count)
                {
                    nextType = (trainCertTypes[newTypeIndex.Value]);
                    if (nextType != null)
                    {
                        if (!nextType.IsAvailable())
                        {
                            MakeTrainAvailable(nextType);
                            trainAvailabilityChanged.Set(true);
                            ReportBuffer.Add(this, "All " + boughtType.ToText()
                                             + "-trains are sold out, "
                                             + nextType.ToText() + "-trains now available");
                        }
                    }
                }
            }

            int trainIndex = boughtType.GetNumberBoughtFromIPO();
            if (trainIndex == 1)
            {
                // First train of a new type bought
                ReportBuffer.Add(this, LocalText.GetText("FirstTrainBought",
                        boughtType.ToText()));
            }

            // New style phase changes, can be triggered by any bought train.
            if (newPhases.ContainsKey(boughtType)
                    && (newPhases[boughtType].ContainsKey(trainIndex)))
            {
                Phase newPhase = newPhases[boughtType][trainIndex];
                GetRoot.PhaseManager.SetPhase(newPhase, train.Owner);
                phaseHasChanged.Set(true);
            }
        }

        public void MakeTrainAvailable(TrainCertificateType type)
        {

            type.SetAvailable();

            BankPortfolio to =
                (type.InitialPortfolio.Equals("Pool", StringComparison.OrdinalIgnoreCase) ? Bank.GetPool(this)
                        : Bank.GetIpo(this));

            foreach (Train train in trainsPerCertType[type])
            {
                to.PortfolioModel.AddTrain(train);
            }
        }

        // checks train obsolete condition
        private bool IsTrainObsolete(Train train, IOwner lastBuyingCompany)
        {
            // check fist if train can obsolete at all
            if (!train.CertType.IsObsoleting) return false;
            // and if it is in the pool (always rust)
            if (train.Owner == Bank.GetPool(this)) return false;

            // then check if obsolete type
            if (obsoleteTrainFor == ObsoleteTrainForType.ALL)
            {
                return true;
            }
            else
            { // otherwise it is AllExceptTriggering
                IOwner owner = train.Owner;
                return (owner is PublicCompany && owner != lastBuyingCompany);
            }
        }

        public void RustTrainType(TrainCertificateType type, IOwner lastBuyingCompany)
        {
            type.SetRusted();
            foreach (Train train in trainsPerCertType[type])
            {
                IOwner owner = train.Owner;
                // check condition for train rusting
                if (IsTrainObsolete(train, lastBuyingCompany))
                {
                    log.Debug("Train " + train.Id + " (owned by "
                            + owner.Id + ") obsoleted");
                    train.SetObsolete();
                    // TODO: is this still required?
                    // train.getHolder().update();
                }
                else
                {
                    log.Debug("Train " + train.Id + " (owned by "
                            + owner.Id + ") rusted");
                    train.SetRusted();
                }
            }
            // report about event
            if (type.IsObsoleting)
            {
                ReportBuffer.Add(this, LocalText.GetText("TrainsObsolete." + obsoleteTrainFor, type.Id));
            }
            else
            {
                ReportBuffer.Add(this, LocalText.GetText("TrainsRusted", type.Id));
            }
        }

        public List<Train> GetAvailableNewTrains()
        {

            List<Train> availableTrains = new List<Train>();
            Train train;

            foreach (TrainCertificateType type in trainCertTypes)
            {
                if (type.IsAvailable())
                {
                    train = Bank.GetIpo(this).PortfolioModel.GetTrainOfType(type);
                    if (train != null)
                    {
                        availableTrains.Add(train);
                    }
                }
            }
            return availableTrains;
        }

        public string GetTrainCostOverview()
        {
            StringBuilder b = new StringBuilder();
            foreach (TrainCertificateType certType in trainCertTypes)
            {
                if (certType.Cost > 0)
                {
                    if (b.Length > 1) b.Append(" ");
                    b.Append(certType.ToText()).Append(":").Append(Bank.Format(this, certType.Cost));
                    if (certType.ExchangeCost > 0)
                    {
                        b.Append("(").Append(Bank.Format(this, certType.ExchangeCost)).Append(")");
                    }
                }
                else
                {
                    foreach (TrainType type in certType.GetPotentialTrainTypes())
                    {
                        if (b.Length > 1) b.Append(" ");
                        b.Append(type.Name).Append(":").Append(Bank.Format(this, type.Cost));
                    }
                }
            }
            return b.ToString();
        }

        public TrainType GetTypeByName(string name)
        {
            return mTrainTypes[name];
        }

        public List<TrainType> GetTrainTypes()
        {
            return lTrainTypes;
        }

        public List<TrainCertificateType> GetTrainCertTypes()
        {
            return trainCertTypes;
        }

        public TrainCertificateType GetCertTypeByName(string name)
        {
            return trainCertTypeMap[name];
        }

        public bool HasAvailabilityChanged()
        {
            return trainAvailabilityChanged.Value;
        }

        public void ResetAvailabilityChanged()
        {
            trainAvailabilityChanged.Set(false); ;
        }

        public bool HasPhaseChanged()
        {
            return phaseHasChanged.Value;
        }

        public bool IsAnyTrainBought()
        {
            return anyTrainBought.Value;
        }

        public void SetAnyTrainBought(bool newValue)
        {
            if (IsAnyTrainBought() != newValue)
            {
                anyTrainBought.Set(newValue);
            }
        }

        public BankPortfolio DiscardTo
        {
            get
            {
                return discardTo;
            }
        }

        public List<TrainType> ParseTrainTypes(string trainTypeName)
        {
            List<TrainType> trainTypes = new List<TrainType>();
            TrainType trainType;
            foreach (string trainTypeSingle in trainTypeName.Split(','))
            {
                trainType = GetTypeByName(trainTypeSingle);
                if (trainType != null)
                {
                    trainTypes.Add(trainType);
                }
                else
                {
                    continue;
                }
            }

            return trainTypes;
        }
    }
}
