using System;


namespace GameLib.Net.Game.State
{
    public interface ITriggerable
    {
        /**
        * Method that is called if something has changed
        */
        void Triggered(Observable observable, Change change);
    }
}
