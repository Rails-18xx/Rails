using System;


/**
 * The owner of a currency (typically something like a bank)
 */

namespace GameLib.Net.Game.State
{
    public interface ICurrencyOwner : IMoneyOwner
    {
        Currency Currency { get; }
    }
}
