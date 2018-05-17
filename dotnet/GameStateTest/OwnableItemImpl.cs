using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

/**
 * Test implementation of OwnableItem

 */
namespace GameStateTest
{
    public class OwnableItemImpl : OwnableItem<IOwnable>
    {
        protected OwnableItemImpl(IItem parent, string id) : base(parent, id)
        {

        }

        public static OwnableItemImpl Create(IItem parent, string id)
        {
            return new OwnableItemImpl(parent, id);
        }
    }
}
