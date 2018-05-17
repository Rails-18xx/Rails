using GameLib.Net.Util;
using System;


namespace GameLib.Net.Game.State
{
     public class CountableItem<T> : AbstractItem, ICountable where T : ICountable
    {
        private readonly Type type;
        private readonly WalletManager wm;
    
    /**
     * Initializes CountableItem
     * @param parent parent is usually a factory that creates the CountableItem(s)  
     * @param id identifier of the item
     * @param type indicates the class used for the Wallets to store this type of CountableItems
     */
    protected CountableItem(IItem parent, string id /*, Type type*/) : base(parent, id)
        {
            this.type = typeof(T); //type;
            this.wm = GetRoot.StateManager.WalletManager;
        }

        public void Move(IOwner from, int amount, IOwner to)
        {
            Precondition.CheckArgument(from != to, "New Owner identical to the existing owner" + to);
            // TODO: Currently we still allow zero amounts
            // as e.g. during withhold the zero amount is paid
            Precondition.CheckArgument(amount >= 0, "Amount to move restricted to positive numbers");

            // add to new wallet
            Wallet<T> newWallet = wm.GetWallet<T>(type, to);
            Precondition.CheckArgument(newWallet != null, "No Wallet available for owner " + to);
            new WalletChange<T>(newWallet, (T)(object)this, amount);

            if (from != wm.UnknownOwner)
            {
                Wallet<T> oldWallet = wm.GetWallet<T>(type, from);
                Precondition.CheckArgument(oldWallet != null, "No Wallet available for owner" + from);
                new WalletChange<T>(oldWallet, (T)(object)this, -amount);
            }
        }

        public int CompareTo(ICountable other)
        {
            return this.Id.CompareTo(other.Id);
        }

    }
}
