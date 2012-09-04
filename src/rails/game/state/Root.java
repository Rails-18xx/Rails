package rails.game.state;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import com.google.common.collect.Lists;
/**
 * Root is the top node of the context/item hierachy
 */
public class Root extends Context{
    
   public final static String ID = ""; 
   private final static String TEXT_ID = "root";

   private StateManager stateManager;
   private HashMapState<String, Item> items;

   // only used during creation
   private boolean delayItems = true;
   private final List<Item> delayedItems = Lists.newArrayList();
    
   protected Root() {
       addItem(this);
   }

   /**
    * @return a Root object with everything initialized (including sub-components like StateManager)
    */
   public static Root create() {
       // precise sequence to avoid any uninitialized problems
       Root root = new Root();
       root.init();
       return root;
   }
   
   protected void init() {
       StateManager stateManager = StateManager.create(this, "states");
       this.stateManager = stateManager;
       stateManager.init();
       initDelayedItems();
   }

   private void initDelayedItems() {
       items = HashMapState.create(this, "items");
       for (Item item: delayedItems) {
           items.put(item.getFullURI(), item);
       }
       delayItems = false;
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
   
   public String toText() {
       return TEXT_ID;
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
       // check if it has to be delayed
       if (delayItems) {
           delayedItems.add(item);
           return;
       }
       
       // check if it already exists
       checkArgument(!items.containsKey(item.getFullURI()), 
               "Root already contains item with identical fullURI = " + item.getFullURI());
       
       // all preconditions ok => add
       items.put(item.getFullURI(), item);
   }

   void removeItem(Item item) {
       // check if it already exists
       checkArgument(items.containsKey(item.getFullURI()), 
               "Root does not contain item with that fullURI = " + item.getFullURI());
       
       // all preconditions ok => remove
       items.remove(item.getFullURI());
   }
   
   @Override
   public String toString() {
       return TEXT_ID;
   }

}
