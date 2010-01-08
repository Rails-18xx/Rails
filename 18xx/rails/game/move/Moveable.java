package rails.game.move;

public interface Moveable {

    public void moveTo(MoveableHolder newHolder);

    public String getName();

    public MoveableHolder getHolder();

}
