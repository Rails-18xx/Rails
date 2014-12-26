package net.sf.rails.game.state;

public interface Accessor1D<R, A extends Item> extends Accessor<R> {

    public R access(A item);
       
}
