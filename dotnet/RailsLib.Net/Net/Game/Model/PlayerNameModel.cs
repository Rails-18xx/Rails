using System;
using System.Collections.Generic;
using System.Text;

/**
 * PlayerNameModel contains both the name of the player
 * and information if the player has priority, thus it has to be observable
 *

 */
namespace GameLib.Net.Game.Model
{
    public class PlayerNameModel : State.Model
    {
        public const string ID = "PresidentModel";

        private PlayerNameModel(Player player, string id) : base(player, id)
        {
            // add dependency on the priority
            player.Parent.PriorityPlayerState.AddModel(this);
        }

        public static PlayerNameModel Create(Player player)
        {
            return new PlayerNameModel(player, ID);
        }

        /**
         * @return restricted to Player
         */
        new public Player Parent
        {
            get
            {
                return (Player)base.Parent;
            }
        }

        override public string ToText()
        {
            return Parent.GetNameAndPriority();
        }
    }
}
