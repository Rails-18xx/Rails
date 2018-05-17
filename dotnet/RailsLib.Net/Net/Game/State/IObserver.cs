using System;

/**
 * An interface defining an Observer to Observable classes
 */
namespace GameLib.Net.Game.State
{
    public interface IObserver
    {
        void Update(string text);

        Observable Observable { get; }
    }
}
