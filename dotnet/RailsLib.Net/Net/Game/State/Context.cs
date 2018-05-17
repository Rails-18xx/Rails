using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
/**
 * A context describe a service that allows to locate items
 * 
 * TODO: Check if we should check for non-null id here
 */

namespace GameLib.Net.Game.State
{
    abstract public class Context : IItem
    {
        public abstract string Id { get; }
        public abstract IItem Parent { get; }
        public abstract Context GetContext { get; }
        public abstract Root GetRoot { get; }
        public abstract string URI { get; }
        public abstract string FullURI { get; }

        public abstract IItem Locate(string uri);

        public abstract void AddItem(IItem item);

        public abstract void RemoveItem(IItem item);

        override public string ToString()
        {
            //return Objects.toStringHelper(this).add("id", getId()).toString();
            return $"{this.GetType().Name}{{id={Id}}}";
        }

        public abstract string ToText();
    }
}
