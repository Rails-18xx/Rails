package rails.game.state;

//An implementation only for testing
class ModelImpl extends Model {

    private final String text;
    
    ModelImpl(Item parent, String id, String text) {
        super(parent, id);
        this.text = text;
    }
    
    @Override
    public String toString() {
        return text;
    }
}
