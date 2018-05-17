using GameLib.Net.Algorithms;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * An object of class Bonus represent extra income for the owning company,
 * usually connected to certain map locations.
 * <p>Currently, Bonus objects will be created in the following cases:
 * <br>1. when a SpecialBaseTokenLay containing a BonusToken
 * is exercised,
 * <br>2. when a private having a LocatedBonus special property is bought by
 * a public company,
 * <br>3. when a sellable bonus is bought from such a public company by another company.
 * @author VosE
 *
 */

namespace GameLib.Net.Game
{
    public class Bonus : ICloseable, IRevenueStaticModifier, IEquatable<Bonus>
    {
        private PublicCompany owner;
        private List<MapHex> locations = null;
        private string name;
        private int value;
        // TODO: What was the intention of those?
        /*    private string removingObjectDesc = null;
            private Object removingObject = null;
        */

        public Bonus(PublicCompany owner,
                string name, int value, List<MapHex> locations)
        {
            this.owner = owner;
            this.name = name;
            this.value = value;
            this.locations = locations;

            // add them to the call list of the RevenueManager
            RailsRoot.Instance.RevenueManager.AddStaticModifier(this);

        }
        public bool IsExecutionable
        {
            get
            {
                return false;
            }
        }

        public PublicCompany Owner
        {
            get
            {
                return owner;
            }
        }

        public List<MapHex> Locations
        {
            get
            {
                return locations;
            }
        }

        public string GetIdForView()
        {

            if (locations == null || locations.Count == 0)
            {
                return name.Substring(0, 2);
            }

            StringBuilder b = new StringBuilder();
            foreach (MapHex location in locations)
            {
                if (b.Length > 0) b.Append(",");
                b.Append(location.Id);
            }
            return b.ToString();
        }

        public string Name
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

        /**
         * Remove the bonus
         * This method can be called by a certain phase when it starts.
         * See prepareForRemovel().
         */
        public void Close()
        {
            RailsRoot.Instance.RevenueManager.RemoveStaticModifier(this);
        }


        public bool Equals(Bonus b)
        {
            if (b == null) return false;
            return (b.name.Equals(name))
                   && b.value == value;
        }

        override public int GetHashCode()
        {
            return name.GetHashCode() ^ value.GetHashCode();
        }

        override public bool Equals(object other)
        {
            if (!(other is Bonus)) return false;
            return Equals((Bonus)other);
        }

        override public string ToString()
        {
            return "Bonus " + name + " hex="
                   + GetIdForView() + " value=" + value;
        }

        public string GetClosingInfo()
        {
            return ToString();
        }

        /**
         * Add bonus value to revenue calculator
         */
        public bool ModifyCalculator(RevenueAdapter revenueAdapter)
        {
            // 1. check operating company
            if (owner != revenueAdapter.Company) return false;

            // 2. find vertices to hex
            bool found = false;
            HashSet<NetworkVertex> bonusVertices = NetworkVertex.GetVerticesByHexes(revenueAdapter.Vertices, locations);
            foreach (NetworkVertex bonusVertex in bonusVertices)
            {
                if (!bonusVertex.IsStation) continue;
                RevenueBonus bonus = new RevenueBonus(value, name);
                bonus.AddVertex(bonusVertex);
                revenueAdapter.AddRevenueBonus(bonus);
                found = true;
            }
            return found;
        }

        public string PrettyPrint(RevenueAdapter revenueAdapter)
        {
            if (name == null) return null;
            return "Bonus active = " + name;
        }
    }
}
