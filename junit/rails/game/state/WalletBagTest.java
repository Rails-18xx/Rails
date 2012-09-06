package rails.game.state;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class WalletBagTest {

    private final static String WALLET_A_ID = "WalletA";
    private final static String WALLET_B_ID = "WalletB";

    private final static String OWNER_A_ID = "OwnerA";
    private final static String OWNER_B_ID = "OwnerB";
    private final static String ITEM_ID = "Item";
    private final static int AMOUNT = 10;
  
    private Root root;
    private WalletBag<Countable> walletA;
    private WalletBag<Countable> walletB;
    private Owner ownerA;
    private Owner ownerB;
    private Countable item;

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        ownerA = OwnerImpl.create(root, OWNER_A_ID);
        ownerB = OwnerImpl.create(root, OWNER_B_ID);
        item = CountableItemImpl.create(root, ITEM_ID);
        walletA = WalletBag.create(ownerA, WALLET_A_ID , Countable.class, item);
        walletB = WalletBag.create(ownerB, WALLET_B_ID , Countable.class, item);
        StateTestUtils.closeAndNew(root);
    }

    @Test
    public void test() {
        assertEquals(0, walletA.value());
        item.move(ownerA, AMOUNT, ownerB);
        assertEquals(-AMOUNT, walletA.value());
        assertEquals(AMOUNT, walletB.value());
        
        StateTestUtils.closeAndUndo(root);
        assertEquals(0, walletA.value());
        assertEquals(0, walletB.value());
        
        StateTestUtils.redo(root);
        assertEquals(-AMOUNT, walletA.value());
        assertEquals(AMOUNT, walletB.value());
    }
    
    
}
