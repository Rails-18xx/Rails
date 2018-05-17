using System;
using System.Collections.Generic;
using System.Text;

/**
 * This interface can be implemented to define a specific type
 * It can be used e.g. to structure a Portfolio
 * @param <T> indicates the class used for typing
 */

namespace GameLib.Net.Game.State
{
    public interface ITypable<T>
    {
        T SpecificType { get; }
    }
}
