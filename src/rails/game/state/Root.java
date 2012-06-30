package rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * Root is the top node of the context/item hierachy
 */
public final class Root extends Manager {
    
   public final static String ID = "/"; 
   
   private StateManager stateManager;
    
   private Root() {
        super(null, ID);
   }

   /**
    * @param stateManagerId for the embedded StateManager
    * @return a Root object with initialized StateManager embedded
    */
   public static Root create(String stateManagerId) {
       Root root = new Root();
       StateManager stateManager = StateManager.create(root, stateManagerId);
       root.addStateManager(stateManager);
       return root;
   }
   
   private void addStateManager(StateManager stateManager) {
       this.stateManager = stateManager;
   }
   
   public StateManager getStateManager() {
       return stateManager;
   }
   
   /**
    * @throws UnsupportedOperationsException
    * Not supported for Root
    */
   @Override
   public Item getParent() {
       throw new UnsupportedOperationException();
   }
   
   /**
    * @return this
    */
   @Override
   public Context getContext() {
       return this;
   }
   
   /**
    * @return this
    */
   @Override
   public Root getRoot() {
       return this;
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
   
   @Override
   public void addItem(Item item) {
       // check if it already exists
       checkArgument(items.containsKey(item.getFullURI()), "Root already contains item with identical fullURI");
       
       // all preconditions ok => add
       items.put(item.getFullURI(), item);
   }

   @Override
   public void removeItem(Item item) {
       // check if it already exists
       checkArgument(!items.containsKey(item.getFullURI()), "Root does not contain item with that fullURI");
       
       // all preconditions ok => remove
       items.remove(item.getFullURI());
   }

   @Override
   public String toString() {
       return ID;
   }
    
}
