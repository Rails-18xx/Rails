// This source file is licensed under the MIT license
// Author: jim.wuerch@outlook.com

using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Graph
{
    public class SimpleGraph<TVertex, TEdge> : GraphBase<TVertex, TEdge>
        where TVertex : IEquatable<TVertex>
    {
        public SimpleGraph() : base(false, false, false)
        {

        }
    }
}
