using GameLib.Net.Common;
using GameLib.Net.Util;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameLib.Net.Game.State
{
    public class ChangeStack
    {
        private static Logger<ChangeStack> log = new Logger<ChangeStack>();

    // static fields
    private readonly StateManager stateManager;

        private readonly Deque<ChangeSet> undoStack = new Deque<ChangeSet>();
        private readonly Deque<ChangeSet> redoStack = new Deque<ChangeSet>();

        private IChangeReporter reporter; // assigned once

        // dynamic fields
        private List<Change> changeBuilder;

        private ChangeStack(StateManager stateManager)
        {
            this.stateManager = stateManager;
            reporter = null;
            changeBuilder = new List<Change>();
        }

        /**
         * Creates a new ChangeStack
         * It is initialized automatically, as there is an open ChangeBuilder
         */
        public static ChangeStack Create(StateManager stateManager)
        {
            ChangeStack changeStack = new ChangeStack(stateManager);
            return changeStack;
        }

        /**
         * Add ChangeReporter
         */
        public void AddChangeReporter(IChangeReporter reporter)
        {
            this.reporter = reporter;
            reporter.Init(this);
            log.Debug("Added ChangeReporter " + reporter);
        }

        /**
         * @return the previous (closed) changeSet, null if empty
         */
        public ChangeSet GetClosedChangeSet()
        {
            if (undoStack.Count == 0)
                return null;

            return undoStack[undoStack.Count - 1];
        }

        /**
         * Add change to current changeSet
         */
        public void AddChange(Change change)
        {
            log.Debug("ChangeSet: Add " + change);
            changeBuilder.Add(change);
            // immediate execution and information of models
            change.Execute();
            change.GameState.InformTriggers(change);
        }

        private bool CheckRequirementsForClose(IChangeAction action)
        {
            if ((changeBuilder.Count == 0) || action == null)
            {
                return false;
            }
            else
            {
                return true;
            }
        }

        public void Close(IChangeAction action)
        {
            if (CheckRequirementsForClose(action))
            {
                // this has to be done before the changeBuilder closes
                int index = undoStack.Count + 1;
                ChangeSet closeSet = new ChangeSet(new List<Change>(changeBuilder), action, index);
                log.Debug("<<< Closed changeSet " + closeSet);
                undoStack.AddToBack(closeSet);
                redoStack.Clear();

                if (reporter != null)
                {
                    reporter.UpdateOnClose();
                }

                // restart builders
                Restart();
                // inform direct and indirect observers
                UpdateObservers(closeSet.GetStates());
            }
        }

        private void Restart()
        {
            changeBuilder = new List<Change>();
        }


        public void UpdateObservers(IEnumerable<GameState> states)
        {
            // update the observers of states and models
            log.Debug("ChangeStack: update Observers");
            stateManager.UpdateObservers(states);
        }

        // is undo possible (protect first index) 
        public bool IsUndoPossible()
        {
            return (undoStack.Count > 1);
        }

        public bool IsUndoPossible(IChangeActionOwner owner)
        {
            return (IsUndoPossible() &&
                    undoStack[undoStack.Count - 1].Owner == owner);
        }

        /**
         * Undo command
         */
        public void Undo()
        {
            Precondition.CheckState(IsUndoPossible(), "Undo not possible");
            ChangeSet undoSet = ExecuteUndo();
            Restart();
            UpdateObservers(undoSet.GetStates());

            if (reporter != null)
            {
                reporter.UpdateAfterUndoRedo();
            }
        }

        /**
         * Example: Undo-Stack has 4 elements (1,2,3,4), size = 4
         * Undo to index 2, requires removing the latest element, such that size = 3
         */

        public void Undo(int index)
        {
            Precondition.CheckState(IsUndoPossible() && index < undoStack.Count, "Undo not possible");
            var states = new List<GameState>();
            while (undoStack.Count > index)
            {
                states.AddRange(ExecuteUndo().GetStates());
            }
            Restart();
            UpdateObservers(states);
            if (reporter != null)
            {
                reporter.UpdateAfterUndoRedo();
            }
        }

        private ChangeSet ExecuteUndo()
        {
            ChangeSet undoSet = undoStack.RemoveFromBack();
            log.Debug("UndoSet = " + undoSet);
            undoSet.Unexecute();
            redoStack.AddToFront(undoSet);

            if (reporter != null)
            {
                reporter.InformOnUndo();
            }

            return undoSet;
        }


        public bool IsRedoPossible()
        {
            return (redoStack.Count != 0);
        }

        public bool IsRedoPossible(IChangeActionOwner owner)
        {
            return (IsRedoPossible() &&
                    redoStack[0].Owner == owner);
        }

        /**
         * Redo command
         * @throws IllegalStateException if redo stack is empty or there is an open ChangeSet
         */
        public void Redo()
        {
            Precondition.CheckState(IsRedoPossible(), "Redo not possible");

            ChangeSet redoSet = ExecuteRedo();
            Restart();
            UpdateObservers(redoSet.GetStates());
            if (reporter != null)
            {
                reporter.UpdateAfterUndoRedo();
            }
        }

        public void Redo(int index)
        {
            Precondition.CheckState(index > undoStack.Count && index <= undoStack.Count + redoStack.Count,
                    "Redo not possible");

            List<GameState> states = new List<GameState>();
            while (undoStack.Count < index)
            {
                states.AddRange(ExecuteRedo().GetStates());
            }
            Restart();
            UpdateObservers(states);
            if (reporter != null)
            {
                reporter.UpdateAfterUndoRedo();
            }
        }

        private ChangeSet ExecuteRedo()
        {
            ChangeSet redoSet = redoStack.RemoveFromFront();
            log.Debug("RedoSet = " + redoSet);
            redoSet.Reexecute();
            undoStack.AddToBack(redoSet);

            if (reporter != null)
            {
                reporter.InformOnRedo();
            }

            return redoSet;
        }

        /**
         * @return current index of the ChangeStack (equal to size of undo stack)
         */
        public int CurrentIndex
        {
            get
            {
                return undoStack.Count;
            }
        }

        /**
         * @return size of undoStack plus RedoStack
         */
        public int MaximumIndex
        {
            get
            {
                return redoStack.Count + undoStack.Count;
            }
        }
    }
}
