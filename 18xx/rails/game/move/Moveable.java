package rails.game.move;

public interface Moveable {

    public void moveTo(MoveableHolderI newHolder);

    public String getName();

    public MoveableHolderI getHolder();

}
