using GameLib.Net.Util;
using System;

namespace GameLib.Net.Game.State
{
    public class Manager : Context
    {
        // item fields
        private readonly string id;
    private readonly IItem parent;
    // context fields
    private readonly Root root;
    private readonly string fullURI;

    protected Manager(IItem parent, string id)
        {
            Precondition.CheckNotNull(id, "Id cannot be null");
            this.id = id;

            // check arguments, parent can only be null for Root
            Precondition.CheckNotNull(parent, "Parent cannot be null");
            this.parent = parent;

            // URI defined recursively
            fullURI = parent.FullURI + IItemConsts.SEP + id;

            // find root and add context there
            root = parent.GetContext.GetRoot;
            // add to root
            root.AddItem(this);
        }

        override public string Id
        {
            get
            {
                return id;
            }
        }

        override public IItem Parent
        {
            get
            {
                return parent;
            }
        }

        override public Context GetContext
        {
            get
            {
                if (parent is Context)
                {
                    return (Context)parent;
                }
                else
                {
                    // recursive definition
                    return parent.GetContext;
                }
            }
        }

        override public string URI
        {
            get
            {
                if (parent is Context)
                {
                    return id;
                }
                else
                {
                    // recursive definition
                    return parent.URI + IItemConsts.SEP + id;
                }
            }
        }

        override public string FullURI
        {
            get
            {
                return fullURI;
            }
        }

        override public string ToText()
        {
            return id;
        }

        // Context methods
        override public IItem Locate(string uri)
        {
            // first try as fullURI
            IItem item = root.LocateFullURI(uri);
            if (item != null) return item;
            // otherwise as local
            return root.LocateFullURI(fullURI + IItemConsts.SEP + uri);
        }

        override public void AddItem(IItem item)
        {
            // check if this context is the containing one
            Precondition.CheckArgument(item.GetContext == this, "Context is not the container of the item to add");

            // add item to root
            root.AddItem(item);
        }

        override public void RemoveItem(IItem item)
        {
            // check if this context is the containing one
            Precondition.CheckArgument(item.GetContext == this, "Context is not the container of the item to add");

            // remove item from root
            root.RemoveItem(item);
        }

        override public Root GetRoot
        {
            get
            {
                return root;
            }
        }
    }
}
