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
   public static final String id = "root";

   // Remark: There is no individual id or parent defined for root

   private final StateManager manager;
   
   // storage of items 
   private HashMapState<String, Item> items;
   
   /**
    * @param the stateManager used for this hierarchy
    */
   public Root(StateManager manager) {
       this.manager = manager;
   }
   
   // Item interface
   @Override
   public void init(Item parent, String id) {
       checkArgument(parent == null, "Parent must be null");
       checkArgument(id != Root.id, "Id must equal " + Root.id);

       HashMapState.create();
   }
   
   public String getId() {
       return Root.id;
   }

   public Item getParent() {
       return null;
   }
   
   public Context getContext() {
       return null;
   }
   
   public String getURI() {
       return Root.id;
   }

   public String getFullURI() {
       return Root.id;
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
   
   public StateManager getStateManager() {
       return manager;
   }
   
   @Override
   public String toString() {
       return Root.id;
   }
    
}
