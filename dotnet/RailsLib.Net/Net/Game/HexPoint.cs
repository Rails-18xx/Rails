using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

/**
* Static class that describes x-y coordinates for GUIHexes
*/

namespace GameLib.Net.Game
{
    public class HexPoint
    {
        private PointF point;

        public HexPoint(float x, float y)
        {
            this.point = new PointF(x, y);
        }

        public HexPoint(PointF point)
        {
            this.point = point;
        }

        public PointF Get2D()
        {
            return point;
        }

        public float X
        {
            get
            {
                return point.X;
            }
        }

        public float Y
        {
            get
            {
                return point.Y;
            }
        }

        override public string ToString()
        {
            return $"{X},{Y}";
        }

        public HexPoint Rotate(float radians)
        {
            if (radians == 0) return this;
            double x = X * Math.Cos(radians) + Y * Math.Sin(radians);
            double y = Y * Math.Cos(radians) - X * Math.Sin(radians);
            return new HexPoint((float)x, (float)y);
        }

        public HexPoint Translate(float x, float y)
        {
            if (x == 0 && y == 0) return this;
            return new HexPoint(X + x, Y + y);
        }

        public static HexPoint Middle(HexPoint a, HexPoint b)
        {
            return new HexPoint((a.X + b.X) / 2.0f, (a.Y + b.Y) / 2.0f);
        }

        public static HexPoint Add(HexPoint a, HexPoint b)
        {
            return new HexPoint(a.X + b.X, a.Y + b.Y);
        }

        public static HexPoint Difference(HexPoint a, HexPoint b)
        {
            return new HexPoint(a.X - b.X, a.Y - b.Y);
        }
    }
}
