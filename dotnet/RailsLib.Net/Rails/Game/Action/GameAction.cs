using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
// This class does not need to be serialized
// TODO: This will have to change as soon as actions are used for online play

/**
 * Rails 2.0: Updated equals and toString methods
 */
namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class GameAction : PossibleAction
    {
        new private const long serialVersionUID = 1L;

        public enum Modes { SAVE, LOAD, UNDO, FORCED_UNDO, REDO, EXPORT, RELOAD }

        // Server-side settings
        protected Modes mode;

        // Client-side settings
        protected string filePath = null; // Only applies to SAVE, LOAD and RELOAD
        protected int moveStackIndex = -1; // target moveStackIndex, only for FORCED_UNDO and REDO

        public GameAction(Modes mode) : base(null)
        {
            //super(null); // not defined by an activity yet
            this.mode = mode;
        }

        public string Filepath
        {
            get
            {
                return filePath;
            }
            set
            {
                filePath = value;
            }
        }

        public int MoveStackIndex
        {
            get
            {
                return moveStackIndex;
            }
            set
            {
                moveStackIndex = value;
            }
        }

        public Modes Mode
        {
            get
            {
                return mode;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            GameAction action = (GameAction)pa;
            bool options = mode == action.mode;

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options
                    && filePath.Equals(action.filePath)
                    && moveStackIndex == action.moveStackIndex;
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("mode", mode)
                        .AddToStringOnlyActed("filePath", filePath)
                        .AddToStringOnlyActed("moveStackIndex", moveStackIndex)
                        .ToString();
        }
    }
}
