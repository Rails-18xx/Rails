using GameLib.Net.Game;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Common.Parser
{
    public class ComponentManager
    {
        private static Logger<ComponentManager> log = new Logger<ComponentManager>();

        private Dictionary<string, IConfigurable> mComponentMap = new Dictionary<string, IConfigurable>();

        public ComponentManager() { }

        public void Start(RailsRoot root, Tag tag)
        {
            List<Tag> componentTags = tag.GetChildren(XmlTags.COMPONENT_ELEMENT_ID);
            foreach (Tag componentTag in componentTags)
            {
                string compName = componentTag.GetAttributeAsString("name");
                log.Debug("Found component " + compName);
                IConfigurable component = ConfigureComponent(root, componentTag);
                // feedback to RailsRoot
                root.SetComponent(component);
            }
        }

        private IConfigurable ConfigureComponent(RailsRoot root, Tag componentTag)
        {
            // Extract the attributes of the Component
            string name = componentTag.GetAttributeAsString(XmlTags.NAME_ATTR);
            if (name == null)
            {
                throw new ConfigurationException(
                        LocalText.GetText("UnnamedComponent"));
            }
            string clazz = componentTag.GetAttributeAsString(XmlTags.CLASS_ATTR);
            if (clazz == null)
            {
                throw new ConfigurationException(LocalText.GetText(
                        "ComponentHasNoClass", name));
            }
            string file = componentTag.GetAttributeAsString(XmlTags.FILE_ATTR);

            // Only one component per name.
            if (mComponentMap.ContainsKey(name))
            {
                throw new ConfigurationException(LocalText.GetText(
                        "ComponentConfiguredTwice", name));
            }

            // Now construct the component: Parent is a RailsRoot
            IConfigurable component = (IConfigurable)Configure.Create(clazz, root, name);

            // Configure the component, from a file, or the embedded XML.
            Tag configElement = componentTag;
            if (file != null)
            {
                string directory = GameInfoParser.DIRECTORY + ResourceLoader.SEPARATOR + root.GameName;
                // #FIXME file access
                var fileData = GameInterface.Instance.XmlLoader.LoadXmlFile(file, directory);
                configElement = Tag.FindTopTagInFile(fileData, name, root.GameOptions);
            }

            try
            {
                component.ConfigureFromXML(configElement);
            }
            catch (ConfigurationException e)
            {
                // Do not do this!
                throw e;
            }

            // Add it to the map of known components.
            mComponentMap[name] = component;
            log.Debug(LocalText.GetText("ComponentInitAs", name, clazz));

            return component;
        }

    }
}
