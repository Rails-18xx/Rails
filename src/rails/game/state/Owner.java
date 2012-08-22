package rails.game.state;


/**
 * A marker for all classes that are the owners of Ownables
 * This can be either direct (being a PortfolioHolder themselves)
 * or indirect via the PortfolioModel (then specialized as Owner)
 *
 */
public interface Owner extends Item {
    
    
}
