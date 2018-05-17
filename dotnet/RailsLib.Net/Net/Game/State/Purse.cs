using GameLib.Net.Util;
using System;
using System.Collections.Generic;

/**
 * Purse is a wallet that allows to store money of only one currency
 * This currency is set at time of creation
 */

namespace GameLib.Net.Game.State
{
    public class Purse : Wallet<Currency>
    {
        private readonly Currency currency;
        private int amount = 0;

        private Purse(IOwner parent, string id, Currency currency) : base(parent, id)
        {
            this.currency = currency;
        }

        /**
         * Creates an empty WalletBag
         */
        public static Purse Create(IOwner parent, string id, Currency currency)
        {
            return new Purse(parent, id, currency);
        }

        /**
         * @return currency of the purse
         */
        public Currency Currency
        {
            get
            {
                return currency;
            }
        }

        override public int Value(Currency currency)
        {
            Precondition.CheckArgument(currency == this.currency, "Purse only accepts " + this.currency);
            return amount;
        }

        override public int Value()
        {
            return amount;
        }

        override public void Change(Currency item, int value)
        {
            amount += value;
        }

        override public string ToText()
        {
            return currency.Format(amount);
        }
    }
}
