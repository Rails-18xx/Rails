using GameLib.Net.Common.Parser;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    public class PhaseManager : RailsManager, IConfigurable
    {
        // static data
        private List<Phase> phaseList = new List<Phase>();
        private Dictionary<string, Phase> phaseMap = new Dictionary<string, Phase>();

        // dynamic data
        private GenericState<Phase> currentPhase;

        /**
         * Used by Configure (via reflection) only
         */
        public PhaseManager(RailsRoot parent, string id) : base(parent, id)
        {
            currentPhase = GenericState<Phase>.Create(this, "currentPhase");
        }

        public void ConfigureFromXML(Tag tag)
        {
            /*
             * Phase class name is now fixed but can be made configurable, if
             * needed.
             */
            List<Tag> phaseTags = tag.GetChildren("Phase");
            Phase phase;
            Phase previousPhase = null;
            string name;

            int n = 0;
            foreach (Tag phaseTag in phaseTags)
            {
                name = phaseTag.GetAttributeAsString("name", $"{n + 1}");
                phase = new Phase(this, name, n++, previousPhase);
                phaseList.Add(phase);
                phaseMap[name] = phase;
                phase.ConfigureFromXML(phaseTag);
                previousPhase = phase;
            }
        }

        public void FinishConfiguration(RailsRoot root)
        {
            foreach (Phase phase in phaseList)
            {
                phase.FinishConfiguration(root);
            }

            Phase initialPhase = phaseList[0];
            SetPhase(initialPhase, null);
        }

        public Phase GetCurrentPhase()
        {
            return currentPhase.Value;
        }

        public GameState CurrentPhaseModel
        {
            get
            {
                return currentPhase;
            }
        }

        public int CurrentPhaseIndex
        {
            get
            {
                return GetCurrentPhase().Index;
            }
        }

        public void SetPhase(string name, IOwner lastTrainBuyer)
        {
            SetPhase(phaseMap[name], lastTrainBuyer);
        }

        public void SetPhase(Phase phase, IOwner lastTrainBuyer)
        {
            if (phase != null)
            {
                phase.SetLastTrainBuyer(lastTrainBuyer);
                currentPhase.Set(phase);
                phase.Activate();
            }
        }

        public Phase GetPhaseByName(string name)
        {
            return phaseMap[name];
        }

        public bool HasReachedPhase(string phaseName)
        {
            return GetCurrentPhase().Index >= GetPhaseByName(phaseName).Index;

        }

        public List<Phase> Phases
        {
            get
            {
                return phaseList;
            }
        }
    }
}
