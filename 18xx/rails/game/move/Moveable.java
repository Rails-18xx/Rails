package rails.game.move;

public interface Moveable {

    public static final int[] AT_END = new int[] {-1};

    public void moveTo(MoveableHolder newHolder);

    public String getName();

    public MoveableHolder getHolder();

}
