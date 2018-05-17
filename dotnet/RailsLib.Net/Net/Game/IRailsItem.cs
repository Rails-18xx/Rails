using GameLib.Net.Game.State;
using System;

/**
 * Adapts Item to the Rails environment
 */
namespace GameLib.Net.Game
{
    public interface IRailsItem : IItem<IRailsItem, RailsRoot>
    {
        //IRailsItem Parent { get; }
        //IRailsRoot GetRoot { get; }
    }
}
