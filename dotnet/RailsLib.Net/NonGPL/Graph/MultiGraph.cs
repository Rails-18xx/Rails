// This source file is licensed under the MIT license
// Author: jim.wuerch@outlook.com
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Graph
{
    public class Multigraph<TVertex, TEdge> : GraphBase<TVertex, TEdge>
        where TVertex : IEquatable<TVertex>
    {
        public Multigraph() : base(true, false, false)
        {

        }
    }
}
