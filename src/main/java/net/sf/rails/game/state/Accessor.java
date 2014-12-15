package net.sf.rails.game.state;


public interface Accessor<R extends Item> {

    public Item access(R parent);
    
    
}
