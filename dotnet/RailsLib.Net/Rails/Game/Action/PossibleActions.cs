using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
/**
 * This class manages the actions that the current user can execute at any point
 * in time. Each possible action is represented by an instance of a subclass of
 * PossibleAction. The complete set is stored in a List.
 * 
 * TODO: Should this be changed to a set?
 */
namespace GameLib.Rails.Game.Action
{
    public class PossibleActions
    {
        private readonly List<PossibleAction> actions;

        private PossibleActions()
        {
            actions = new List<PossibleAction>();
        }

        public static PossibleActions Create()
        {
            return new PossibleActions();
        }

        public void Clear()
        {
            actions.Clear();
        }

        public void Add(PossibleAction action)
        {
            actions.Add(action);
        }

        public void Remove(PossibleAction action)
        {
            actions.Remove(action);
        }

        public void AddAll<T>(List<T> actions) where T : PossibleAction
        {
            this.actions.AddRange(actions);
        }

        public bool Contains(Type type)
        {
            foreach (PossibleAction action in actions)
            {
                if (type.IsAssignableFrom(action.GetType())) return true;
            }
            return false;
        }

        // Removed, because I think the one below is good enough
        //public List<T> GetActionType<T>(Type type) where T : PossibleAction
        //{
        //    List<T> result = new List<T>();
        //    foreach (PossibleAction action in actions)
        //    {
        //        if (type.IsAssignableFrom(action.GetType()))
        //        {
        //            result.Add((T)action);
        //        }
        //    }
        //    return result;
        //}

        public List<T> GetActionType<T>() where T : PossibleAction
        {
            List<T> result = new List<T>();
            foreach (PossibleAction action in actions)
            {
                if (typeof(T).IsAssignableFrom(action.GetType()))
                {
                    result.Add((T)action);
                }
            }
            return result;
        }

        public List<PossibleAction> GetList()
        {
            return new List<PossibleAction>(actions); //ImmutableList.copyOf(actions);
        }

        public bool IsEmpty
        {
            get
            {
                return actions.Count == 0;
            }
        }

        public bool ContainsOnlyPass()
        {
            if (actions.Count != 1) return false;
            PossibleAction action = actions[0];
            if (action is NullAction && ((NullAction)action).Mode == NullAction.Modes.PASS)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        public bool ContainsCorrections()
        {
            foreach (PossibleAction action in actions)
            {
                if (action.IsCorrection) return true;
            }
            return false;
        }

        /** Check if a given action exists in the current list of possible actions */
        public bool Validate(PossibleAction checkedAction)
        {

            // Some actions are always allowed
            var checkList = new List<GameAction.Modes> { GameAction.Modes.SAVE, GameAction.Modes.RELOAD, GameAction.Modes.EXPORT };
            if (checkedAction is GameAction && checkList.Contains(((GameAction)checkedAction).Mode))
            {
                return true;
            }

            // Check if action occurs in the list of possible actions
            foreach (PossibleAction action in actions)
            {
                if (action.EqualsAsOption(checkedAction))
                {
                    return true;
                }
            }
            return false;
        }
    }
}
