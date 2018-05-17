using System;
using GameLib.Rails.Game.Action;
/**
* RoundNG is the abstract base class for Round types (like StockRound, OperatingRound, StartRound) in Rails.
*/
namespace GameLib.Net.Game.GameRound
{
    abstract public class RoundNG : RailsManager, IRoundFacade
    {
        public abstract string RoundName { get; }

        protected RoundNG(IRailsItem parent, String id) : base(parent, id)
        {
        }

        public abstract void Start();
        public abstract void Finish();
        public abstract bool Process(PossibleAction action);
        public abstract bool SetPossibleActions();
        public abstract void Resume();
        public abstract void ProcessPhaseAction(string name, string value);
    }
}
