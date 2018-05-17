using System;

/**
 * Used to initialize ownable items with an owner
 * PortfolioManager creates a Singleton of this class
 */

namespace GameLib.Net.Game.State
{
    public class UnknownOwner : AbstractItem, IOwner
    {
        private UnknownOwner(IItem parent, string id) : base(parent, id)
        {
        }

        public static UnknownOwner Create(IItem parent, string id)
        {
            return new UnknownOwner(parent, id);
        }
    }
}
