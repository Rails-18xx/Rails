package rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * Root is the top node of the context/item hierachy
 */
public final class Root extends Context {
    
   public final static String ID = ""; 
   
   private StateManager stateManager;
   private final HashMapState<String, Item> items = HashMapState.create(this, null);
    
   private Root() {
   }

   /**
    * @return a Root object with initialized StateManager embedded
    */
   public static Root create() {
       Root root = new Root();
       StateManager stateManager = StateManager.create(root, "states");
       root.addStateManager(stateManager);
       root.addItem(root);
       return root;
   }
   
   private void addStateManager(StateManager stateManager) {
       this.stateManager = stateManager;
       this.addItem(stateManager);
   }
   
   public StateManager getStateManager() {
       return stateManager;
   }

   // Item methods
   
   /**
    * @throws UnsupportedOperationsException
    * Not supported for Root
    */
   public Item getParent() {
       throw new UnsupportedOperationException();
   }

   public String getId() {
       return "";
   }
   
   /**
    * @return this
    */
   public Context getContext() {
       return this;
   }
   
   /**
    * @return this
    */
   public Root getRoot() {
       return this;
   }
   
   public String getURI() {
       return "";
   }

   public String getFullURI() {
       return "";
   }
   
   // Context methods
   public Item locate(String uri) {
       // first try as fullURI
       Item item = items.get(uri);
       if (item != null) return item;
       // otherwise as local
       return items.get(Item.SEP + uri);
   }

   // used by other context
   Item locateFullURI(String uri) {
       return items.get(uri);
   }
   
   void addItem(Item item) {
       // check if it already exists
       checkArgument(!items.containsKey(item.getFullURI()), "Root already contains item with identical fullURI");
       
       // all preconditions ok => add
       items.put(item.getFullURI(), item);
   }

   void removeItem(Item item) {
       // check if it already exists
       checkArgument(items.containsKey(item.getFullURI()), "Root does not contain item with that fullURI");
       
       // all preconditions ok => remove
       items.remove(item.getFullURI());
   }

   @Override
   public String toString() {
       return ID;
   }

    
}
