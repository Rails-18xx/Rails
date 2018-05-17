using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * RailsOwnableItem is the rails specific version of RailsOwnableItem
 */

namespace GameLib.Net.Game
{
    public class RailsOwnableItem<T> : OwnableItem<T>, IRailsItem where T : IOwnable
    {
        protected RailsOwnableItem(IRailsItem parent, string id /*, Class<T> type */) : base(parent, id)
        {
        }

        IRailsItem IItem<IRailsItem, RailsRoot>.Parent => (IRailsItem)base.Parent;

        RailsRoot IItem<IRailsItem, RailsRoot>.GetRoot => (RailsRoot)base.GetRoot;

        new public IRailsItem Parent
        {
            get
            {
                return (IRailsItem)base.Parent;
            }
        }

        new public RailsRoot GetRoot
        {
            get
            {
                return (RailsRoot)base.GetRoot;
            }
        }

        /**
         * Moves the item to the owner of the portfolioModel
         * @param model the model of the new owner 
         */
        public void MoveTo(PortfolioModel model)
        {
            MoveTo(model.Parent);
        }
    }
}
