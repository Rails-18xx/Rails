using System;
using System.Collections.Generic;


namespace GameLib.Net.Game.State
{
    abstract public class Wallet<T> : GameState where T : ICountable
    {

        private readonly Type type;

        /**
         * Creation of a wallet
         * @param parent owner of the wallet
         * @param id identifier of the wallet
         * @param type type of items stored in the wallet
         */
        protected Wallet(IOwner parent, string id) : base(parent, id)
        {
            this.type = typeof(T);
            WalletManager.AddWallet(this);
        }

        protected WalletManager WalletManager
        {
            get
            {
                return StateManager.WalletManager;
            }
        }

        /**
         * @return the owner of the wallet
         */
        new public IOwner Parent
        {
            get
            {
                return (IOwner)base.Parent;
            }
        }

        /**
         * @param item for which the value is retrieved
         * @return the current amount of the item inside the wallet
         */
        public abstract int Value(T item);

        /**
         * @return total value of all items
         */
        public abstract int Value();


        public Type GetWalletType()
        {
            return type;
        }

        public abstract void Change(T item, int value);
    }
}
