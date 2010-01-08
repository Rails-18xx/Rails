package rails.game.move;

public interface MoveableHolder {

    public boolean addObject(Moveable object);

    public boolean removeObject(Moveable object);

    public String getName();

}
