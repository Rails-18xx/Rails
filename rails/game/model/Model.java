package rails.game.model;

import rails.game.state.Notifiable;

/**
 * An interface defining all classes that convert model
 * information into something used for the UI clients  
 * 
 * @author freystef
 *
 */
public interface Model<E> extends Notifiable {
    
    public void addView(View<E> view);

    public void removeView(View<E> view);
    
    public E getData();

}
