using System;
using System.Collections.Generic;
using System.Text;

/**
 * HexSide represents the sides of a Hex
 */

namespace GameLib.Net.Game
{
    public class HexSide : TrackPoint
    {
        private static List<HexSide> sides = new List<HexSide>();

        static HexSide()
        {
            for (int s = 0; s < 6; s++)
            {
                sides.Add(new HexSide(s));
            }
        }

        public static HexSide Get(int orientation)
        {
            return sides[(orientation + 6) % 6];
        }

        public static List<HexSide> All()
        {
            return sides;
        }

        public static List<HexSide> AllRotated(HexSide rotation)
        {
            List<HexSide> sideBuilder = new List<HexSide>();
            foreach (HexSide side in sides)
            {
                sideBuilder.Add(HexSide.Get(rotation.number + side.number));
            }
            return sideBuilder;
        }

        public static List<HexSide> AllExceptDefault()
        {
            return sides.GetRange(1, 5);
        }

        public static List<HexSide> Head()
        {
            return sides.GetRange(0, 3);
        }

        public static HexSide DefaultRotation
        {
            get
            {
                return sides[0];
            }
        }

        private int number;

        private HexSide(int number)
        {
            this.number = number;
        }

        public HexSide Opposite
        {
            get
            {
                return Get(this.number + 3);
            }
        }

        public HexSide Negative
        {
            get
            {
                return Get(-this.number);
            }
        }

        public HexSide Next
        {
            get
            {
                return Get(this.number + 1);
            }
        }

        override public TrackPoint Rotate(HexSide rotation)
        {
            return Get(this.number + rotation.number);
        }

        override public int TrackPointNumber
        {
            get
            {
                return number;
            }
        }

        override public TrackPointTypeEnum TrackPointType
        {
            get
            {
                return TrackPoint.TrackPointTypeEnum.SIDE;
            }
        }

        override public String ToString()
        {
            //return Objects.toStringHelper(this).add("number", number).toString();
            return $"{this.GetType().Name}{{number={number}}}";
        }
    }
}
