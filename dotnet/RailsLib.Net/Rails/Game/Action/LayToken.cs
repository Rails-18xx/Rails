using GameLib.Net.Game;
using GameLib.Net.Game.Special;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    abstract public class LayToken : PossibleORAction
    {
        /*--- Preconditions ---*/

        /** Where to lay a token (null means anywhere) */
        /*transient*/
        [JsonIgnore]
        protected List<MapHex> locations = null;
        protected string locationNames;

        /**
         * Special property that will be fulfilled by this token lay. If null, this
         * is a normal token lay.
         */
        /*transient*/
        [JsonIgnore]
        protected SpecialProperty specialProperty = null;
        protected int specialPropertyId;

        /*--- Postconditions ---*/

        /** The map hex on which the token is laid */
        /*transient*/
        [JsonIgnore]
        protected MapHex chosenHex = null;
        protected string chosenHexName;

        new public const long serialVersionUID = 1L;

        /**
         * Allow laying a base token on a given location.
         */
        public LayToken(List<MapHex> locations)
        {
            this.locations = locations;
            if (locations != null)
            {
                this.locations = locations;
                BuildLocationNameString();
            }
        }

        public LayToken(SpecialBaseTokenLay specialProperty)
        {
            this.locations = specialProperty.Locations;
            if (locations != null) BuildLocationNameString();
            this.specialProperty = specialProperty;
            this.specialPropertyId = specialProperty.UniqueId;
        }

        public LayToken(SpecialBonusTokenLay specialProperty)
        {
            this.locations = specialProperty.Locations;
            if (locations != null) BuildLocationNameString();
            this.specialProperty = specialProperty;
            this.specialPropertyId = specialProperty.UniqueId;
        }

        public LayToken(MapHex hex)
        {
            this.locations = new List<MapHex>(1);
            locations.Add(hex);
            BuildLocationNameString();
        }

        public LayToken()
        {
            this.locations = null;
        }

        /**
         * @return Returns the chosenHex.
         */
        public MapHex ChosenHex
        {
            get
            {
                return chosenHex;
            }
            set
            {
                this.chosenHex = value;
                this.chosenHexName = chosenHex.Id;
            }
        }

        /**
         * @return Returns the specialProperty.
         */
        public abstract SpecialProperty GetSpecialProperty();

        /**
         * @param specialProperty The specialProperty to set.
         */
        public void SetSpecialProperty(SpecialBaseTokenLay specialProperty)
        {
            this.specialProperty = specialProperty;
            // TODO this.specialPropertyUniqueId = specialProperty.getUniqueId();
        }

        public List<MapHex> Locations
        {
            get
            {
                return locations;
            }
        }

        public string LocationNameString
        {
            get
            {
                return locationNames;
            }
        }

        private void BuildLocationNameString()
        {
            StringBuilder b = new StringBuilder();
            foreach (MapHex hex in locations)
            {
                if (b.Length > 0) b.Append(",");
                b.Append(hex.Id);
            }
            locationNames = b.ToString();
        }

        public abstract int GetPotentialCost(MapHex hex);

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            LayToken action = (LayToken)pa;
            bool options = (locations.Equals(action.locations) || this.locations == null && (action.locations.Count == 0))
                    && specialProperty.Equals(action.specialProperty);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && chosenHex.Equals(action.chosenHex);
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("locations", locations)
                        .AddToString("specialProperty", specialProperty)
                        .AddToStringOnlyActed("chosenHex", chosenHex)
                        .ToString();
        }
    }
}
