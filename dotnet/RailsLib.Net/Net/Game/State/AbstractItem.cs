using GameLib.Net.Util;
using System;

/**
 * An AbstractItem is a default implementation of Item
 */
namespace GameLib.Net.Game.State
{
    abstract public class AbstractItem : IItem
    {
        private string id;
    private IItem parent;
    private Context context;

    protected AbstractItem(IItem parent, string id)
        {
            Precondition.CheckNotNull(parent, "Parent cannot be null");
            Precondition.CheckArgument(id != Root.ID, "Id cannot equal " + Root.ID);

            // defined standard fields
            this.parent = parent;
            this.id = id;

            if (parent is Context)
            {
                context = (Context)parent;
            }
            else
            {
                // recursive definition
                context = parent.GetContext;
            }

            // add item to context
            context.AddItem(this);
        }

        public string Id
        {
            get
            {
                return id;
            }
        }

        public IItem Parent
        {
            get
            {
                return parent;
            }
        }

        public Context GetContext
        {
            get
            {
                return context;
            }
        }

        public Root GetRoot
        {
            get
            {
                // forward it to the context
                return context.GetRoot;
            }
        }

        public string URI
        {
            get
            {
                if (parent is Context)
                {
                    return id;
                } else
                {
                    // recursive definition
                    return parent.URI + IItemConsts.SEP + id;
                }
            }
        }

        public string FullURI
        {
            get
            {
                // recursive definition
                return parent.FullURI + IItemConsts.SEP + id;
            }
        }

        virtual public string ToText()
        {
            return id;
        }

        override public string ToString()
        {
            //return Objects.toStringHelper(this).add("URI", getFullURI()).toString();
            return $"{this.GetType().Name}{{URI={FullURI}}}";
        }
    }
}
