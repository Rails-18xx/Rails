package rails.game.model;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import rails.game.Bank;
import rails.game.state.Item;

/**
 * This class allows calculated values to be used as model objects by using
 * reflection. The value to be calculated is obtained by calling a method
 * specified by name. This method must return an Integer and may not have any
 * parameters.
 */
public class CalculatedMoneyModel extends Model {

    protected static Logger log =
        Logger.getLogger(CalculatedMoneyModel.class.getPackage().getName());

    private String methodName;

    public boolean suppressZero = false;

    /**
     * The id can be defined independent of the methodName in init()
     * However it makes sense to set it identical
     */
    public CalculatedMoneyModel(String id) {
        super(id);
    }

    /**
     * Creates a fully specified and initialized CalculatedMoneyModel
     */
    public static CalculatedMoneyModel create(Item parent, String id, String methodName) {
        return new CalculatedMoneyModel(id).init(parent).initMethod(methodName);
    }
    
    public CalculatedMoneyModel init(Item parent) {
        super.init(parent);
        return this;
    }

    /**
    * @param methodName defines a method defined inside the parent
    */
    public CalculatedMoneyModel initMethod(String methodName) {
        this.methodName = methodName;
        return this;
    }
    
    public void setSuppressZero(boolean suppressZero) 
    {
        this.suppressZero = suppressZero;
    }
    
    protected int calculate() {

        Class<?> objectClass = getParent().getClass();
        Integer amount;
        try {
            Method method = objectClass.getMethod(methodName, (Class[]) null);
            amount = (Integer) method.invoke(getParent(), (Object[]) null);
        } catch (Exception e) {
            log.error("ERROR while invoking method " + methodName
                      + " on class " + objectClass.getName(), e);
            return -1;
        }
        return amount.intValue();
    }

    @Override
    protected String getText() {
        int amount = calculate();
        if (amount == 0 && suppressZero)
            return "";
        else
            return Bank.format(amount);
    }

}
