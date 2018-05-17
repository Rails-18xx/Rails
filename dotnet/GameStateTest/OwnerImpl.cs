using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

/**
 * Implementation of an Owner used for Testing
 */

namespace GameStateTest
{
    public class OwnerImpl : AbstractItem, IOwner
    {
        private OwnerImpl(IItem parent, string id) : base(parent, id)
        {
        }

        public static OwnerImpl Create(IItem parent, String id)
        {
            return new OwnerImpl(parent, id);
        }

        override public string ToString()
        {
            return "Owner(Id=" + Id + ")";
        }
    }
}
