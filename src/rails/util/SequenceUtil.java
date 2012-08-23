package rails.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rails.game.Bank;
import rails.game.Company;
import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.model.CashOwner;


public class SequenceUtil {

    // private constructor
    private SequenceUtil() {};
    
    private static <E extends CashOwner> List<E> 
        selectCashOwners(Class<E> clazz, Collection<CashOwner> coll)  {
        
        // select all cashholders of that type
        List<E> list = new ArrayList<E>();
        
        for (CashOwner c:coll) { 
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
    public static List<CashOwner> sortCashHolders(Collection<CashOwner> coll) {
        
       List<CashOwner> sortedList = new ArrayList<CashOwner>();

       // first add players
       List<Player> players = selectCashOwners(Player.class, coll);
       Collections.sort(players);
       sortedList.addAll(players);
       
       // then public companies
       List<PublicCompany> PublicCompanys = selectCashOwners(PublicCompany.class, coll);
       Collections.sort(PublicCompanys, Company.COMPANY_COMPARATOR);
       sortedList.addAll(PublicCompanys);
       
       // last add the bank
       sortedList.addAll(selectCashOwners(Bank.class, coll));
       
       return sortedList;
    }
}
