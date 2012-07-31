package rails.game.state;

// An implementation only for testing
class StateImpl extends State {

    private final String text;
    
    private StateImpl(Item parent, String id, String text) {
        super(parent, id);
        this.text = text;
    }
    
    static StateImpl create(Item parent, String id, String text) {
        return new StateImpl(parent, id, text);
    }
    
    @Override
    public String observerText() {
        return text;
    }
    
    @Override
    public String toString() {
        return text;
    }
}
