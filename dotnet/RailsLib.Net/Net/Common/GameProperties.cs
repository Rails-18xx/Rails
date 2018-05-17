using GameLib.Net.Common;
using System;
using System.Collections.Generic;
using System.IO;

namespace GameLib.Net.Common
{
    public class GameProperties
    {
        Dictionary<string, string> properties;

        public GameProperties()
        {
            properties = new Dictionary<string, string>();
        }

        public Dictionary<string, string> Properties { get => properties; }

        static public GameProperties LoadFromString(string data)
        {
            throw new NotImplementedException();
            // load from JSON file
            //return null; 
        }

        static public GameProperties LoadFromFile(IGameFile file)
        {
            string s = file.LoadFile();

            return LoadFromString(s);
        }

        static public GameProperties LoadFromFile(string filename)
        {
            //throw new NotImplementedException();
            GameProperties ret =  new GameProperties();

            if (!File.Exists(filename))
                return ret;

            using (StreamReader sr = new StreamReader(filename))
            {
                string line;
                // Read and display lines from the file until the end of 
                // the file is reached.
                while ((line = sr.ReadLine()) != null)
                {
                    if (line.Length == 0 || line[0] == '#')
                    {
                        // comment or blank line
                        continue;
                    }

                    var items = line.Split('=');
                    if (items.Length == 2)
                    {
                        ret.properties[items[0]] = items[1];
                    }
                    else if (items.Length == 1)
                    {
                        ret.properties[items[0]] = null;
                    }
                }
            }

            return ret;
        }

        public void StoreToFile(IGameFile file)
        {
            throw new NotImplementedException();
        }

        public string GetProperty(string key)
        {
            if (!properties.ContainsKey(key))
            {
                return null;
            }

            return properties[key];
        }

        public void SetProperty(string key, string value)
        {
            properties[key] = value;
        }
    }
}
