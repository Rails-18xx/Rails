using GameLib.Net.Game;
using GameLib.Net.Game.Special;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class LayBaseToken : LayToken
    {
        /* LayTile types */
        public const int GENERIC = 0; // Stop-gap only
        public const int LOCATION_SPECIFIC = 1; // Valid hex
        public const int SPECIAL_PROPERTY = 2; // Directed by a special
        public const int HOME_CITY = 3; // If city on home hex is undefined in 1st turn
                                        // property
        public const int CORRECTION = 99; // Correction token lays

        protected int type = 0;

        /*--- Preconditions ---*/

        /*--- Postconditions ---*/

        /** The station (or city) on the hex where the token is laid */
        protected int chosenStation = 0; // Default

        new public const long serialVersionUID = 1L;

        public LayBaseToken(int type) : base()
        {
            this.type = type;
        }

        /**
         * Lay a base token on one of a given list of locations.
         * <p>This constructor is only intended to be used for normal lays of non-home tokens
         * in the operating company LAY_TOKEN OR step.
         * 
         * @param locations A list of valid locations (hexes) where the acting company can lay a base token.<br>
         * <i>Note:</i> Currently, the game engine cannot yet provide such a list, as all knowledge about routes
         * is contained in the user interface code. As a consequence, this constructor is only called
         * with the value <b>null</b>, which allows laying a base token on <i>any</i> empty city slot.
         * In fact, the UI will now apply the restriction to valid locations only.
         * Over time, applying this restriction should be moved to the game engine.
         */
        public LayBaseToken(List<MapHex> locations) : base(locations)
        {
            type = LOCATION_SPECIFIC;
        }

        /** Lay a base token as allowed via a Special Property.
         * <p>The valid locations (hexes) of such a token should be defined inside the special property.
         * Typically, such locations do not need to be connected to the existing network of a company.
         * 
         * @param specialProperty The special property that allows laying an extra or unconnected base token.
         */
        public LayBaseToken(SpecialBaseTokenLay specialProperty) : base(specialProperty)
        {
            type = SPECIAL_PROPERTY;
        }

        /** Lay a base token on a given location.
         * <p> This constructor is specifically intended to allow the player to select a city for its <b>home</b> token
         * on a multi-city hex or tile (e.g. an OO tile, such as the Erie in 1830 or the THB in 1856).
         * 
         * @param hex The hex on which a city must be selected to lay a home token on.
         */
        public LayBaseToken(MapHex hex) : base(hex)
        {
            ChosenHex = hex;
            type = HOME_CITY;
        }


        [Obsolete]
        public int ChosenStation
        {
            get
            {
                return chosenStation;
            }
            set
            {
                chosenStation = value;
            }
        }

        public Stop ChosenStop
        {
            get
            {
                return chosenHex.GetRelatedStop(chosenStation);
            }
        }

        public int LayBaseTokenType
        {
            get
            {
                return type;
            }
        }


        override public /*SpecialBaseTokenLay*/SpecialProperty GetSpecialProperty()
        {
            return /*(SpecialBaseTokenLay)*/specialProperty;
        }

        override public int GetPotentialCost(MapHex hex)
        {
            if (hex == null)
            {
                return 0;
            }
            else
            {
                if (specialProperty != null && ((SpecialBaseTokenLay)specialProperty).IsFree)
                {
                    return 0;
                }
                else
                {
                    return company.GetBaseTokenLayCost(hex);
                }
            }

        }

        override public int Cost
        {
            get
            {
                return GetPotentialCost(chosenHex);
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            LayBaseToken action = (LayBaseToken)pa;
            bool options = type == action.type;

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && chosenStation == action.chosenStation;
        }

        override public bool IsCorrection
        {
            get
            {
                return (type == LayBaseToken.CORRECTION);
            }
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("type", type)
                        .AddToStringOnlyActed("chosenStation", chosenStation)
                        .ToString();
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

            if (specialPropertyId > 0)
            {
                specialProperty =
                    (SpecialBaseTokenLay)Net.Game.Special.SpecialProperty.GetByUniqueId(GetRoot, specialPropertyId);
            }
            if (chosenHexName != null && chosenHexName.Length > 0)
            {
                chosenHex = mmgr.GetHex(chosenHexName);
            }
        }
    }
}
