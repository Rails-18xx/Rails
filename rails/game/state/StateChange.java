package rails.game.state;

/**
 * @author Erik Vos, freystef
 */
final class StateChange<E> implements Change {

    final private GenericState<E> state;
    final private E previous, next;

    public StateChange(final GenericState<E> state, E object) {
        this.state = state;
        previous = state.get();
        next = object;
        ChangeStack.add(this);
    }

    public void execute() {
        state.change(next);
    }

    public void undo() {
        state.change(previous);
    }

    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return "Change State " + state.getId() + " from " + previous.toString() + " to" + next.toString();
    }

}
