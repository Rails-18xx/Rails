using GameLib.Net.Util;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.State
{
    public class OwnableItem<T> : AbstractItem, IOwnable, IComparable<IOwnable> where T : IOwnable
    {
        private readonly Type type;
        private readonly PortfolioManager pm;
        private GenericState<IOwner> owner;

        /**
         * Initializes OwnableItem
         * @param parent parent is usually a factory that creates the OwnableItem(s)  
         * @param id identifier of the item
         * @param type indicates the class used for the portfolios to store this type of OwnableItems
         */
        protected OwnableItem(IItem parent, String id /*, Type type*/) : base(parent, id)
        {
            owner = GenericState<IOwner>.Create(this, "owner");
            this.type = typeof(T); //T.GetType(); // type;
            this.pm = GetRoot.StateManager.PortfolioManager;
            this.owner.Set(pm.UnknownOwner);
        }

        public void MoveTo(IOwner newOwner)
        {
            Precondition.CheckArgument(newOwner != owner.Value,
                    "New Owner identical to the existing owner" + newOwner);


            // check newPortfolio
            Portfolio<T> newPortfolio = pm.GetPortfolio<T>(newOwner);
            Precondition.CheckArgument(newPortfolio != null, "No Portfolio available for owner " + newOwner);

            // create change for new portfolio
            newPortfolio.Include((T)(object)(this));

            //  remove from old portfolio
            if (owner.Value != pm.UnknownOwner)
            {
                Portfolio<T> oldPortfolio = pm.GetPortfolio<T>(owner.Value);
                oldPortfolio.Exclude((T)(object)(this));
            }

            // and change the owner
            owner.Set(newOwner);
        }

        public IOwner Owner
        {
            get
            {
                return owner.Value;
            }
        }

        public void TriggeredOnOwnerChange(ITriggerable t)
        {
            owner.AddTrigger(t);
        }

        virtual public int CompareTo(IOwnable other)
        {
            return this.Id.CompareTo(other.Id);
        }
    }
}
