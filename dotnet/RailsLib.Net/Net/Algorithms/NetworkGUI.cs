using GameLib.Net.Game;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

namespace GameLib.Net.Algorithms
{
    public class HexMap
    {
        public GUIHex GetHex(MapHex hex)
        {
            throw new NotImplementedException();
        }
    }

    public class GUIHex
    {
        public PointF GetStopPoint2D(Stop stop)
        {
            throw new NotImplementedException();
        }

        public PointF GetSidePoint2D(HexSide side)
        {
            throw new NotImplementedException();
        }
    }

    abstract public class Shape
    {
        public Shape()
        {

        }
    }

    public class Line2D : Shape
    {

        public Line2D(PointF src, PointF dst) : base()
        {
            Src = src;
            Dst = dst;
        }

        public PointF Src { get; set; }
        public PointF Dst { get; set; }
    }
}
