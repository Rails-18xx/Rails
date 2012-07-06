package rails.game.state;

// An implementation only for testing
class StateImpl extends State {

    private final String text;
    
    StateImpl(Item parent, String id, String text) {
        super(parent, id);
        this.text = text;
    }
    
    @Override
    public String toString() {
        return text;
    }
    
}
