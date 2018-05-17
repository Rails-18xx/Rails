using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameLib.Net.Common.Parser
{
    public sealed class XmlTags
    {
        /* TAGS */
        public const string GAME_TAG = "Game";
        public const string DESCR_TAG = "Description";
        public const string CREDITS_TAG = "Credits";
        public const string PLAYERS_TAG = "Players";
        public const string OPTION_TAG = "GameOption";
        public const string GAMES_LIST_TAG = "GamesList";
        public const string NOTE_TAG = "Note";

        /* ATTRIBUTES */
        public const string NAME_ATTR = "name";
        public const string TEXT_ATTR = "text";
        public const string MIN_ATTR = "minimum";
        public const string MAX_ATTR = "maximum";
        public const string PARM_ATTR = "parm";
        public const string TYPE_ATTR = "type";
        public const string DEFAULT_ATTR = "default";
        public const string VALUES_ATTR = "values";
        public const string CLASS_ATTR = "class";
        public const string FILE_ATTR = "file";

        public const char VALUES_DELIM = ',';

        /* Used by ComponentManager. */
        public const string COMPONENT_MANAGER_ELEMENT_ID = "ComponentManager";
        public const string COMPONENT_ELEMENT_ID = "Component";
    }
}
