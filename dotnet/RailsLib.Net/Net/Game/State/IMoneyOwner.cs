using System;

namespace GameLib.Net.Game.State
{
    public interface IMoneyOwner : IOwner
    {
        Purse Purse { get; }

        int Cash { get; }
    }
}
