package net.sf.rails.game.state;

public final class GenericStateChange<E> extends Change {

    final private GenericState<E> state;
    final private E previous, next;

    public GenericStateChange(GenericState<E> state, E object) {
        this.state = state;
        previous = state.value();
        next = object;
        super.init(state);
    }

    @Override void execute() {
        state.change(next);
    }

    @Override void undo() {
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
