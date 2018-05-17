using GameLib.Net.Game;
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
    public class DiscardTrain : PossibleORAction
    {
        // Server settings
        /*transient*/
        [JsonIgnore]
        private List<Train> ownedTrains = null;
        private string[] ownedTrainsUniqueIds;

        /** True if discarding trains is mandatory */
        bool forced = false;

        // Client settings
        /*transient*/
        [JsonIgnore]
        private Train discardedTrain = null;
        private string discardedTrainUniqueId;

        new public const long serialVersionUID = 1L;

        public DiscardTrain(PublicCompany company, List<Train> trains) : base()
        {
            this.ownedTrains = trains;
            this.ownedTrainsUniqueIds = new string[trains.Count];
            int i = 0;
            foreach (Train train in trains)
            {
                ownedTrainsUniqueIds[i++] = train.Id;
            }
            this.company = company;
            this.companyName = company.Id;
        }

        public DiscardTrain(PublicCompany company, List<Train> trainsToDiscardFrom, bool forced) :
            this(company, trainsToDiscardFrom)
        {
            this.forced = forced;
        }

        public List<Train> OwnedTrains
        {
            get
            {
                return ownedTrains;
            }
        }

        private List<TrainType> GetOwnedTrainTypes()
        {
            List<TrainType> types = new List<TrainType>();
            foreach (Train train in ownedTrains)
            {
                types.Add(train.GetTrainType());
            }
            return types;
        }

        public Train DiscardedTrain
        {
            get
            {
                return discardedTrain;
            }
            set
            {
                discardedTrain = value;
                discardedTrainUniqueId = value.Id;
            }
        }

        public bool IsForced
        {
            get
            {
                return forced;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            DiscardTrain action = (DiscardTrain)pa;
            // TODO: only the types have to be identical, due Rails 1.x backward compatibility
            // #FIXME does this list compare need to be using Except instead?
            bool options = GetOwnedTrainTypes().SequenceEqual(action.GetOwnedTrainTypes())
                    && (forced == action.forced);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            // TODO: only the types have to be identical, due Rails 1.x backward compatibility
            return options && discardedTrain.GetTrainType().Equals(action.discardedTrain.GetTrainType());
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("ownedTrains", ownedTrains)
                        .AddToString("forced", forced)
                        .AddToStringOnlyActed("discardedTrain", discardedTrain)
                        .ToString();
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            TrainManager trainManager = RailsRoot.Instance.TrainManager;

            if (discardedTrainUniqueId != null)
            {
                discardedTrain = trainManager.GetTrainByUniqueId(discardedTrainUniqueId);
            }

            if (ownedTrainsUniqueIds != null && ownedTrainsUniqueIds.Length > 0)
            {
                ownedTrains = new List<Train>();
                for (int i = 0; i < ownedTrainsUniqueIds.Length; i++)
                {
                    ownedTrains.Add(trainManager.GetTrainByUniqueId(ownedTrainsUniqueIds[i]));
                }
            }
        }
    }
}
