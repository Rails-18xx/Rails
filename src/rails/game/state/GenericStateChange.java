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
        return "Change for " + state + ": Replace " + previous + " by " + next ;
    }

}
