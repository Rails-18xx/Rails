using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.State
{
    public interface IOwnable : IItem, IComparable<IOwnable>
    {
        /**
        * Moves the ownable (item) to the new owner  
        * @param newOwner the new Owner of the Item
        * @throws IllegalArgumentException if the new owner has no wallet which accepts the item
        * @throws IllegalArgumentException if the new owner is identical to the current one 
        */
        void MoveTo(IOwner newOwner);

        /**
         * @return the current owner
         */
        IOwner Owner { get; }
    }
}
