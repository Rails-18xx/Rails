using GameLib.Rails.Game.Action;
using GameLib.Net.Game.State;
using System;


namespace GameLib.Net.Game.GameRound
{
    public interface IRoundFacade : ICreateable, IRailsItem
    {
        // called from GameManager
        bool Process(PossibleAction action);

        // called from GameManager and GameLoader
        bool SetPossibleActions();

        // called from GameManager
        void Resume();

        // called from GameManager and GameUIManager
        string RoundName { get; }

        /** A stub for processing actions triggered by a phase change.
         * Must be overridden by subclasses that need to process such actions.
         * @param name (required) The name of the action to be executed
         * @param value (optional) The value of the action to be executed, if applicable
         */
        // can this be moved to GameManager, not yet as there are internal dependencies
        // called from GameManager
        void ProcessPhaseAction(string name, string value);
    }
}
