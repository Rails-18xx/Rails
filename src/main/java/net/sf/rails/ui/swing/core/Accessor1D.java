package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Observable;

import java.awt.Color;

public abstract class Accessor1D<T extends Item> {

    private final Class<T> clazz;
    
    protected Accessor1D(Class<T> clazz) {
        this.clazz = clazz;
    }
    
    public Class<T> getClassT() {
        return clazz;
    }
    
    public static abstract class AText<T extends Item> extends Accessor1D<T> {

        protected AText(Class<T> clazz) {
            super(clazz);
        }
        
        public String get(Item item) {
            T castItem = getClassT().cast(item);
            return access(castItem);
        }
        
        protected abstract String access(T item); 

    }
    
    public static abstract class AObservable<T extends Item> extends Accessor1D<T> {

        protected AObservable(Class<T> clazz) {
            super(clazz);
        }
        
        public Observable get(Item item) {
            T castItem = getClassT().cast(item);
            return access(castItem);
        }
        
        protected abstract Observable access(T item);
        
    }
    
    
    public static abstract class AColor<T extends Item> extends Accessor1D<T> {

        protected AColor(Class<T> clazz) {
            super(clazz);
        }
   
        public Color get(Item item) {
            T castItem = getClassT().cast(item);
            return access(castItem);
        }
        
        protected abstract Color access(T item); 
 
    }
    
    public static abstract class AColorModel<T extends Item> extends Accessor1D<T> {
        
        protected AColorModel(Class<T> clazz) {
            super(clazz);
        }
   
        public ColorModel get(Item item) {
            T castItem = getClassT().cast(item);
            return access(castItem);
        }
        
        protected abstract ColorModel access(T item); 
        
        
        
    }
    
}
