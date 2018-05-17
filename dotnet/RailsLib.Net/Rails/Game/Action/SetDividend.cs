using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;

/**
 * Action class that comprises the earnings setting and distribution steps. In
 * the current versions, the earnings must always be calculated and entered by
 * the user. In a later version, the earnings may have been calculated by the
 * back-end; in that case, the user can only select the earnings distribution
 * method.
 *
 * Rails 2.0: Updated equals and toString methods
 */

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class SetDividend : PossibleORAction, ICloneable
    {
        public const int UNKNOWN = -1;
        public const int WITHHOLD = 0;
        public const int SPLIT = 1;
        public const int PAYOUT = 2;
        public const int NO_TRAIN = 3;
        public const int NUM_OPTIONS = 4;

        /** Allocation name keys in the resource bundle */
        public static readonly string[] allocationNameKeys =
            new string[] { "WITHHOLD", "SPLIT", "PAYOUT", "NO_TRAIN" };

        /*--- Server-side settings ---*/
        /**
         * The revenue as proposed by the back-end. Currently this is always the
         * previous revenue. In the future, this could be the calculated revenue.
         */
        protected int presetRevenue;

        /**
         * Is the user allowed to set the revenue? Currently, this will aways be
         * true, except if the company has no trains (the revenue is then 0). In the
         * future, it will only be true if the user has some influence on it (e.g.,
         * in 1844, the user may opt for less that maximum revenue is some cases).
         */
        protected bool mayUserSetRevenue;

        /**
         * The revenue allocations that the user may select from. If only one value
         * is provided, the user has no option (e.g. minor companies always split in
         * most games).
         */
        protected int[] allowedRevenueAllocations;

        /** Cash that should be minimally raised as revenue
         * (for instance, to pay loan interest as in 1856).
         * If actual revenue is below this value, the dividend will be zero,
         * and no dividend allocation should be requested.
         * */
        protected int requiredCash = 0;

        /*--- Client-side settings ---*/

        /** The revenue as set (or accepted, or just seen) by the user. */
        protected int actualRevenue;

        /** The revenue destination selected by the user (if he has a choice at all). */
        protected int revenueAllocation;

        new public const long serialVersionUID = 1L;

        public SetDividend(int presetRevenue, bool mayUserSetRevenue, int[] allowedAllocations) :
                this(presetRevenue, mayUserSetRevenue, allowedAllocations, 0)
        {

        }

        public SetDividend(int presetRevenue, bool mayUserSetRevenue, int[] allowedAllocations, int requiredCash) : base()
        {
            this.presetRevenue = presetRevenue;
            this.mayUserSetRevenue = mayUserSetRevenue;
            this.allowedRevenueAllocations = (int[])allowedAllocations.Clone();
            this.requiredCash = requiredCash;
            if (allowedRevenueAllocations.Length == 1)
            {
                revenueAllocation = allowedRevenueAllocations[0];
            }
            else
            {
                revenueAllocation = UNKNOWN;
            }
        }

        /** Clone an instance (used by clone) */
        protected SetDividend(SetDividend action) :
            this(action.presetRevenue, action.mayUserSetRevenue,
                    action.allowedRevenueAllocations,
                    action.requiredCash)
        {
        }

        public int PresetRevenue
        {
            get
            {
                return presetRevenue;
            }
        }

        public int ActualRevenue
        {
            get
            {
                return actualRevenue;
            }
            set
            {
                actualRevenue = value;
            }
        }

        public int[] GetAllowedAllocations()
        {
            return allowedRevenueAllocations;
        }

        public bool IsAllocationAllowed(int allocationType)
        {
            foreach (int at in allowedRevenueAllocations)
            {
                if (at == allocationType) return true;
            }
            return false;
        }

        public int RequiredCash
        {
            get
            {
                return requiredCash;
            }
        }

        public int RevenueAllocation
        {
            get
            {
                return revenueAllocation;
            }
            set
            {
                revenueAllocation = value;
            }
        }

        public static string GetAllocationNameKey(int allocationType)
        {
            if (allocationType >= 0 && allocationType < NUM_OPTIONS)
            {
                return allocationNameKeys[allocationType];
            }
            else
            {
                return "<invalid allocation type: " + allocationType + ">";
            }
        }

        public object Clone()
        {

            SetDividend result = new SetDividend(this);
            result.ActualRevenue = actualRevenue;
            result.RevenueAllocation = revenueAllocation;
            return result;
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            SetDividend action = (SetDividend)pa;
            bool options =
                    (presetRevenue == action.presetRevenue)
                    && (mayUserSetRevenue == action.mayUserSetRevenue)
                    && (Enumerable.SequenceEqual(allowedRevenueAllocations, action.allowedRevenueAllocations))
                    && (requiredCash == action.requiredCash);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && (actualRevenue == action.actualRevenue) && (revenueAllocation == action.revenueAllocation);
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("presetRevenue", presetRevenue)
                        .AddToString("mayUserSetRevenue", mayUserSetRevenue)
                        .AddToString("allowedRevenueAllocations", allowedRevenueAllocations)
                        .AddToString("requiredCash", requiredCash)
                        .AddToStringOnlyActed("actualRevenue", actualRevenue)
                        .AddToStringOnlyActed("revenueAllocation", revenueAllocation)
                        .ToString();
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
