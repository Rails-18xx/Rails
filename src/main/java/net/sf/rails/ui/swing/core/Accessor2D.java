package net.sf.rails.ui.swing.core;

import java.awt.Color;

import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Observable;

public abstract class Accessor2D<T extends Item, S extends Item> {
    
    private final Class<T> clazzT;
    private final Class<S> clazzS;
    
    protected Accessor2D(Class<T> clazzT, Class<S> clazzS) {
        this.clazzT = clazzT;
        this.clazzS = clazzS;
    }
    
    public Class<T> getClassT() {
        return clazzT;
    }
    
    public Class<S> getClassS() {
        return clazzS;
    }
    
    public static abstract class AText<T extends Item, S extends Item> extends Accessor2D<T,S> {
        
        protected AText(Class<T> clazzT, Class<S> clazzS) {
            super(clazzT, clazzS);
        }
        
        public String get(Item itemT, Item itemS) {
            T castItemT = getClassT().cast(itemT);
            S castItemS = getClassS().cast(itemS);
            return access(castItemT, castItemS);
        }
        
        protected abstract String access(T itemT, S itemS);
        
    }

    public static abstract class AObservable<T extends Item, S extends Item> extends Accessor2D<T,S> {
        
        protected AObservable(Class<T> clazzT, Class<S> clazzS) {
            super(clazzT, clazzS);
        }
        
        public Observable get(Item itemT, Item itemS) {
            T castItemT = getClassT().cast(itemT);
            S castItemS = getClassS().cast(itemS);
            return access(castItemT, castItemS);
        }
        
        protected abstract Observable access(T itemT, S itemS);
        
    }
    
   public static abstract class AColor<T extends Item, S extends Item> extends Accessor2D<T,S> {
        
        protected AColor(Class<T> clazzT, Class<S> clazzS) {
            super(clazzT, clazzS);
        }
        
        public Color get(Item itemT, Item itemS) {
            T castItemT = getClassT().cast(itemT);
            S castItemS = getClassS().cast(itemS);
            return access(castItemT, castItemS);
        }
        
        protected abstract Color access(T itemT, S itemS);
        
    }
    
    
}
