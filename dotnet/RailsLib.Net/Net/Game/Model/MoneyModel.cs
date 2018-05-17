using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * The base model for money
 * FIXME: Removed "" equivalence for null in setText
 * FIXME: PublicCompany money is shown as "" as long as it has not started, this
 * was coded inside the toString() method

 */
namespace GameLib.Net.Game.Model
{
    abstract public class MoneyModel : RailsModel
    {
        public const int CASH_DEFAULT = 0;

        // Data
        private StringState fixedText;
        private Currency currency;

        // Format Options (with defaults)
        private BooleanState suppressZero;
        private bool suppressInitialZero = false;
        private bool addPlus = false;
        private bool displayNegative = false;

        protected MoneyModel(IRailsItem parent, string id, Currency currency) : base(parent, id)
        {
            fixedText = StringState.Create(this, "fixedText");
            suppressZero = BooleanState.Create(this, "suppressZero");
            this.currency = currency;
        }

        public Currency Currency
        {
            get
            {
                return currency;
            }
        }

        /**
         * @param suppressZero true: displays an empty string instead of a zero value
         * This is a state variable, thus can be changed after initialization
         */
        public void SetSuppressZero(bool suppressZero)
        {
            this.suppressZero.Set(suppressZero);
        }

        /**
         * @param suppressInitialZero true: displays an empty string for the initial zero value
         * This is not a state variable, so do not change after the MoneyModel is used
         */
        public void SetSuppressInitialZero(bool suppressInitialZero)
        {
            this.suppressInitialZero = suppressInitialZero;
        }

        /**
         * @param addPlus true: adds a plus sign for positive values
         * This is not a state variable, so do not change after the MoneyModel is used
         */
        public void SetAddPlus(bool addPlus)
        {
            this.addPlus = addPlus;
        }

        /**
         * @param displayNegative true: does not display negative values
         * This is not a state variable, so do not change after the MoneyModel is used
         */
        public void SetDisplayNegative(bool displayNegative)
        {
            this.displayNegative = displayNegative;
        }

        /** 
         * @param text fixed text to be displayed instead of money value
         * using null removes text and displays value again
         * Remark: Setting the text triggers an update of the model
         */
        public void SetText(string text)
        {
            fixedText.Set(text); // this triggers the update of the model
        }

        /**
         * @return current value of the MoneyModel
         */
        public abstract int Value { get; }


        /**
         * @return formatted value of the MoneyModel
         */
        public string FormattedValue()
        {
            return currency.Format(Value);
        }

        /**
         * @return true if MoneyValue has a value set already
         */
        public abstract bool Initialized { get; }


        override public string ToText()
        {
            if (fixedText.Value != null)
            {
                return fixedText.Value;
            }
            int amount = this.Value;
            if (amount == 0
                && (suppressZero.Value
                        || suppressInitialZero
                            && !Initialized))
            {
                return "";
            }
            else if (amount < 0 && !displayNegative)
            {
                return "";
            }
            else if (addPlus)
            {
                return "+" + currency.Format(amount);
            }
            else
            {
                return currency.Format(amount);
            }
        }
    }
}
