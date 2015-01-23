package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Observable;

public abstract class Accessor2D<T extends Item, S extends Item> {
    
    private final Class<T> clazzT;
    private final Class<S> clazzS;
    
    protected Accessor2D(Class<T> clazzT, Class<S> clazzS) {
        this.clazzT = clazzT;
        this.clazzS = clazzS;
    }
    
    public Class<T> getItemTClass() {
        return clazzT;
    }
    
    public Class<S> getItemSClass() {
        return clazzS;
    }
    
    public static abstract class AText<T extends Item, S extends Item> extends Accessor2D<T,S> {
        
        protected AText(Class<T> clazzT, Class<S> clazzS) {
            super(clazzT, clazzS);
        }
        
        public String get(Item itemT, Item itemS) {
            T castItemT = getItemTClass().cast(itemT);
            S castItemS = getItemSClass().cast(itemS);
            return access(castItemT, castItemS);
        }
        
        protected abstract String access(T itemT, S itemS);
        
    }

    public static abstract class AObservable<T extends Item, S extends Item> extends Accessor2D<T,S> {
        
        protected AObservable(Class<T> clazzT, Class<S> clazzS) {
            super(clazzT, clazzS);
        }
        
        public Observable get(Item itemT, Item itemS) {
            T castItemT = getItemTClass().cast(itemT);
            S castItemS = getItemSClass().cast(itemS);
            return access(castItemT, castItemS);
        }
        
        protected abstract Observable access(T itemT, S itemS);
        
    }
    
    public static abstract class AColors<T extends Item, S extends Item> extends Accessor2D<T,S> {
        
        protected AColors(Class<T> clazzT, Class<S> clazzS) {
            super(clazzT, clazzS);
        }
        
        public GridColors get(Item itemT, Item itemS) {
            T castItemT = getItemTClass().cast(itemT);
            S castItemS = getItemSClass().cast(itemS);
            return access(castItemT, castItemS);
        }
        
        protected abstract GridColors access(T itemT, S itemS);
        
    }

    public static abstract class AColorModel<T extends Item, S extends Item> extends Accessor2D<T,S> {
        
        protected AColorModel(Class<T> clazzT, Class<S> clazzS) {
            super(clazzT, clazzS);
        }
        
        public ColorModel get(Item itemT, Item itemS) {
            T castItemT = getItemTClass().cast(itemT);
            S castItemS = getItemSClass().cast(itemS);
            return access(castItemT, castItemS);
        }
        
        protected abstract ColorModel access(T itemT, S itemS);
        
    }


}
