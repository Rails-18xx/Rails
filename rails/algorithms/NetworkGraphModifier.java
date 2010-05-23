package rails.algorithms;

/**
 * Classes that change properties of the network graph
 * before both route evaluation and revenenue calculation starts
 * implement this interface.
 *  
 * They have to register themselves to the RevenueManager via the GameManager instance.
 * @author freystef
 *
 */

public interface NetworkGraphModifier {

    public void modifyGraph(NetworkGraphBuilder graphBuilder);
    
}
