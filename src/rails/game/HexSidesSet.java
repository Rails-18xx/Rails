package rails.game;

import java.util.BitSet;
import java.util.Iterator;

/**
 * HexSides define a BitSet(6) that define booleans on those sides
 */
public class HexSidesSet implements Iterable<HexSide> {
    private final BitSet sides;
    
    private HexSidesSet(BitSet sides) {
        this.sides = sides;
    }
    
    public static HexSidesSet create() {
        return new HexSidesSet(new BitSet(6));
    }
    
    public static HexSidesSet create(BitSet sides) {
        return new HexSidesSet(sides);
    }
    
    public static HexSidesSet rotated(HexSidesSet base, HexSide rotation) {
        if (rotation == HexSide.defaultRotation()) return base;
        
        HexSidesSet.Builder sidesBuilder = HexSidesSet.builder();
        for (HexSide side:HexSide.all()) {
            if (base.get(side)) {
                sidesBuilder.setRotated(side, rotation.negative());
            }
        }
        return sidesBuilder.build();
    }
    
    public BitSet getSides() {
        return sides;
    }
    
    public boolean get(HexSide side) {
        return sides.get(side.getTrackPointNumber());
    }
    
    public HexSide getNext(HexSide current) {
        for (HexSide potentialNext:HexSide.allRotated(current)) {
            if (get(potentialNext)) return potentialNext;
        }
        return null;
    }
    
    public boolean isEmpty() {
        return sides.isEmpty();
    }
    
    public HexSidesSet intersection(HexSidesSet other) {
        BitSet intersection = (BitSet)sides.clone();
        intersection.and(other.getSides());
        return HexSidesSet.create(intersection);
    }
    
    public boolean intersects(HexSidesSet other) {
        return sides.intersects(other.getSides());
    }
    
    @Override
    public String toString() {
        return sides.toString();
    }

    public Iterator<HexSide> iterator() {
        return new Iterator<HexSide>() {
            private int i = 0;
            public boolean hasNext() {
                return (sides.nextSetBit(i) != -1);
            }
            public HexSide next() {
                int s = sides.nextSetBit(i);
                i = s + 1;
                return HexSide.get(s);
            }

            public void remove() { }
        };
    }
    
    public static HexSidesSet.Builder builder() {
        return new HexSidesSet.Builder();
    }

    public static class Builder {
        private final BitSet sides;
        
        private Builder() {
            sides = new BitSet(6);
        }
        
        public void set(HexSide side) {
            sides.set(side.getTrackPointNumber());
        }
        
        public void setRotated(HexSide side, HexSide rotation) {
            this.set(side.getTrackPointNumber() + rotation.getTrackPointNumber());
        }
        
        private void set(int side) {
            sides.set((side + 6) % 6);
        }
        
        public HexSidesSet build() {
            return new HexSidesSet(sides);
        }
    }

}
    


