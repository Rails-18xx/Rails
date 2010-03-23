package rails.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rails.game.Bank;
import rails.game.CashHolder;
import rails.game.Player;
import rails.game.PublicCompany;


public class SequenceUtil {

    // private constructor
    private SequenceUtil() {};
    
    private static <E extends CashHolder> List<E> 
        selectCashHolders(Class<E> clazz, Collection<CashHolder> coll)  {
        
        // select all cashholders of that type
        List<E> list = new ArrayList<E>();
        
        for (CashHolder c:coll) { 
            if (clazz.isAssignableFrom(c.getClass())) {
                @SuppressWarnings("unchecked")
                E cast = (E) c;
                list.add(cast);
            }
        }
        
        return list;
    }
    
    
    /**
     * Defines a sorting on cashHolders
     * @return sorted list of cashholders
     */
    public static List<CashHolder> sortCashHolders(Collection<CashHolder> coll) {
        
       List<CashHolder> sortedList = new ArrayList<CashHolder>();

       // first add players
       List<Player> players = selectCashHolders(Player.class, coll);
       Collections.sort(players);
       sortedList.addAll(players);
       
       // then public companies
       List<PublicCompany> PublicCompanys = selectCashHolders(PublicCompany.class, coll);
       Collections.sort(PublicCompanys);
       sortedList.addAll(PublicCompanys);
       
       // last add the bank
       sortedList.addAll(selectCashHolders(Bank.class, coll));
       
       return sortedList;
    }
}
