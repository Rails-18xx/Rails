using GameLib.Net.Common;
using GameLib.Net.Game;
using GameLib.Net.Game.GameRound;
using GameLib.Net.Game.State;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class PossibleAction : IChangeAction
    {
        protected string playerName;
        protected int playerIndex;
        /*transient*/
        [JsonIgnore]
        protected Player player;

        protected bool acted = false;

        /*transient*/
        [JsonIgnore]
        protected RailsRoot root;
        /*transient*/
        [JsonIgnore]
        protected Activity activity;

        public const long serialVersionUID = 3L;

        protected static Logger<PossibleAction> log = new Logger<PossibleAction>();

        // TODO: Replace this by a constructor argument for the player
        public PossibleAction(Activity activity)
        {
            root = RailsRoot.Instance;
            player = GetRoot.PlayerManager.CurrentPlayer;
            if (player != null)
            {
                playerName = player.Id;
                playerIndex = player.Index;
            }
            this.activity = activity;
        }

        public string PlayerName
        {
            get
            {
                return playerName;
            }
            /**
            * Set the name of the player who <b>executed</b> the action (as opposed to
            * the player who was <b>allowed</b> to do the action, which is the one set
            * in the constructor).
            *
            * @param playerName
            */
            set
            {
                playerName = value;
            }
        }

        public int PlayerIndex
        {
            get
            {
                return playerIndex;
            }
        }

        public Player Player
        {
            get
            {
                return player;
            }
        }

        public bool HasActed
        {
            get
            {
                return acted;
            }
        }

        public void SetActed()
        {
            this.acted = true;
        }

        // joint internal method for both equalAs methods
        virtual protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // compared to null, always false
            if (pa == null) return false;
            // not identical class, always false
            if (!(this.GetType().Equals(pa.GetType()))) return false;

            // check asOption attributes
            bool options = this.player.Equals(pa.player)
                            || pa is NullAction; // TODO: Old save files are sometimes wrong to assign Null Actions 

            // finish if asOptions check
            if (asOption) return options;

            return options
                    && this.acted.Equals(pa.acted);
        }


        /** 
         * Compare the choice options of two action objects, without regard to whatever choice has been made, if any.
         * In other words: only the server-set (prior) attributes must be compared.
         * <p>This method is used by the server (engine) to validate 
         * the incoming action that has actually been chosen in the client (GUI),
         * but only for the purpose to check if the chosen option was really on offer,
         * not to check if the chosen action is actually valid. 
         * These perspectives could give different results in cases where 
         * the PossibleAction does not fully restrict choices to valid values only
         * (such as the blanket LayTile that does no restrict the hex to lay a tile on,
         * or the SetDividend that will accept any revenue value).
         * @param pa Another PossibleAction to compare with.
         * @return True if the compared PossibleAction object has equal choice options.
         */
        public bool EqualsAsOption(PossibleAction pa)
        {
            return EqualsAs(pa, true);
        }

        /** 
         * Compare the chosen actions of two action objects.
         * In other words: the client-set (posterior) attributes must be compared,
         * in addition to those server-set (prior) attributes that sufficiently identify the action.
         * <p>This method is used by the server (engine) to check if two action 
         * objects represent the same actual action, as is done when reloading a saved file
         * (i.e. loading a later stage of the same game).
         * @param pa Another PossibleAction to compare with.
         * @return True if the compared PossibleAction object has equal selected action values.
         */
        public bool EqualsAsAction(PossibleAction pa)
        {
            return EqualsAs(pa, false);
        }

        protected RailsRoot GetRoot
        {
            get
            {
                return root;
            }
        }

        protected GameManager GameManager
        {
            get
            {
                return root.GameManager;
            }
        }

        protected CompanyManager CompanyManager
        {
            get
            {
                return root.CompanyManager;
            }
        }

        public Activity Activity
        {
            get
            {
                return activity;
            }
        }

        /**
         * @return true if it is an action to correct the game state
         */
        virtual public bool IsCorrection
        {
            get
            {
                return false;
            }
        }

        /** Default version of an Menu item text. To be overridden where useful. */
        virtual public string ToMenu()
        {
            return ToString();
        }

        // Implementation for ChangeAction Interface
        public IChangeActionOwner ActionOwner
        {
            get
            {
                return player;
            }
        }

        override public string ToString()
        {
            return RailsObjects.GetStringHelper(this).AddBaseText().ToString();
        }

        // TODO: Rails 2.0 check if the combination above works correctly
        [OnDeserialized]
        internal void OnDeserialized(StreamingContext context)
        {
            if (context.Context != null && (context.Context is GameLoader))
            {
                root = ((GameLoader)context.Context).GetRoot;
            }

            if (playerName != null)
            {
                player = GetRoot.PlayerManager.GetPlayerByName(playerName);
            }
            else
            {
                player = GetRoot.PlayerManager.GetPlayerByIndex(playerIndex);
            }
        }
    }
}
