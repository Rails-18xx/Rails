using Newtonsoft.Json;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Specialized;

namespace GameLib.Net.Common
{
    [JsonObject(MemberSerialization.Fields)]
    public class GameOptionsSet
    {
        //private LinkedHashMap<string, string> optionsToValues= new LinkedHashMap<string, string>();
        //private OrderedDictionary optionsToValues = new OrderedDictionary();
        private Dictionary<string, string> optionsToValues = new Dictionary<string, string>();

        public GameOptionsSet() { }

        private GameOptionsSet(int nbPlayers)
        {
            optionsToValues[GameOption.NUMBER_OF_PLAYERS] = nbPlayers.ToString();
        }

        private static GameOptionsSet Create(int nbPlayers, List<GameOption> options)
        {
            GameOptionsSet set = new GameOptionsSet(nbPlayers);
            foreach (GameOption option in options)
            {
                set.optionsToValues[option.Name] = option.SelectedValue;
            }
            return set;
        }

        public Dictionary<string, string> GetOptions()
        {
            return optionsToValues;
            //Dictionary<string, string> ret = new Dictionary<string, string>();
            //IDictionaryEnumerator en = optionsToValues.GetEnumerator();
            //while (en.MoveNext())
            //{
            //    ret[(string)en.Key] = (string)en.Value;
            //}

            //return ret;
        }

        public string Get(string option)
        {
            return (string)optionsToValues[option];
        }

        public static Builder GetBuilder()
        {
            return new Builder();
        }

        public class Builder
        {
            private SortedSet<GameOption> options = new SortedSet<GameOption>();

            // #builder_constructor
            public Builder() { }

            public void Add(GameOption option)
            {
                options.Add(option);
            }

            public List<GameOption> GetOptions()
            {
                return new List<GameOption>(options); //ImmutableList.copyOf(options);
            }

            public GameOptionsSet Build(int nbPlayers)
            {
                return GameOptionsSet.Create(nbPlayers, this.GetOptions());
            }
        }

    }
}
