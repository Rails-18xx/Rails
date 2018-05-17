using GameLib.Net.Common;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * FIXME: Use other mechanism (TokenManager) to store token ids
 * FIXME: UniqueId and id are a double structure
 */

namespace GameLib.Net.Game
{
    public abstract class Token<T> : RailsOwnableItem<T>, IUpgrade where T : Token<T>
    {
        protected string description = "";
        protected string uniqueId;

        // TODO: storing id in string is for legacy reasons
        protected const string STORAGE_NAME = "Token";

        protected static Logger<Token<T>> log = new Logger<Token<T>>();
    
    protected Token(IRailsItem parent, string id) : base(parent, id)
        {
            uniqueId = id;
            parent.GetRoot.GameManager.StoreObject(STORAGE_NAME, this);
        }

        // these two are implemented in the parent already
        //new public IRailsItem Parent
        //{
        //    get
        //    {
        //        return (IRailsItem)base.Parent;
        //    }
        //}

        //override public RailsRoot GetRoot
        //{
        //    get
        //    {
        //        return (RailsRoot)base.GetRoot;
        //    }
        //}

        public string UniqueId
        {
            get
            {
                return uniqueId;
            }
        }

        // TODO: Rails 2.0 Move it to Token manager 

        /** 
         * @return Token unique_id 
         */
        protected static string CreateUniqueId(IRailsItem item)
        {
            return STORAGE_NAME + "_" + item.GetRoot.GameManager.GetStorageId(STORAGE_NAME);
        }

        public static T GetByUniqueId(IRailsItem item, string id)
        {
            int i = int.Parse(id.Replace(STORAGE_NAME + "_", ""));
            return (T)(item.GetRoot.GameManager.RetrieveObject(STORAGE_NAME, i));
        }
    }
}
