using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * A BonusToken object represents a token that a operating public company can
 * place on the map to gain extra revenue or other privileges.
 * <p>Such tokens are usually not placed in city slots, 
 * which are intended for base tokens, but on some unoccupied part of a tile.  
 */

namespace GameLib.Net.Game
{
    public class BonusToken : Token<BonusToken>, ICloseable, IConfigurable
    {
        private int value;
        private string name;
        private string removingObjectDesc = null;
        private Object removingObject = null;
        private PublicCompany user = null;

        private BonusToken(IRailsOwner parent, string id) : base(parent, id)
        {
        }

        public static BonusToken Create(IRailsOwner parent)
        {
            string uniqueId = CreateUniqueId(parent);
            BonusToken token = new BonusToken(parent, uniqueId);
            return token;
        }

        public void ConfigureFromXML(Tag tag)
        {
            Tag bonusTokenTag = tag.GetChild("BonusToken");
            if (bonusTokenTag == null)
            {
                throw new ConfigurationException("<BonusToken> tag missing");
            }
            value = bonusTokenTag.GetAttributeAsInteger("value");
            if (value <= 0)
            {
                throw new ConfigurationException("Missing or invalid value " + value);
            }

            name = bonusTokenTag.GetAttributeAsString("name");
            if (string.IsNullOrEmpty(name))
            {
                throw new ConfigurationException("Bonus token must have a name");
            }
            description = name + " +" + Bank.Format(this, value) + " bonus token";

            removingObjectDesc = bonusTokenTag.GetAttributeAsString("removed");
        }

        public void FinishConfiguration(RailsRoot root)
        {
            PrepareForRemoval(root.PhaseManager);
        }

        /**
         * Remove the token.
         * This method can be called by a certain phase when it starts.
         * See prepareForRemovel().
         */
        public void Close()
        {
            this.MoveTo(GetRoot.Bank.ScrapHeap);
            if (user != null)
            {
                user.RemoveBonus(name);
            }
        }

        /**
         * Prepare the bonus token for removal, if so configured.
         * The only case currently implemented to trigger removal
         * is the start of a given phase.
         */
        public void PrepareForRemoval(PhaseManager phaseManager)
        {

            if (removingObjectDesc == null) return;

            if (removingObject == null)
            {
                string[] spec = removingObjectDesc.Split(':');
                if (spec[0].Equals("Phase", StringComparison.OrdinalIgnoreCase))
                {
                    removingObject = phaseManager.GetPhaseByName(spec[1]);
                }
            }

            if (removingObject is Phase)
            {
                ((Phase)removingObject).AddObjectToClose(this);
            }
        }

        public void SetUser(PublicCompany user)
        {
            this.user = user;
        }

        public bool IsPlaced
        {
            get
            {
                return (Owner is MapHex);
            }
        }
        // #FIXME this hides AbstractItem.Id  should it?
        new public string Id
        {
            get
            {
                return name;
            }
        }

        public int Value
        {
            get
            {
                return value;
            }
        }

        public string GetClosingInfo()
        {
            return description;
        }

        override public string ToString()
        {
            return description;
        }
    }
}
