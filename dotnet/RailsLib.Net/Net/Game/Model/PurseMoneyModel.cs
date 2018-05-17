using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * A MoneyModel with a wallet inside

 */
namespace GameLib.Net.Game.Model
{
    public class PurseMoneyModel : MoneyModel
    {
        private Purse purse;

        private BooleanState initialized;

        private PurseMoneyModel(IRailsMoneyOwner parent, string id, Boolean init, Currency currency) : base(parent, id, currency)
        {
            purse = Purse.Create(parent, "purse", currency);
            purse.AddModel(this);
            initialized = BooleanState.Create(this, "initialized", init);
        }

        public static PurseMoneyModel Create(Bank parent, string id, bool init, Currency currency)
        {
            return new PurseMoneyModel(parent, id, init, currency);
        }

        public static PurseMoneyModel Create(IRailsMoneyOwner parent, string id, Boolean init)
        {
            Currency currency = parent.GetRoot.Bank.Currency;
            return new PurseMoneyModel(parent, id, init, currency);
        }

        new public IRailsMoneyOwner Parent
        {
            get
            {
                return (IRailsMoneyOwner)base.Parent;
            }
        }

        public Purse Purse
        {
            get
            {
                return purse;
            }
        }

        // MoneyModel abstracts
        override public int Value
        {
            get
            {
                return purse.Value();
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
