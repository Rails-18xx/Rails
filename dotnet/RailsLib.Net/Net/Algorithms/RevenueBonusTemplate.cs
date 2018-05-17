using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game;
using System;
using System.Collections.Generic;
using System.Text;

/** 
 * defines a template for a revenue bonus at creation time of rails objects
 * will be converted to a true RevenueBonus object during each revenue calculation
 * @author freystef

 */
namespace GameLib.Net.Algorithms
{
    public sealed class RevenueBonusTemplate : IConfigurable
    {
        private static Logger<RevenueBonusTemplate> log = new Logger<RevenueBonusTemplate>();

        // bonus value
        private int value;

        // bonus name
        private string name;

        // template condition attributes
        private List<int> identVertices;
        private List<string> identTrainTypes;
        private List<string> identPhases;

        public RevenueBonusTemplate()
        {
            identVertices = new List<int>();
            identTrainTypes = new List<string>();
            identPhases = new List<string>();
        }

        public void ConfigureFromXML(Tag tag)
        {
            value = tag.GetAttributeAsInteger("value");
            name = tag.GetAttributeAsString("name");

            // check for vertices
            List<Tag> vertexTags = tag.GetChildren("Vertex");
            if (vertexTags != null)
            {
                foreach (Tag vertexTag in vertexTags)
                {
                    int id = vertexTag.GetAttributeAsInteger("id");
                    //if (id != null)
                    {
                        // #FIXME check if id can be 0
                        identVertices.Add(id);
                    }
                }
            }

            // check for train (types)
            List<Tag> trainTags = tag.GetChildren("Train");
            if (trainTags != null)
            {
                foreach (Tag trainTag in trainTags)
                {
                    string type = trainTag.GetAttributeAsString("type");
                    if (type != null)
                    {
                        identTrainTypes.Add(type);
                    }
                }
            }

            // check for phases 
            List<Tag> phaseTags = tag.GetChildren("phase");
            if (phaseTags != null)
            {
                foreach (Tag phaseTag in phaseTags)
                {
                    string type = phaseTag.GetAttributeAsString("name");
                    if (type != null)
                    {
                        identPhases.Add(type);
                    }
                }
            }
            log.Info("Configured " + this);
        }

        /**
         * is not used, use toRevenueBonus instead
         */
        public void FinishConfiguration(RailsRoot parent)

        {
            throw new ConfigurationException("Use toRevenueBonus");
        }

        public RevenueBonus ToRevenueBonus(MapHex hex, RailsRoot root, NetworkGraph graph)
        {
            log.Info("Convert " + this);
            RevenueBonus bonus = new RevenueBonus(value, name);
            if (!ConvertVertices(bonus, graph, hex))
            {
                log.Warn("Not all vertices of RevenueBonusTemplate found " + this.ToString());
                return null;
            }
            ConvertTrainTypes(bonus, root.TrainManager);
            ConvertPhases(bonus, root.PhaseManager);
            log.Info("Converted to " + bonus);
            return bonus;
        }

        private bool ConvertVertices(RevenueBonus bonus, NetworkGraph graph, MapHex hex)
        {
            foreach (int identVertex in identVertices)
            {
                NetworkVertex vertex = graph.GetVertex(hex, identVertex);
                if (vertex == null)
                {
                    return false;
                }
                else
                {
                    bonus.AddVertex(vertex);
                }
            }
            return true;
        }

        private void ConvertTrainTypes(RevenueBonus bonus, TrainManager tm)
        {
            foreach (string identTrainType in identTrainTypes)
            {
                TrainType trainType = tm.GetTypeByName(identTrainType);
                if (trainType != null)
                {
                    bonus.AddTrainType(trainType);
                }
            }
        }

        private void ConvertPhases(RevenueBonus bonus, PhaseManager pm)
        {
            foreach (string identPhase in identPhases)
            {
                Phase phase = pm.GetPhaseByName(identPhase);
                if (phase != null)
                {
                    bonus.AddPhase(phase);
                }
            }
        }

        /**
         *  @return bonus name for display
         */
        public string Name
        {
            get
            {
                return name;
            }
        }

        /**
         * @return bonus toolTip text
         */
        public string GetToolTip()
        {
            StringBuilder s = new StringBuilder();
            s.Append(value);
            if (identPhases.Count != 0)
            {
                s.Append(identPhases);
                if (identTrainTypes.Count != 0)
                {
                    s.Append("");
                }
            }
            if (identTrainTypes.Count != 0)
            {
                s.Append(identTrainTypes);
            }
            return s.ToString();
        }

        override public string ToString()
        {
            StringBuilder s = new StringBuilder();
            s.Append("RevenueBonusTemplate");
            if (name == null)
                s.Append(" unnamed");
            else
                s.Append(" name = " + name);
            s.Append(", value " + value);
            s.Append(", identVertices = " + identVertices);
            s.Append(", identTrainTypes = " + identTrainTypes);
            s.Append(", identPhases = " + identPhases);
            return s.ToString();
        }
    }
}
