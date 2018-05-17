using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace GameLib.Net.Common.Parser
{
    // #FIXME_file_access
    public class GameInfoParser
    {
        public const string DIRECTORY = "data";
        private const string FILENAME = "GamesList.xml";

        private XmlParser parser = new XmlParser();
        private string credits;

        public GameInfoParser() { }

        public string GetCredits()
        {
            return credits;
        }

        // returns a sorted list
        public SortedSet<GameInfo> ProcessGameList()
        {
            SortedSet<GameInfo> gameList = new SortedSet<GameInfo>();

            XmlDocument doc = parser.GetDocument(FILENAME, DIRECTORY);
            XmlElement root = parser.GetTopElement(doc);

            // <CREDITS>
            List<XmlElement> creditsElement = parser.GetElementList(XmlTags.CREDITS_TAG, root.ChildNodes);
            this.credits = parser.GetElementText(creditsElement[0].ChildNodes);

            List<XmlElement> gameElements = parser.GetElementList(XmlTags.GAME_TAG, root.ChildNodes);

            // <GAME>
            IEnumerator<XmlElement> it = gameElements.GetEnumerator();
            int count = 0;
            while (it.MoveNext())
            {
                XmlElement el = it.Current;

                GameInfo.Builder gameInfo = GameInfo.GetBuilder();

                //			ArrayList<GameOption> optionsList = new ArrayList<GameOption>();

                //TODO: push validation into getAttributeAs* methods
                gameInfo.Name = parser.GetAttributeAsString(XmlTags.NAME_ATTR, el);

                List<XmlElement> childElements = parser.GetElementList(el.ChildNodes);

                // <PLAYER> , <OPTION>, <DESCRIPTION>
                IEnumerator<XmlElement> childIt = childElements.GetEnumerator();
                while (childIt.MoveNext())
                {
                    XmlElement child = childIt.Current;

                    if (child.Name.Equals(XmlTags.DESCR_TAG))
                    {
                        gameInfo.Description = parser.GetElementText(child.ChildNodes);
                    }

                    if (child.Name.Equals(XmlTags.NOTE_TAG))
                    {
                        gameInfo.Note = parser.GetElementText(child.ChildNodes);
                    }

                    if (child.Name.Equals(XmlTags.PLAYERS_TAG))
                    {
                        gameInfo.MinPlayers = parser.GetAttributeAsInteger(XmlTags.MIN_ATTR, child);
                        gameInfo.MaxPlayers = parser.GetAttributeAsInteger(XmlTags.MAX_ATTR, child);
                    }

                }
                gameList.Add(gameInfo.Build(count++));
            }
            return gameList;
        }
    }
}
