using GameLib.Net.Game;
using GameLib.Net.Game.State;
using GameLib.Net.Util;
using GameLib.Rails.Game.Action;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

/**
 * Correction action that changes the cash position of a MoneyOwner.
 * 
 * Rails 2.0: updated equals and toString methods
 */

namespace GameLib.Rails.Game.Correct
{
    [JsonObject(MemberSerialization.Fields)]
    public class CashCorrectionAction : CorrectionAction
    {
        /** The Constant serialVersionUID. */
        new public const long serialVersionUID = 1L;

        /* Preconditions */

        /** cash holder */
        /*transient*/
        [JsonIgnore]
        private IMoneyOwner correctCashHolder;

        /** converted to name */
        private string cashHolderName;
        private string cashHolderType;

        /** maximum Amount to deduct */
        private int maximumNegative;

        /* Postconditions */

        /** selected cash amount */
        private int correctAmount;

        /**
         * Instantiates a new correct cash
         * 
         * @param pl Player
         */
        public CashCorrectionAction(Player pl)
        {
            correctCashHolder = pl;
            cashHolderName = pl.Id;
            cashHolderType = "Player";
            maximumNegative = pl.CashValue;
            SetCorrectionType(CorrectionType.CORRECT_CASH);
        }

        /**
         * Instantiates a new correct cash
         * 
         * @param pc Public Company
         */
        public CashCorrectionAction(PublicCompany pc)
        {
            correctCashHolder = pc;
            cashHolderName = pc.Id;
            cashHolderType = "PublicCompany";
            maximumNegative = pc.Cash;
            SetCorrectionType(CorrectionType.CORRECT_CASH);
        }

        public IMoneyOwner CashHolder
        {
            get
            {
                return correctCashHolder;
            }
        }

        public string CashHolderName
        {
            get
            {
                return cashHolderName;
            }
        }

        public int MaximumNegative
        {
            get
            {
                return maximumNegative;
            }
        }

        public int Amount
        {
            get
            {
                return correctAmount;
            }
            set
            {
                correctAmount = value;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            CashCorrectionAction action = (CashCorrectionAction)pa;
            bool options = correctCashHolder.Equals(action.correctCashHolder)
                    && (maximumNegative == action.maximumNegative);

            // finish if asOptions check
            if (asOption) return options;

            return options && (correctAmount == action.correctAmount);
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("correctCashHolder", correctCashHolder)
                        .AddToString("maximumNegative", maximumNegative)
                        .AddToStringOnlyActed("correctAmount", correctAmount)
                    .ToString();
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            if (!string.IsNullOrEmpty(correctionName))
                correctionType = CorrectionType.ValueOf(correctionName);

            if (!string.IsNullOrEmpty(cashHolderType) && !string.IsNullOrEmpty(cashHolderName))
            {
                if (cashHolderType.Equals("Player"))
                {
                    correctCashHolder = GameManager.GetRoot.PlayerManager.GetPlayerByName(cashHolderName);
                }
                else if (cashHolderType.Equals("PublicCompany"))
                {
                    correctCashHolder = CompanyManager.GetPublicCompany(cashHolderName);
                }
            }
        }
    }
}
