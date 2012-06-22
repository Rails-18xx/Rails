package rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Root is a context that serves as the top node

 * It also contains the StateManager if the object tree
 * should be able to contain states
 */
public final class Root extends Context {

   /**
    * The reserved id for a root
    */
   public static final String ID = "root";
  
   private Root(StateManager parent) {
       super(parent, ID);
   }

   /**
    * @param the game used for this hierarchy
    */
   public static Root create(Game parent) {
       return new Root(parent);
   }
   
   @Override
   public StateManager getParent() {
       return null;
   }
   
   @Override
   public Context getContext() {
       return null;
   }
   
   @Override
   public String getURI() {
       return ID;
   }

   @Override
   public String getFullURI() {
       return ID;
   }
   
   // Context methods
   @Override
   public Item localize(String uri) {
       return items.get(uri);
   }
   
   // Root methods
   void addItemToRoot(Item item) {
       // check if it already exists
       checkArgument(items.containsKey(item.getFullURI()), "Root already contains item with identical fullURI");
       
       // all preconditions ok
       items.put(item.getFullURI(), item);
   }

   @Override
   public String toString() {
       return ID;
   }
    
}
