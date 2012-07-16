package rails.game.state;

final class GenericStateChange<E> extends Change {

    final private GenericState<E> state;
    final private E previous, next;

    public GenericStateChange(GenericState<E> state, E object) {
        this.state = state;
        previous = state.value();
        next = object;
        super.init(state);
    }

    @Override
    public void execute() {
        state.change(next);
    }

    @Override
    public void undo() {
        state.change(previous);
    }

    @Override
    public GenericState<E> getState() {
        return state;
    }

    @Override
    public String toString() {
        String from, to;
        if (previous == null) {
            from = "<null>";
        } else {
            from = previous.toString();
        }
        if (next == null) {
            to = "<null>";
        } else {
            to = next.toString();
        }
        
        return "Change State " + state + " from " + from + " to " + to ;
    }

}
