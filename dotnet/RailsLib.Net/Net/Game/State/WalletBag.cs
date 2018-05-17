using GameLib.Net.Util;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * A WalletBag allows the storage of only one item of the specified class
 */

namespace GameLib.Net.Game.State
{
    public class WalletBag<T> : Wallet<T> where T : ICountable
    {
        private T item;
        private int amount = 0;

        private WalletBag(IOwner parent, string id, T item) : base(parent, id)
        {
            this.item = item;
        }

        /**
         * Creates an empty WalletBag
         */
        public static WalletBag<T> Create(IOwner parent, string id, T item)
        {
            return new WalletBag<T>(parent, id, item);
        }

        /**
         * @param item for which the value is retrieved
         * @return the current amount of the item inside the wallet
         */
        override public int Value(T item)
        {
            Precondition.CheckArgument(item.Equals(this.item), "WalletBag only accepts item " + this.item);
            return amount;
        }

        override public int Value()
        {
            return amount;
        }

        override public void Change(T item, int value)
        {
            amount += value;
        }


        override public string ToText()
        {
            return amount.ToString();
        }

    }
}
