package net.sf.rails.game.state;

public interface MoneyOwner extends Owner {
    
    public Purse getPurse();
    
    public int getCash();
}
