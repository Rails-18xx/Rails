using System;

/**
 * Identifies items which are countable
 * They are stored inside wallets
 */

namespace GameLib.Net.Game.State
{
    public interface ICountable : IItem, IComparable<ICountable>
    {
        /**
           * Moves the countable (item) from one to another owner
           * @param from the previous owner
           * @param int amount
           * @param to the new owner
           * @throws IllegalArgumentException if the new or the previous owner has no wallet which accepts the item
           * @throws IllegalArgumentException if the new owner is identical to the current one 
           */
        void Move(IOwner from, int amount, IOwner to);
    }
}
