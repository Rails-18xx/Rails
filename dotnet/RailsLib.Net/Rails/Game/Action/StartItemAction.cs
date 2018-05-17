using GameLib.Net.Game;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class StartItemAction : PossibleAction
    {
        /* Server-provided fields */
        /*transient*/
        [JsonIgnore]
        protected StartItem startItem;
        protected string startItemName;
        protected int itemIndex;

        new public const long serialVersionUID = 1L;

        /**
         * 
         */

        public StartItemAction() : base(null) { }

        public StartItemAction(StartItem startItem) : base(null)
        {
            this.startItem = startItem;
            this.startItemName = startItem.Id;
            this.itemIndex = startItem.Index;
        }

        /**
         * @return Returns the startItem.
         */
        public StartItem StartItem
        {
            get
            {
                return startItem;
            }
        }

        public int ItemIndex
        {
            get
            {
                return itemIndex;
            }
        }

        public int GetStatus()
        {
            return startItem.GetStatus();
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            StartItemAction action = (StartItemAction)pa;
            return startItem.Equals(action.startItem)
                    && (itemIndex == action.itemIndex);
            // no asAction attributes to be checked
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                    .AddToString("startItem", startItem)
                    .AddToString("itemIndex", itemIndex)
                    .ToString()
            ;
        }

        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            startItem = root.CompanyManager.GetStartItemById(startItemName);
        }
    }
}
