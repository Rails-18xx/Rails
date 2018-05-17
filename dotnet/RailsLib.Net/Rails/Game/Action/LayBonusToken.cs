using GameLib.Net.Game;
using GameLib.Net.Game.Special;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class LayBonusToken : LayToken
    {
        /*transient*/
        [JsonIgnore]
        BonusToken token = null;
        string tokenId = null;

        /*--- Preconditions ---*/

        /*--- Postconditions ---*/

        new public const long serialVersionUID = 1L;

        public LayBonusToken(SpecialBonusTokenLay specialProperty, BonusToken token) : base(specialProperty)
        {
            this.token = token;
            this.tokenId = token.UniqueId;
        }

        public void FinishConfiguration(RailsRoot root)
        {
            token.PrepareForRemoval(root.PhaseManager);
        }

        public BonusToken Token
        {
            get
            {
                return token;
            }
        }

        override public /*SpecialBonusTokenLay*/SpecialProperty GetSpecialProperty()
        {
            return /*(SpecialBonusTokenLay)*/specialProperty;
        }

        override public int GetPotentialCost(MapHex hex)
        {
            return 0;
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            LayBonusToken action = (LayBonusToken)pa;
            return token.Equals(action.token);
            // no asAction attributes to be checked
        }

        override public string ToString()
        {
            StringBuilder b = new StringBuilder("LayBonusToken ");
            if (chosenHex == null)
            {
                b.Append(" location=").Append(locationNames).Append(" spec.prop=").Append(specialProperty);
            }
            else
            {
                b.Append("hex=").Append(chosenHex.Id);
            }
            return b.ToString();
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            MapManager mmgr = GetRoot.MapManager;
            locations = new List<MapHex>();
            if (!string.IsNullOrEmpty(locationNames))
            {
                foreach (string hexName in locationNames.Split(','))
                {
                    locations.Add(mmgr.GetHex(hexName));
                }
            }

            if (tokenId != null)
            {
                token = Token<BonusToken>.GetByUniqueId(GetRoot, tokenId);
            }
            if (specialPropertyId > 0)
            {
                specialProperty =
                        (SpecialBonusTokenLay)SpecialProperty.GetByUniqueId(GetRoot, specialPropertyId);
            }
            if (chosenHexName != null && chosenHexName.Length > 0)
            {
                chosenHex = mmgr.GetHex(chosenHexName);
            }
        }
    }
}
