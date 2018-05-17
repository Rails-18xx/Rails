using GameLib.Rails.Game.Action;
using GameLib.Net.Game.State;
using System;


namespace GameLib.Net.Game.GameRound
{
    abstract public class Activity : RailsAbstractItem
    {
        private readonly BooleanState enabled; 


    protected Activity(RoundNG parent, String id) : base(parent, id)
        {
            enabled = BooleanState.Create(this, "enabled");
        }

        public void SetEnabled(bool enabled)
        {
            this.enabled.Set(enabled);
        }

        public bool IsEnabled
        {
            get
            {
                return enabled.Value;
            }
        }

        /**
         * create actions and add them to the possibleActions object
         */
        public abstract void CreateActions(IActor actor, PossibleActions actions);

        /**
         * checks if the conditions of the actions are fulfilled
         */
        public abstract bool IsActionExecutable(PossibleAction action);

        /**
         * executes the action
         */
        public abstract void ExecuteAction(PossibleAction action);

        /**
         * reports action execution
         */
        public abstract void ReportExecution(PossibleAction action);
    }
}
