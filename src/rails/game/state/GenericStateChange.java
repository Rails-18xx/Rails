package rails.game.state;

final class GenericStateChange<E> extends Change {

    final private GenericState<E> state;
    final private E previous, next;

    public GenericStateChange(GenericState<E> state, E object) {
        super(state);
        this.state = state;
        previous = state.get();
        next = object;
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
        return "Change State " + state.getId() + " from " + previous.toString() + " to" + next.toString();
    }

}
