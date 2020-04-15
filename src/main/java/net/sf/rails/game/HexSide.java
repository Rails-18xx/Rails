package net.sf.rails.game;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * HexSide represents the sides of a Hex
 */
public class HexSide extends TrackPoint {

    private static final ImmutableList<HexSide> sides;

    static {
        ImmutableList.Builder<HexSide> sideBuilder = ImmutableList.builder();
        for (int s = 0; s < 6; s++) {
            sideBuilder.add(new HexSide(s));
        }
        sides = sideBuilder.build();
    }

    public static HexSide get(int orientation) {
        return sides.get((orientation + 6) % 6);
    }

    public static List<HexSide> all() {
        return sides;
    }

    public static List<HexSide> allRotated(HexSide rotation) {
        ImmutableList.Builder<HexSide> sideBuilder = ImmutableList.builder();
        for (HexSide side : sides) {
            sideBuilder.add(HexSide.get(rotation.number + side.number));
        }
        return sideBuilder.build();
    }

    public static List<HexSide> allExceptDefault() {
        return sides.subList(1, 6);
    }

    public static List<HexSide> head() {
        return sides.subList(0, 3);
    }

    public static HexSide defaultRotation() {
        return sides.get(0);
    }

    private int number;

    private HexSide(int number) {
        this.number = number;
    }

    public HexSide opposite() {
        return get(this.number + 3);
    }

    public HexSide negative() {
        return get(-this.number);
    }

    public HexSide next() {
        return get(this.number + 1);
    }

    @Override
    public HexSide rotate(HexSide rotation) {
        return get(this.number + rotation.number);
    }

    @Override
    public int getTrackPointNumber() {
        return number;
    }

    @Override
    public Type getTrackPointType() {
        return TrackPoint.Type.SIDE;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("number", number)
                .toString();
    }

}
