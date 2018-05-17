using GameLib.Net.Game.State;
using System;

/**
 * RailsManager adds Rails specific methods to Manager
 */
namespace GameLib.Net.Game
{
    abstract public class RailsManager : Manager, IRailsItem
    {
        protected RailsManager(IRailsItem parent, String id) : base(parent, id)
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
