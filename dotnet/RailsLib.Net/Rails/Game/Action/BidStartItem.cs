using GameLib.Net.Game;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class BidStartItem : StartItemAction
    {
        /* Server-provided fields */
        private int minimumBid;
        private int bidIncrement;
        private bool selected;
        private bool selectForAuction;

        /* Client-provided fields */
        private int actualBid = 0;

        new public const long serialVersionUID = 1L;

        public BidStartItem() { }
        /**
         * 
         */
        public BidStartItem(StartItem startItem, int minimumBid, int bidIncrement,
                bool selected, bool selectForAuction) : base(startItem)
        {
            this.minimumBid = minimumBid;
            this.bidIncrement = bidIncrement;
            this.selected = selected;
            this.selectForAuction = selectForAuction;
        }

        public BidStartItem(StartItem startItem, int minimumBid, int bidIncrement,
                bool selected) : this(startItem, minimumBid, bidIncrement, selected, false)
        {

        }

        public int MinimumBid
        {
            get
            {
                return minimumBid;
            }
        }

        public int BidIncrement
        {
            get
            {
                return bidIncrement;
            }
        }

        public int ActualBid
        {
            get
            {
                return actualBid;
            }
            set
            {
                actualBid = value;
            }
        }

        public bool IsSelected
        {
            get
            {
                return selected;
            }
        }

        public bool IsSelectForAuction
        {
            get
            {
                return selectForAuction;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            BidStartItem action = (BidStartItem)pa;
            bool options = (minimumBid == action.minimumBid)
                    && (bidIncrement == action.bidIncrement)
                    && (selected == action.selected)
                    && (selectForAuction == action.selectForAuction);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && (actualBid == action.actualBid);
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("minimumBid", minimumBid)
                        .AddToString("bidIncrement", bidIncrement)
                        .AddToString("selected", selected)
                        .AddToString("selectForAuction", selectForAuction)
                        .AddToStringOnlyActed("actualBid", actualBid)
                        .ToString();
        }
    }
}
