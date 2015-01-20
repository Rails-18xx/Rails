package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Observable;

public abstract class Accessor1D<T extends Item> {

    private final Class<T> clazz;
    
    protected Accessor1D(Class<T> clazz) {
        this.clazz = clazz;
    }
    
    public Class<T> getItemClass() {
        return clazz;
    }
    
    public static abstract class AText<T extends Item> extends Accessor1D<T> {

        protected AText(Class<T> clazz) {
            super(clazz);
        }
        
        public String get(Item item) {
            T castItem = getItemClass().cast(item);
            return access(castItem);
        }
        
        protected abstract String access(T item); 

    }
    
    public static abstract class AObservable<T extends Item> extends Accessor1D<T> {

        protected AObservable(Class<T> clazz) {
            super(clazz);
        }
        
        public Observable get(Item item) {
            T castItem = getItemClass().cast(item);
            return access(castItem);
        }
        
        protected abstract Observable access(T item);
        
    }
    
    
    public static abstract class AColors<T extends Item> extends Accessor1D<T> {

        protected AColors(Class<T> clazz) {
            super(clazz);
        }
   
        public GridColors get(Item item) {
            T castItem = getItemClass().cast(item);
            return access(castItem);
        }
        
        protected abstract GridColors access(T item); 
 
    }
    
    public static abstract class AColorModel<T extends Item> extends Accessor1D<T> {
        
        protected AColorModel(Class<T> clazz) {
            super(clazz);
        }
   
        public ColorModel get(Item item) {
            T castItem = getItemClass().cast(item);
            return access(castItem);
        }
        
        protected abstract ColorModel access(T item); 
        
        
        
    }
    
}
