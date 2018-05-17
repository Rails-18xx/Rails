using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameStateTest
{
    public class CountableItemImpl : CountableItem<ICountable>
    {
        protected CountableItemImpl(IItem parent, string id) : base(parent, id)
        {
        }

        public static CountableItemImpl Create(IItem parent, string id)
        {
            return new CountableItemImpl(parent, id);
        }
    }
}
