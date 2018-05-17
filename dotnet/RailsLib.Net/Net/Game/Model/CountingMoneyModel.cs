using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.Model
{
    public class CountingMoneyModel : MoneyModel
    {
        private IntegerState value;
        private BooleanState initialized;

        private CountingMoneyModel(IRailsItem parent, string id, int amount, bool init, Currency currency) : base(parent, id, currency)
        {
            value = IntegerState.Create(this, "counting", amount);
            initialized = BooleanState.Create(this, "initialized", init);
        }

        public static CountingMoneyModel Create(IRailsItem parent, string id, bool init)
        {
            Currency currency = parent.GetRoot.Bank.Currency;
            return new CountingMoneyModel(parent, id, 0, init, currency);
        }

        public static CountingMoneyModel Create(IRailsItem parent, string id, int amount)
        {
            Currency currency = parent.GetRoot.Bank.Currency;
            return new CountingMoneyModel(parent, id, amount, true, currency);
        }

        /**
         * @param amount the new cash amount
         */
        public void Set(int amount)
        {
            if (!initialized.Value)
            {
                initialized.Set(true);
            }
            value.Set(amount);
        }

        // Countable interface
        public void Change(int amount)
        {
            if (initialized.Value)
            {
                value.Add(amount);
            }
            else
            {
                Set(amount);
            }
        }

        // MoneyModel abstracts
        override public int Value
        {
            get
            {
                return value.Value;
            }
        }

        override public bool Initialized
        {
            get
            {
                return initialized.Value;
            }
        }
    }
}
