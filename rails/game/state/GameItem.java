package rails.game.state;

/**
 * A GameItem is a default implementation of Item
 * 
 * @author freystef
 */
public abstract class GameItem implements Item {
        
        private final String id;

        // parent can be initialized later
        private Item parent;
        
        @Deprecated
        // TODO: Remove that default constructor here
        public GameItem() {
            this.id = null;
        }

        /**
         * Creates an Item
         * @param id identifier for the item (cannot be null)
         */
        public GameItem(String id){
            if (id == null) {
                throw new IllegalArgumentException("Missing id for a GameItem in hierarchy");
            }
            this.id = id;
        }

        /**
         * Initializing of GameItem
         * @param parent has to be of type GameItem otherwise an Exception is raised
         */
        public GameItem init(Item parent){
            if (this.parent != null) {
                throw new IllegalStateException("GameItem already intialized");
            }
            this.parent = parent;
            return this;
        }
        
        public String getId() {
            return id;
        }

        public Item getParent() {
            if (parent == null) {
                throw new IllegalStateException("GameItem not yet intialized");
            }
            return parent;
        }
        
        public Context getContext() {
            if (parent == null) {
                throw new IllegalStateException("GameItem not yet intialized");
            }
            return parent.getContext();
        }

        public String getURI() {
            if (parent != null && parent.getURI() != null ) {
                if (parent instanceof Context) {
                    return id;
                } else {
                    return parent.getURI() + Context.SEP + id;
                }
            } else {
                return id;
            }
        }
}
