using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Coordinates and stores all elements related to revenue calculation,
 * which are permanent.
 * The conversion of Rails elements is in the responsibility of the RevenueAdapter.
 * For each GameManager instance only one RevenueManager is created.
 */

namespace GameLib.Net.Algorithms
{
    public class RevenueManager : RailsManager, IConfigurable
    {
        protected static Logger<RevenueManager> log = new Logger<RevenueManager>();

        // Modifiers that are configurable
        private HashSet<IConfigurable> configurableModifiers = new HashSet<IConfigurable>();

        // Variables to store modifiers (permanent)
        private ListState<INetworkGraphModifier> graphModifiers;
        private ListState<IRevenueStaticModifier> staticModifiers;
        private ListState<IRevenueDynamicModifier> dynamicModifiers;
        private IRevenueCalculatorModifier calculatorModifier;

        // Variables that store the active modifier (per RevenueAdapter)
        private List<IRevenueStaticModifier> activeStaticModifiers = new List<IRevenueStaticModifier>();
        private List<IRevenueDynamicModifier> activeDynamicModifiers = new List<IRevenueDynamicModifier>();
        // TODO: Still add that flag if the calculator is active
        //    private bool activeCalculator;

        /**
         * Used by Configure (via reflection) only
         */
        public RevenueManager(RailsRoot parent, string id) : base(parent, id)
        {
            graphModifiers = ListState<INetworkGraphModifier>.Create(this, "graphModifiers");
            staticModifiers = ListState<IRevenueStaticModifier>.Create(this, "staticModifiers");
            dynamicModifiers = ListState<IRevenueDynamicModifier>.Create(this, "dynamicModifiers");
        }

        public void ConfigureFromXML(Tag tag)
        {
            // define modifiers
            List<Tag> modifierTags = tag.GetChildren("Modifier");

            if (modifierTags != null)
            {
                foreach (Tag modifierTag in modifierTags)
                {
                    // get classname
                    string className = modifierTag.GetAttributeAsString("class");
                    if (className == null)
                    {
                        throw new ConfigurationException(LocalText.GetText("ComponentHasNoClass", "Modifier"));
                    }
                    // create modifier
                    object modifier;
                    try
                    {
                        var type = Type.GetType(className);
                        modifier = Activator.CreateInstance(type);
                        //modifier = Class.forName(className).newInstance();
                    }
                    catch (Exception e)
                    {
                        throw new ConfigurationException(LocalText.GetText(
                                "ClassCannotBeInstantiated", className), e);
                    }
                    bool isModifier = false;
                    // add them to the revenueManager
                    if (modifier is INetworkGraphModifier)
                    {
                        graphModifiers.Add((INetworkGraphModifier)modifier);
                        isModifier = true;
                        log.Info("Added as graph modifier = " + className);
                    }
                    if (modifier is IRevenueStaticModifier)
                    {
                        staticModifiers.Add((IRevenueStaticModifier)modifier);
                        isModifier = true;
                        log.Info("Added as static modifier = " + className);
                    }
                    if (modifier is IRevenueDynamicModifier)
                    {
                        dynamicModifiers.Add((IRevenueDynamicModifier)modifier);
                        isModifier = true;
                        log.Info("Added as dynamic modifier = " + className);
                    }
                    if (modifier is IRevenueCalculatorModifier)
                    {
                        if (calculatorModifier != null)
                        {
                            throw new ConfigurationException(LocalText.GetText(
                                    "MoreThanOneCalculatorModifier", className));
                        }
                        calculatorModifier = (IRevenueCalculatorModifier)modifier;
                        isModifier = true;
                        log.Info("Added as calculator modifier = " + className);
                    }
                    if (!isModifier)
                    {
                        throw new ConfigurationException(LocalText.GetText(
                                "ClassIsNotAModifier", className));
                    }
                    if (isModifier && modifier is IConfigurable)
                    {
                        configurableModifiers.Add((IConfigurable)modifier);
                    }
                }
            }
        }

        public void FinishConfiguration(RailsRoot parent)
        {
            foreach (IConfigurable modifier in configurableModifiers)
            {
                modifier.FinishConfiguration(parent);
            }
        }

        public void AddStaticModifier(IRevenueStaticModifier modifier)
        {
            staticModifiers.Add(modifier);
            log.Info("Revenue Manager: Added static modifier " + modifier);
        }

        public bool RemoveStaticModifier(IRevenueStaticModifier modifier)
        {
            bool result = staticModifiers.Remove(modifier);
            if (result)
            {
                log.Info("RevenueManager: Removed static modifier " + modifier);
            }
            else
            {
                log.Info("RevenueManager: Cannot remove" + modifier);
            }
            return result;
        }

        public void AddGraphModifier(INetworkGraphModifier modifier)
        {
            graphModifiers.Add(modifier);
            log.Info("Revenue Manager: Added graph modifier " + modifier);
        }

        public bool RemoveGraphModifier(INetworkGraphModifier modifier)
        {
            bool result = graphModifiers.Remove(modifier);
            if (result)
            {
                log.Info("RevenueManager: Removed graph modifier " + modifier);
            }
            else
            {
                log.Info("RevenueManager: Cannot remove" + modifier);
            }
            return result;
        }

        public void AddDynamicModifier(IRevenueDynamicModifier modifier)
        {
            dynamicModifiers.Add(modifier);
            log.Info("Revenue Manager: Added dynamic modifier " + modifier);
        }

        public bool RemoveDynamicModifier(IRevenueDynamicModifier modifier)
        {
            bool result = dynamicModifiers.Remove(modifier);
            if (result)
            {
                log.Info("RevenueManager: Removed dynamic modifier " + modifier);
            }
            else
            {
                log.Info("RevenueManager: Cannot remove" + modifier);
            }
            return result;
        }

        public void ActivateMapGraphModifiers(NetworkGraph graph)
        {
            foreach (INetworkGraphModifier modifier in graphModifiers.View())
            {
                modifier.ModifyMapGraph(graph);
            }
        }

        public void ActivateRouteGraphModifiers(NetworkGraph graph, PublicCompany company)
        {
            foreach (INetworkGraphModifier modifier in graphModifiers.View())
            {
                modifier.ModifyRouteGraph(graph, company);
            }
        }


        public void InitStaticModifiers(RevenueAdapter revenueAdapter)
        {
            activeStaticModifiers.Clear();
            foreach (IRevenueStaticModifier modifier in staticModifiers.View())
            {
                if (modifier.ModifyCalculator(revenueAdapter))
                {
                    activeStaticModifiers.Add(modifier);
                }
            }
        }

        /**
         * @param revenueAdapter
         * @return true if there are active dynamic modifiers
         */
        public bool InitDynamicModifiers(RevenueAdapter revenueAdapter)
        {
            activeDynamicModifiers.Clear();
            foreach (IRevenueDynamicModifier modifier in dynamicModifiers.View())
            {
                if (modifier.PrepareModifier(revenueAdapter))
                    activeDynamicModifiers.Add(modifier);
            }
            return activeDynamicModifiers.Count > 0;
        }

        /**
         * @param revenueAdapter
         * @return revenue from active calculator
         */
        // FIXME: This does not fully cover all cases that needs the revenue from the calculator
        int RevenueFromDynamicCalculator(RevenueAdapter revenueAdapter)
        {
            return calculatorModifier.CalculateRevenue(revenueAdapter);

        }

        /**
         * Allows dynamic modifiers to adjust the optimal run 
         * @param optimalRun
         */
        public void AdjustOptimalRun(List<RevenueTrainRun> optimalRun)
        {
            // allow dynamic modifiers to change the optimal run
            foreach (IRevenueDynamicModifier modifier in activeDynamicModifiers)
            {
                modifier.AdjustOptimalRun(optimalRun);
            }
        }

        /**
         * @param run the current run
         * @param optimal flag if this is the found optimal run
         * @return total value of dynamic modifiers
         */
        public int EvaluationValue(List<RevenueTrainRun> run, bool optimal)
        {
            // this allows dynamic modifiers to change the optimal run
            // however this is forbidden outside the optimal run!
            int value = 0;
            foreach (IRevenueDynamicModifier modifier in activeDynamicModifiers)
            {
                value += modifier.EvaluationValue(run, optimal);
            }
            return value;
        }

        /**
         * @return total prediction value of dynamic modifiers
         */
        public int PredictionValue(List<RevenueTrainRun> run)
        {
            // do not change the optimal run!
            int value = 0;
            foreach (IRevenueDynamicModifier modifier in activeDynamicModifiers)
            {
                value += modifier.PredictionValue(run);
            }
            return value;
        }

        /**
         * 
         * @param revenueAdapter
         * @return pretty print output from all modifiers (both static and dynamic)
         */
        public string PrettyPrint(RevenueAdapter revenueAdapter)
        {
            StringBuilder prettyPrint = new StringBuilder();

            foreach (IRevenueStaticModifier modifier in activeStaticModifiers)
            {
                string modifierText = modifier.PrettyPrint(revenueAdapter);
                if (modifierText != null)
                {
                    prettyPrint.Append(modifierText + "\n");
                }
            }

            foreach (IRevenueDynamicModifier modifier in activeDynamicModifiers)
            {
                string modifierText = modifier.PrettyPrint(revenueAdapter);
                if (modifierText != null)
                {
                    prettyPrint.Append(modifierText + "\n");
                }
            }

            return prettyPrint.ToString();
        }

    }
}
