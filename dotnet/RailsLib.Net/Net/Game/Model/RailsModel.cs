using GameLib.Net.Game.State;
using System;

/**
 * RailsModel adds Rails specific methods to Model
 */

namespace GameLib.Net.Game.Model
{
    public class RailsModel : State.Model, IRailsItem
    {
        protected RailsModel(IRailsItem parent, String id) : base(parent, id)
        {
        }

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
    }
}
