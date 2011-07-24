package rails.game.state;

public interface Moveable extends Item {

    public void moveTo(Holder newHolder);

    public Holder getHolder();

}
