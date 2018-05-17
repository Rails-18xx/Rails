using System;

/**
 * A ChangeAction can be related to a ChangeSet
 */
namespace GameLib.Net.Game.State
{
    public interface IChangeAction
    {
        IChangeActionOwner ActionOwner { get; }
    }
}
