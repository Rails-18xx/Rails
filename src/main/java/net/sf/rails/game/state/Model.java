package net.sf.rails.game.state;

/**
 * Model is an abstract generic class
 * that defines the a middle layer between State(s) and
 * the Observer(s) in the user interface.
 * 
 * Models themselves can be layered upon each other.
 * 
 * It replaces the ModelObject class in Rails 1.0 
 */

public abstract class Model extends Observable {

    protected Model(Item parent, String id) {
        super(parent, id);
    }

}
