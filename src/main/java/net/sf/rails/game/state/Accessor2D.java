package net.sf.rails.game.state;

public interface Accessor2D<R, A extends Item, B extends Item> extends Accessor<R> {
    
    public R access(A itemR, B itemS);
  
}
