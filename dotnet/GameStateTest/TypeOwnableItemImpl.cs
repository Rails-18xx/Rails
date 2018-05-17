using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameStateTest
{
    public class TypeOwnableItemImpl : OwnableItem<TypeOwnableItemImpl>, ITypable<string>, IComparable<TypeOwnableItemImpl>, IComparable
    {
        private string type;

        private TypeOwnableItemImpl(IItem parent, string id, string type) : base(parent, id)
        {
            this.type = type;
        }

        public static TypeOwnableItemImpl Create(IItem parent, string id, string type)
        {
            return new TypeOwnableItemImpl(parent, id, type);
        }

        public int CompareTo(TypeOwnableItemImpl other)
        {
            if (other == null) return 1;
            return type.CompareTo(other.type);
        }

        public int CompareTo(object obj)
        {
            if (obj == null) return 1;
            if (!(obj is TypeOwnableItemImpl))
                throw new InvalidOperationException("Can't compare non-matching types");
            return type.CompareTo(((TypeOwnableItemImpl)obj).type);
        }

        public string SpecificType
        {
            get
            {
                return type;
            }
        }
    }
}
