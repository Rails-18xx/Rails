package net.sf.rails.game.state;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.sf.rails.game.state.Countable;
import net.sf.rails.game.state.CountableItem;
import net.sf.rails.game.state.Owner;
import net.sf.rails.game.state.Root;
import net.sf.rails.game.state.WalletBag;
import net.sf.rails.game.state.WalletManager;

import org.junit.Before;
import org.junit.Test;

public class WalletManagerTest {
    private final static String WALLET_A_ID = "WalletA";
    private final static String WALLET_B_ID = "WalletB";
    private final static String ITEM_ID = "Item";
    private final static String OWNER_A_ID = "OwnerA";
    private final static String OWNER_B_ID = "OwnerB";
    private final static String OWNER_C_ID = "OwnerC";
    
    private Root root;
    private WalletManager wm;
    private CountableItemImpl item;
    private WalletBag<Countable> walletA, walletB;
    private Owner ownerA, ownerB, ownerC;
    
    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        wm = root.getStateManager().getWalletManager();
        item = CountableItemImpl.create(root, ITEM_ID);
        ownerA = OwnerImpl.create(root, OWNER_A_ID);
        ownerB = OwnerImpl.create(root, OWNER_B_ID);
        ownerC = OwnerImpl.create(root, OWNER_C_ID);
        walletA = WalletBag.create(ownerA, WALLET_A_ID , Countable.class, item);
        walletB = WalletBag.create(ownerB, WALLET_B_ID , Countable.class, item);
        StateTestUtils.close(root);
    }

    @Test
    public void testWMKey() {
        // create various keys
        WalletManager.WMKey<?> keyA1, keyA2, keyADiff, keyB;
        keyA1 = wm.createWMKey(Countable.class, ownerA);
        keyA2 = wm.createWMKey(Countable.class, ownerA);
        keyADiff = wm.createWMKey(CountableItem.class, ownerA);
        keyB = wm.createWMKey(Countable.class, ownerB);

        // check that the two A keys are identical...
        assertTrue(keyA1.equals(keyA2));
        assertTrue(keyA2.equals(keyA1));

        // ... the other differ
        assertFalse(keyA1.equals(keyADiff));
        assertFalse(keyADiff.equals(keyA1));
        assertFalse(keyA1.equals(keyB));
        assertFalse(keyB.equals(keyA1));
        
        // check hashcodes
        assertTrue(keyA1.hashCode() == keyA2.hashCode());
        assertFalse(keyA1.hashCode() == keyADiff.hashCode());
        assertFalse(keyA1.hashCode() == keyB.hashCode());
    }
    
    @Test
    public void testGetUnkownOwner() {
        assertNotNull(wm.getUnkownOwner());
    }

    @Test
    public void testRemoveWallet() {
        wm.removeWallet(walletA);
        assertNull(wm.getWallet(Countable.class, ownerA));
        
        // undo and redo check
        StateTestUtils.closeAndUndo(root);
        assertSame(walletA, wm.getWallet(Countable.class, ownerA));
        StateTestUtils.redo(root);
        assertNull(wm.getWallet(Countable.class, ownerA));
    }

    @Test
    public void testAddWallet() {
        // remove first to prepare
        wm.removeWallet(walletA);
        StateTestUtils.close(root);
        assertNull(wm.getWallet(Countable.class, ownerA));
        
        // then add
        wm.addWallet(walletA);
        assertSame(walletA, wm.getWallet(Countable.class, ownerA));
        
        // undo and redo check
        StateTestUtils.closeAndUndo(root);
        assertNull(wm.getWallet(Countable.class, ownerA));

        // redo check
        StateTestUtils.redo(root);
        assertSame(walletA, wm.getWallet(Countable.class, ownerA));
    }
    
    @Test
    public void testGetWallet() {
        assertSame(walletA, wm.getWallet(Countable.class, ownerA));
        assertSame(walletB, wm.getWallet(Countable.class, ownerB));
        assertNull(wm.getWallet(CountableItem.class, ownerA));
        assertNull(wm.getWallet(Countable.class, ownerC));
        assertNull(wm.getWallet(Countable.class, wm.getUnkownOwner()));
    }

}
