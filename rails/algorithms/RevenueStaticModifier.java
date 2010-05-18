package rails.algorithms;


/**
 * Classes that change properties of the revenue calculation
 * before the actual calculation starts implement a the static modifier.
 *  
 * They have to register themselves to the RevenueManager via the GameManager instance.
 * @author freystef
 *
 */


public interface RevenueStaticModifier{
    
    public void modifyCalculator(RevenueAdapter revenueAdapter);
    
}
