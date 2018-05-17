using GameLib.Net.Util;
using GameLib.Rails.Game.Action;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

/**
 * OR action for no map mode 
 * mirrors operating actions like tile and token lays, but
 * only changes the cash position of the public company

 * Rails 2.0: Updated equals and toString methods
 */

namespace GameLib.Rails.Game.Correct
{
    [JsonObject(MemberSerialization.Fields)]
    public class OperatingCost : PossibleORAction
    {
        public enum OCTypes { LAY_TILE, LAY_BASE_TOKEN };

        /** The Constant serialVersionUID. */
        new public const long serialVersionUID = 2L;

        /* Preconditions */

        /** operating cost type (as tile lay, token lay etc.) */
        private OCTypes operatingCostType;

        /** suggested costs */
        private int suggestedCost;

        /** maximum costs */
        private int maximumCost;

        /** allow free entry */
        private bool freeEntryAllowed;

        /* Postconditions */

        /** selected cash amount */
        private int operatingCost;

        /**
         * Instantiates an operating costs action
         * 
         * @param pc Public Company
         */
        public OperatingCost(OCTypes ot, int ocCosts, bool freeEntry) : base()
        {
            operatingCostType = ot;
            suggestedCost = ocCosts;
            freeEntryAllowed = freeEntry;
            maximumCost = company.Cash;
        }

        public bool IsFreeEntryAllowed
        {
            get
            {
                return freeEntryAllowed;
            }
        }

        public int Amount
        {
            get
            {
                if (acted)
                    return operatingCost;
                else
                    return suggestedCost;
            }
            set
            {
                acted = true;
                operatingCost = value;
            }
        }

        public OCTypes OCType
        {
            get
            {
                return operatingCostType;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            OperatingCost action = (OperatingCost)pa;
            bool options = (operatingCostType == action.operatingCostType)
                    && (suggestedCost == action.suggestedCost)
                    && (maximumCost == action.maximumCost)
                    && (freeEntryAllowed == action.freeEntryAllowed);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && (operatingCost == action.operatingCost);
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("operatingCostType", operatingCostType)
                        .AddToString("suggestedCost", suggestedCost)
                        .AddToString("maximumCost", maximumCost)
                        .AddToString("freeEntryAllowed", freeEntryAllowed)
                        .AddToStringOnlyActed("operatingCost", operatingCost)
                        .ToString();           ;
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            if (!string.IsNullOrEmpty(companyName))
            {
                company = CompanyManager.GetPublicCompany(companyName);
            }
        }
    }
}
