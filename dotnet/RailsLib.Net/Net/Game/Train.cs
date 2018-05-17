using GameLib.Net.Common;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    public class Train : RailsOwnableItem<Train>, ICreatable
    {
        protected TrainCertificateType certificateType;

        protected GenericState<TrainType> type;

        /** Some specific trains cannot be traded between companies */
        protected bool tradeable = true;

        protected BooleanState obsolete;

        // sorting id to correctly sort them inside a portfolio
        // this is a workaround to have 2.0 compatible with 1.x save files
        // it should be removed in the mid-term by selecting trains from a portfolio based only on type, not on id
        protected int sortingId;

        /**
         * Used by Configure (via reflection) only
         */
        public Train(IRailsItem parent, string id) : base(parent, id)
        {
            type = GenericState<TrainType>.Create(this, "type");
            obsolete = BooleanState.Create(this, "obsolete");
        }

        public static Train Create(IRailsItem parent, int uniqueId, TrainCertificateType certType, TrainType type)
        {
            string id = certType.Id + "_" + uniqueId;
            Train train = certType.CreateTrain(parent, id, uniqueId);
            train.CertType = certType;
            train.SetTrainType(type);
            return train;
        }

        //@Override
        //public RailsItem getParent()
        //{
        //    return (RailsItem)super.getParent();
        //}

        //@Override
        //public RailsRoot getRoot()
        //{
        //    return (RailsRoot)super.getRoot();
        //}

        public void SetSortingId(int sortingId)
        {
            this.sortingId = sortingId;
        }

        public void SetTrainType(TrainType type)
        {
            this.type.Set(type);
        }

        /**
         * @return Returns the type.
         */
        public TrainCertificateType CertType
        {
            get
            {
                return certificateType;
            }
            set
            {
                certificateType = value;
            }
        }

        public TrainType GetTrainType()
        {
            return IsAssigned ? type.Value : null;
        }

        /**import rails.game.state.AbstractItem;

         * @return Returns the cityScoreFactor.
         */
        public int CityScoreFactor
        {
            get
            {
                return GetTrainType().CityScoreFactor;
            }
        }

        /**
         * @return Returns the cost.
         */
        public int Cost
        {
            get
            {
                return GetTrainType().Cost;
            }
        }

        /**
         * @return Returns the majorStops.
         */
        public int MajorStops
        {
            get
            {
                return GetTrainType().MajorStops;
            }
        }

        /**
         * @return Returns the minorStops.
         */
        public int MinorStops
        {
            get
            {
                return GetTrainType().MinorStops;
            }
        }

        /**
         * @return Returns the townCountIndicator.
         */
        public int TownCountIndicator
        {
            get
            {
                return GetTrainType().TownCountIndicator;
            }
        }

        /**
         * @return Returns the townScoreFactor.
         */
        public int TownScoreFactor
        {
            get
            {
                return GetTrainType().TownScoreFactor;
            }
        }

        /**
         * @return true => hex train (examples 1826, 1844), false => standard 1830 type train
         */
        public bool IsHTrain
        {
            get
            {
                // TODO Auto-generated method stub
                return false;
            }
        }

        /**
         * @return true => train is express train; false =>
         */
        public bool IsETrain
        {
            get
            {
                return false;
            }
        }

        public bool IsAssigned
        {
            get
            {
                return type.Value != null;
            }
        }

        public bool IsPermanent
        {
            get
            {
                return certificateType.IsPermanent;
            }
        }

        public bool IsObsolete()
        {
            return obsolete.Value;
        }

        public void SetRusted()
        {
            // if not on scrapheap already
            if (this.Owner != Bank.GetScrapHeap(this))
            {
                this.MoveTo(Bank.GetScrapHeap(this));
            }
        }

        public void SetObsolete()
        {
            obsolete.Set(true);
        }

        public bool CanBeExchanged
        {
            get
            {
                return certificateType.NextCanBeExchanged;
            }
        }

        public void Discard()
        {
            BankPortfolio discardTo;
            if (IsObsolete())
            {
                discardTo = Bank.GetScrapHeap(this);
            }
            else
            {
                discardTo = GetRoot.TrainManager.DiscardTo;
            }
            string discardText = LocalText.GetText("CompanyDiscardsTrain", Owner.Id, this.ToText(), discardTo.Id);
            ReportBuffer.Add(this, discardText);
            this.MoveTo(discardTo);
        }

        override public string ToText()
        {
            return IsAssigned ? type.Value.Name : certificateType.ToText();
        }

        public bool IsTradeable
        {
            get
            {
                return tradeable;
            }
            set
            {
                tradeable = value;
            }
        }

        override public int CompareTo(IOwnable other)
        {
            if (other is Train oTrain)
            {
                int result = CertType.CompareTo(oTrain.CertType);
                if (result != 0) return result;

                return sortingId.CompareTo(oTrain.sortingId);
                //Train oTrain = (Train)other;
                //return ComparisonChain.start()
                //        .compare(this.getCertType(), oTrain.getCertType())
                //        .compare(this.sortingId, oTrain.sortingId)
                //        .result();
            }
            // #TODO_is_this_right?
            // should this be return base.CompareTo(other)?
            return 0;
        }
    }
}
