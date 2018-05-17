using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace GameLib.Net.Common.Parser
{
    public class GameOptionsParser
    {
        private const string FILENAME = "GameOptions.xml";
        private XmlParser parser = new XmlParser();

        public GameOptionsParser() { }

        public GameOptionsSet.Builder ProcessOptions(string directory)
        {
            GameOptionsSet.Builder options = GameOptionsSet.GetBuilder();

            XmlDocument doc = parser.GetDocument(FILENAME, directory);
            XmlElement root = parser.GetTopElement(doc);

            List<XmlElement> elements = parser.GetElementList(XmlTags.OPTION_TAG, root.ChildNodes);

            // use ordering provided in the xml-file
            int ordering = 0;
            foreach (XmlElement element in elements)
            {
                Dictionary<string, string> optionMap = parser.GetAllAttributes(element);

                GameOption.Builder option;
                if (optionMap.ContainsKey(XmlTags.NAME_ATTR))
                {
                    option = GameOption.CreateBuilder(optionMap[XmlTags.NAME_ATTR]);
                }
                else
                {
                    option = null;
                }

                if (option != null)
                {
                    option.SetOrdering(ordering++);

                    if (optionMap.ContainsKey(XmlTags.TYPE_ATTR))
                    {
                        option.SetType(optionMap[XmlTags.TYPE_ATTR]);
                    }

                    if (optionMap.ContainsKey(XmlTags.DEFAULT_ATTR))
                    {
                        option.SetDefaultValue(optionMap[XmlTags.DEFAULT_ATTR]);
                    }

                    if (optionMap.ContainsKey(XmlTags.PARM_ATTR))
                    {
                        string parameters = optionMap[XmlTags.PARM_ATTR];
                        option.SetParameters(parameters.Split(XmlTags.VALUES_DELIM));
                    }

                    if (optionMap.ContainsKey(XmlTags.VALUES_ATTR))
                    {
                        string values = optionMap[XmlTags.VALUES_ATTR];
                        option.SetAllowedValues(values.Split(XmlTags.VALUES_DELIM));
                    }
                    options.Add(option.Build());
                }
            }
            return options;
        }

        public static GameOptionsSet.Builder Load(string gameName)
        {
            GameOptionsParser gop = new GameOptionsParser();
            // use the Separator provided by Resource-Loader!
            string directory = GameInfoParser.DIRECTORY + ResourceLoader.SEPARATOR + gameName;
            return gop.ProcessOptions(directory);
        }
    }
}
