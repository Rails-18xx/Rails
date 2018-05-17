using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Text;

/**
 * HexSides define a BitSet(6) that define booleans on those sides
 */

namespace GameLib.Net.Game
{
    public class HexSidesSet : IEnumerable<HexSide>
    {
        private const int SIDES = 6;
        private BitVector32 sides;

        private HexSidesSet(BitVector32 sides)
        {
            this.sides = sides;
        }

        public static HexSidesSet Create()
        {
            return new HexSidesSet(new BitVector32(0));
        }

        public static HexSidesSet Create(BitVector32 sides)
        {
            return new HexSidesSet(sides);
        }

        public static HexSidesSet Rotated(HexSidesSet baseSet, HexSide rotation)
        {
            if (rotation == HexSide.DefaultRotation) return baseSet;

            HexSidesSet.Builder sidesBuilder = GetBuilder();

            //HexSidesSet.Builder sidesBuilder = HexSidesSet.builder();
            foreach (HexSide side in HexSide.All())
            {
                if (baseSet.Get(side))
                {
                    sidesBuilder.SetRotated(side, rotation.Negative);
                }
            }
            return sidesBuilder.Build();
        }

        public BitVector32 Sides
        {
            get
            {
                return sides;
            }
        }

        public bool Get(HexSide side)
        {
            return sides[side.TrackPointNumber];
        }

        public HexSide GetNext(HexSide current)
        {
            foreach (HexSide potentialNext in HexSide.AllRotated(current))
            {
                if (Get(potentialNext)) return potentialNext;
            }
            return null;
        }

        public bool OnlySingle
        {
            get
            {
                bool ret = false;
                for (int i = 0; i < SIDES; ++i)
                {
                    if (sides[i])
                    {
                        if (ret)
                            return false; // already been set, so this is 2nd bit
                        else
                            ret = true;
                    }

                }
                return ret;
            }
        }

        public bool IsEmpty
        {
            get
            {
                return sides.Data == 0; //sides.isEmpty();
            }
        }

        public HexSidesSet Intersection(HexSidesSet other)
        {
            int intersection = other.sides.Data & sides.Data;

            return HexSidesSet.Create(new BitVector32(intersection));
        }

        public bool Intersects(HexSidesSet other)
        {
            return (sides.Data & other.sides.Data) != 0; //sides.intersects(other.getSides());
        }


        override public string ToString()
        {
            return sides.ToString();
        }

        public IEnumerator<HexSide> GetEnumerator()
        {
            return new HexSideEnumerator(this);
            //        return new Iterator<HexSide>()
            //        {
            //        private int i = 0;
            //    public boolean hasNext()
            //    {
            //        return (sides.nextSetBit(i) != -1);
            //    }
            //    public HexSide next()
            //    {
            //        int s = sides.nextSetBit(i);
            //        i = s + 1;
            //        return HexSide.get(s);
            //    }

            //    public void remove() { }
            //};
        }

        public static HexSidesSet.Builder GetBuilder()
        {
            return new HexSidesSet.Builder();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return this.GetEnumerator();
        }

        public class Builder
        {
            private BitVector32 sides;

            public Builder()
            {
                sides = new BitVector32(0);
            }

            public void Set(HexSide side)
            {
                sides[side.TrackPointNumber] = true;
            }

            public void SetRotated(HexSide side, HexSide rotation)
            {
                this.Set(side.TrackPointNumber + rotation.TrackPointNumber);
            }

            private void Set(int side)
            {
                sides[(side + 6) % 6] = true;
            }

            public HexSidesSet Build()
            {
                return new HexSidesSet(sides);
            }
        }

        private class HexSideEnumerator : IEnumerator<HexSide>, IDisposable
        {
            HexSidesSet sidesSet;
            int index = -1;
            public HexSideEnumerator(HexSidesSet set)
            {
                sidesSet = set;
            }

            #region IDisposable Support
            private bool disposedValue = false; // To detect redundant calls

            public HexSide Current { get; private set; }

            object IEnumerator.Current
            {
                get
                {
                    return this.Current;
                }
            }

            protected virtual void Dispose(bool disposing)
            {
                if (!disposedValue)
                {
                    if (disposing)
                    {
                        // TODO: dispose managed state (managed objects).
                    }

                    // TODO: free unmanaged resources (unmanaged objects) and override a finalizer below.
                    // TODO: set large fields to null.

                    disposedValue = true;
                }
            }

            // TODO: override a finalizer only if Dispose(bool disposing) above has code to free unmanaged resources.
            // ~HexSideEnumerator() {
            //   // Do not change this code. Put cleanup code in Dispose(bool disposing) above.
            //   Dispose(false);
            // }

            // This code added to correctly implement the disposable pattern.
            public void Dispose()
            {
                // Do not change this code. Put cleanup code in Dispose(bool disposing) above.
                Dispose(true);
                // TODO: uncomment the following line if the finalizer is overridden above.
                // GC.SuppressFinalize(this);
            }
            #endregion

            private int NextSetBit()
            {
                // get next set bit
                for (int i = index + 1; i < SIDES; ++index)
                {
                    if (sidesSet.sides[i])
                    {
                        return i;
                    }
                }

                return -1;
            }

            public bool MoveNext()
            {
                index = NextSetBit();
                if (index == -1)
                {
                    return false;
                }
                else
                {
                    Current = HexSide.Get(index);
                    return true;
                }
                //..int s = sides.nextSetBit(i);
                //i = s + 1;
                //return HexSide.get(s);
            }

            public void Reset()
            {
                index = -1;
            }
            

        }
    }
}
