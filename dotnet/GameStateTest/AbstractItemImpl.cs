using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

// Implementation for Testing only
namespace GameStateTest
{
    public class AbstractItemImpl : AbstractItem
    {
        public AbstractItemImpl(IItem parent, string id) : base(parent, id)
        {
        }

        public static AbstractItemImpl Create(IItem parent, string id)
        {
            return new AbstractItemImpl(parent, id);
        }

        override public string ToString()
        {
            return "Item(Id=" + Id + ")";
        }
    }
}
