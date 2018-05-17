using GameLib.Net.Algorithms;
using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using GameLib.Net.Graph;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.Special
{
    public class SpecialRight : SpecialProperty, INetworkGraphModifier
    {
        /** The public company of which a share can be obtained. */
        private string rightName;
        private string rightDefaultValue;
        private string rightValue;
        private int cost = 0;
        private string locationNames;
        private List<MapHex> locations;

        /**
         * Used by Configure (via reflection) only
         */
        public SpecialRight(IRailsItem parent, string id) : base(parent, id)
        {
        }

        override public void ConfigureFromXML(Tag tag)
        {

            base.ConfigureFromXML(tag);

            Tag rightTag = tag.GetChild("SpecialRight");
            if (rightTag == null)
            {
                throw new ConfigurationException("<SpecialRight> tag missing");
            }

            rightName = rightTag.GetAttributeAsString("name");
            if (string.IsNullOrEmpty(rightName))
            {
                throw new ConfigurationException("SpecialRight: no Right name specified");
            }

            rightDefaultValue = rightValue = rightTag.GetAttributeAsString("defaultValue", null);

            cost = rightTag.GetAttributeAsInteger("cost", 0);

            locationNames = rightTag.GetAttributeAsString("location", null);
        }

        override public void FinishConfiguration(RailsRoot root)
        {
            base.FinishConfiguration(root);

            // add them to the call list of the RevenueManager
            root.RevenueManager.AddGraphModifier(this);

            if (locationNames != null)
            {
                locations = new List<MapHex>();
                MapManager mmgr = root.MapManager;
                MapHex hex;
                foreach (string hexName in locationNames.Split(','))
                {
                    hex = mmgr.GetHex(hexName);
                    if (hex == null)
                    {
                        throw new ConfigurationException("Unknown hex '" + hexName + "' for Special Right");
                    }
                    locations.Add(hex);
                }
            }
        }

        override public bool IsExecutionable
        {
            // FIXME: Check if this works correctly
            // IT is better to rewrite this check
            // see ExchangeForShare
            get
            {
                return ((PrivateCompany)originalCompany).Owner is Player;
            }
        }

        public string Name
        {
            get
            {
                return rightName;
            }
        }

        public string DefaultValue
        {
            get
            {
                return rightDefaultValue;
            }
        }

        public string Value
        {
            get
            {
                return rightValue;
            }
            set
            {
                rightValue = value;
            }
        }

        public int Cost
        {
            get
            {
                return cost;
            }
        }

        public string LocationNames
        {
            get
            {
                return locationNames;
            }
        }

        public List<MapHex> Locations
        {
            get
            {
                return locations;
            }
        }

        override public string ToText()
        {
            StringBuilder b = new StringBuilder();
            b.Append(cost > 0 ? "Buy '" : "Get '").Append(rightName).Append("'");
            if (locationNames != null) b.Append(" at ").Append(locationNames);
            if (cost > 0) b.Append(" for ").Append(Bank.Format(this, cost));
            return b.ToString();
        }

        override public string ToMenu()
        {
            return LocalText.GetText("BuyRight",
                    rightName,
                    Bank.Format(this, cost));
        }

        override public string GetInfo()
        {
            return ToMenu();
        }


        public void ModifyMapGraph(NetworkGraph mapGraph)
        {
            // Do nothing
        }

        public void ModifyRouteGraph(NetworkGraph routeGraph, PublicCompany company)
        {
            // 1. check operating company if it has the right then it is excluded from the removal
            // TODO: Only use one right for all companies instead of one per company
            if (this.OriginalCompany != company || company.HasRight(this)) return;

            SimpleGraph<NetworkVertex, NetworkEdge> graph = routeGraph.Graph;

            // 2. find vertices to hex and remove the station
            ICollection<NetworkVertex> verticesToRemove = NetworkVertex.GetVerticesByHexes(graph.Vertices, locations);
            // 3 ... and remove them from the graph
            graph.RemoveVertices(verticesToRemove);
        }
    }
}
