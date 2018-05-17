
using System.IO;

namespace GameLib.Net.Common
{
    public class XmlLoader : IXmlLoader
    {
        public XmlLoader()
        {

        }

        public string LoadXmlFile(string fileName, string dir)
        {
            string path = Path.Combine(dir, fileName);
            return File.ReadAllText(path);
        }
    }

    public class GameInterface : IGameInterface
    {
        static GameInterface instance = new GameInterface();

        private GameInterface()
        {
            XmlLoader = new XmlLoader();
        }
        static public IGameInterface Instance { get => instance; }

        public IXmlLoader XmlLoader { get; set; }
        public IGameFileInterface GameFileInterface { get; set; }
    }
}
