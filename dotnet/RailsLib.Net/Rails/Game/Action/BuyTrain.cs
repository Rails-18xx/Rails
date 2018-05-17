using GameLib.Net.Game;
using GameLib.Net.Game.Special;
using GameLib.Net.Game.State;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class BuyTrain : PossibleORAction
    {
        // Initial settings
        /*transient*/
        [JsonIgnore]
        private Train train;
        private string trainUniqueId;
        /*transient*/
        [JsonIgnore]
        private IOwner from;
        private string fromName;
        private int fixedCost = 0;
        private bool forcedBuyIfNoRoute = false; // TODO Can be disabled once route checking exists
        /*transient*/
        [JsonIgnore]
        private List<Train> trainsForExchange = null;
        private string[] trainsForExchangeUniqueIds;

        /** Obsolete, but left in for backwards compatibility of saved files */
#pragma warning disable 414
        private bool forcedExchange = false;
#pragma warning restore 414

        private bool presidentMustAddCash = false; // If buying from the bank
        private bool presidentMayAddCash = false;  // If buying from a company
        private int presidentCashToAdd = 0;

        /*transient*/
        [JsonIgnore]
        private SpecialTrainBuy specialProperty = null;
        private int specialPropertyId = 0;

        private string extraMessage = null;

        // Added jun2011 by EV to cover dual trains.
        // NOTE: Train objects from now on represent train *certificates* 
        /*transient*/
        [JsonIgnore]
        private TrainType type;
        private string typeName;

        // User settings
        private int pricePaid = 0;
        private int addedCash = 0;
        /*transient*/
        [JsonIgnore]
        private Train exchangedTrain = null;
        private string exchangedTrainUniqueId;

        new public const long serialVersionUID = 2L;

        public BuyTrain(Train train, IOwner from, int fixedCost) : this(train, train.GetTrainType(), from, fixedCost)
        {
        }

        public BuyTrain(Train train, TrainType type, IOwner from, int fixedCost)
        {
            this.train = train;
            this.trainUniqueId = train.Id;
            this.from = from;
            this.fromName = from.Id;
            this.fixedCost = fixedCost;
            this.type = type;
            this.typeName = type.Name;
        }

        public BuyTrain SetTrainsForExchange(List<Train> trains)
        {
            trainsForExchange = trains;
            if (trains != null)
            {
                trainsForExchangeUniqueIds = new string[trains.Count];
                int i = 0;
                foreach (Train train in trains)
                {
                    trainsForExchangeUniqueIds[i++] = train.Id;
                }
            }
            return this;
        }

        public BuyTrain SetPresidentMustAddCash(int amount)
        {
            presidentMustAddCash = true;
            presidentCashToAdd = amount;
            return this;
        }

        public BuyTrain SetPresidentMayAddCash(int amount)
        {
            presidentMayAddCash = true;
            presidentCashToAdd = amount;
            return this;
        }

        public string ExtraMessage
        {
            get
            {
                return extraMessage;
            }
            set
            {
                extraMessage = value;
            }
        }

        /**
         * @return Returns the specialProperty.
         */
        public SpecialTrainBuy SpecialProperty
        {
            get
            {
                return specialProperty;
            }
            set
            {
                specialProperty = value;
                specialPropertyId = specialProperty.UniqueId;
            }
        }

        public bool HasSpecialProperty
        {
            get
            {
                return specialProperty != null;
            }
        }

        /**
         * To be used for all usage of train, also within this class.
         * After reloading the 2nd copy etc. of a train with unlimited quantity,
         * the train attribute will be null (because readObject() is called and the
         * train is initiated before the actions have been executed - the second
         * train is in this case only created after buying the first one).
         * @return
         */
        public Train Train
        {
            get
            {
                if (train == null)
                {
                    train = RailsRoot.Instance.TrainManager.GetTrainByUniqueId(trainUniqueId);
                }
                return train;
            }
        }

        public TrainType TrainType
        {
            get
            {
                return type;
            }
        }

        public IOwner FromOwner
        {
            get
            {
                return from;
            }
        }

        public int FixedCost
        {
            get
            {
                return fixedCost;
            }
            set
            {
                fixedCost = value;
            }
        }

        public bool IsForExchange
        {
            get
            {
                return trainsForExchange != null && (trainsForExchange.Count > 0);
            }
        }

        public List<Train> TrainsForExchange
        {
            get
            {
                return trainsForExchange;
            }
        }

        public bool MustPresidentAddCash
        {
            get
            {
                return presidentMustAddCash;
            }
        }

        public bool MayPresidentAddCash
        {
            get
            {
                return presidentMayAddCash;
            }
        }

        public int PresidentCashToAdd
        {
            get
            {
                return presidentCashToAdd;
            }
        }

        public bool IsForcedBuyIfNoRoute
        {
            get
            {
                return forcedBuyIfNoRoute;
            }
            set
            {
                forcedBuyIfNoRoute = value;
            }
        }

        public IOwner Owner
        {
            get
            {
                return Train.Owner;
            }
        }

        public int AddedCash
        {
            get
            {
                return addedCash;
            }
            set
            {
                addedCash = value;
            }
        }

        public int PricePaid
        {
            get
            {
                return pricePaid;
            }
            set
            {
                pricePaid = value;
            }
        }

        public Train ExchangedTrain
        {
            get
            {
                return exchangedTrain;
            }
            set
            {
                exchangedTrain = value;
                if (exchangedTrain != null)
                    exchangedTrainUniqueId = exchangedTrain.Id;
            }
        }

        // TODO: Check for and add the missing attributes
        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            BuyTrain action = (BuyTrain)pa;
            bool options = Train.GetTrainType().Equals(action.Train.GetTrainType())
                    // only types have to be equal, and the getTrain() avoids train == null
                    && from.Equals(action.from)
                    && (action.fixedCost == 0 || fixedCost == action.pricePaid)
                    && trainsForExchange.SequenceEqual(action.trainsForExchange);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options
                    && train.Equals(action.train)
                    && (pricePaid == action.pricePaid)
                    && (addedCash == action.addedCash)
                    && (exchangedTrainUniqueId == action.exchangedTrainUniqueId);
        }

        // TODO: Check for and add the missing attributes
        override public string ToString()
        {

            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("train", train)
                        .AddToString("from", from)
                        .AddToString("fixedCost", fixedCost)
                        .AddToString("trainsForExchange", trainsForExchange)
                        .AddToStringOnlyActed("pricePaid", pricePaid)
                        .AddToStringOnlyActed("addedCash", addedCash)
                        .AddToStringOnlyActed("exchangedTrainUniqueId", exchangedTrainUniqueId)
                    .ToString();
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            RailsRoot root = RailsRoot.Instance;
            TrainManager trainManager = root.TrainManager;
            CompanyManager companyManager = root.CompanyManager;

            fromName = companyManager.CheckAlias(fromName);

            train = trainManager.GetTrainByUniqueId(trainUniqueId);
            // Note: the 2nd etc. copy of an unlimited quantity train will become null this way.
            // Set getTrain() for how this is fixed.
            if (typeName == null)
            {
                if (train == null)
                {
                    // Kludge to cover not yet cloned unlimited trains
                    typeName = trainUniqueId.Split('_')[0];
                    type = trainManager.GetTypeByName(typeName);
                }
                else
                {
                    type = train.GetTrainType();
                    typeName = type.Name;
                }
            }
            else
            {
                type = trainManager.GetTypeByName(typeName);
            }

            // TODO: This has to be replaced by a new mechanism for owners at some time
            from = GameManager.GetPortfolioByName(fromName).Parent;
            if (trainsForExchangeUniqueIds != null
                && trainsForExchangeUniqueIds.Length > 0)
            {
                trainsForExchange = new List<Train>();
                for (int i = 0; i < trainsForExchangeUniqueIds.Length; i++)
                {
                    trainsForExchange.Add(trainManager.GetTrainByUniqueId(trainsForExchangeUniqueIds[i]));
                }
            }

            if (specialPropertyId > 0)
            {
                specialProperty =
                        (SpecialTrainBuy)Net.Game.Special.SpecialProperty.GetByUniqueId(GetRoot, specialPropertyId);
            }

            if (!string.IsNullOrEmpty(exchangedTrainUniqueId))
            {
                exchangedTrain = trainManager.GetTrainByUniqueId(exchangedTrainUniqueId);
            }
        }
    }
}
