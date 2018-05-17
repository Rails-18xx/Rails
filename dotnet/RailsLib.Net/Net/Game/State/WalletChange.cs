using System;


namespace GameLib.Net.Game.State
{
    public class WalletChange<T> : Change where T : ICountable
    {
        private readonly Wallet<T> wallet;
        private readonly T item;
        private readonly int amount;

        public WalletChange(Wallet<T> wallet, T item, int amount)
        {
            this.wallet = wallet;
            this.item = item;
            this.amount = amount;
            base.Init(wallet);
        }

        override public void Execute()
        {
            wallet.Change(item, amount);
        }


        override public void Undo()
        {
            wallet.Change(item, -amount);
        }

        public override GameState GameState
        {
            get
            {
                return wallet;
            }
        }

        override public string ToString()
        {
            return "Change for " + wallet + ": " + amount + " of " + item;
        }
    }
}
