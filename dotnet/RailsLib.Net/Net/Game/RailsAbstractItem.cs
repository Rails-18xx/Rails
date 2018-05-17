using GameLib.Net.Game.State;
using System;


namespace GameLib.Net.Game
{
    abstract public class RailsAbstractItem : AbstractItem, IRailsItem
    {
        protected RailsAbstractItem(IRailsItem parent, string id) : base(parent, id)
        {
        }

        new public IRailsItem Parent
        {
            get
            {
                return (IRailsItem)base.Parent;
            }
        }

        //public IItem IItem.Parent => Parent;

        new public RailsRoot GetRoot
        {
            get
            {
                return (RailsRoot)base.GetRoot;
            }
        }
    }
}
