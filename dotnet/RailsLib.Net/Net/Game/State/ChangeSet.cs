using GameLib.Net.Common;
using GameLib.Net.Util;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
/**
* A ChangeSet object represents the collection of all changes
* that belong to the same activity.
* 
* ChangeSet objects are stored in the ChangeStack.
*/
namespace GameLib.Net.Game.State
{
    public class ChangeSet
    {
        private static readonly Logger<ChangeSet> log = new Logger<ChangeSet>();

        // static fields
        private readonly List<Change> changes;
        private readonly IChangeAction action;
        private readonly int index;

        public ChangeSet(List<Change> changes, IChangeAction action, int index)
        {
            this.changes = changes;
            this.action = action;
            this.index = index;
        }

        /**
         * retrieves all states that are changed by Changes in the ChangeSet
         * @return set of all states affected by Changes
         */
        public IReadOnlyList<GameState> GetStates()
        {
            List<GameState> builder = new List<GameState>();
            foreach (Change change in changes)
            {
                builder.Add(change.GameState);
            }
            return builder;
        }

        /**
         * re-execute all Changes in the ChangeSet (redo)
         * @ŧhrows IllegalStateException if ChangeSet is still open 
         */
        public void Reexecute()
        {
            foreach (Change change in changes)
            {
                change.Execute();
                log.Debug("Redo: " + change);
            }
        }

        /**
         * un-executed all Changes in the ChangeSet (undo)
         * @throws IllegalStateException if ChangeSet is still open or ChangeSet is initial
         */
        public void Unexecute()
        {
            Precondition.CheckState(index != -1, "ChangeSet is initial - cannot be undone");

            var rev = new List<Change>(changes);
            rev.Reverse();
            // iterate reverse
            foreach (Change change in rev)
            {
                log.Debug("About to undo: " + change);
                change.Undo();
                log.Debug("Undone: " + change);
            }
        }

        /**
         * returns the ChangeAction associated with the ChangeSet
         * @return the associated ChangeAction
         */
        IChangeAction Action
        {
            get
            {
                return action;
            }
        }

        /**
         * returns the Owner associated with the ChangeSet
         */
        public IChangeActionOwner Owner
        {
            get
            {
                return action.ActionOwner;
            }
        }

        public int Index
        {
            get
            {
                return index;
            }
        }

        override public string ToString()
        {
            //return Objects.toStringHelper(this).add("action", action)
            //        .add("Owner", getOwner())
            //        .add("Index", index)
            //        .toString();
            return $"{this.GetType().Name}{{Action={Action}}} {{Owner={Owner}}} {{Index={Index}}}";
        }
    }
}
