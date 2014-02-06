package net.sf.rails.game.state;

import static org.junit.Assert.*;

import net.sf.rails.game.state.Ownable;
import net.sf.rails.game.state.OwnableItem;
import net.sf.rails.game.state.Owner;
import net.sf.rails.game.state.PortfolioManager;
import net.sf.rails.game.state.PortfolioSet;
import net.sf.rails.game.state.Root;

import org.junit.Before;
import org.junit.Test;

public class PortfolioManagerTest {

    private final static String PORTFOLIO_A_ID = "PortfolioA";
    private final static String PORTFOLIO_B_ID = "PortfolioB";
    private final static String OWNER_A_ID = "OwnerA";
    private final static String OWNER_B_ID = "OwnerB";
    private final static String OWNER_C_ID = "OwnerC";
    
    private Root root;
    private PortfolioManager pm;
    private PortfolioSet<Ownable> portfolioA, portfolioB;
    private Owner ownerA, ownerB, ownerC;
    
    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        pm = root.getStateManager().getPortfolioManager();
        ownerA = OwnerImpl.create(root, OWNER_A_ID);
        ownerB = OwnerImpl.create(root, OWNER_B_ID);
        ownerC = OwnerImpl.create(root, OWNER_C_ID);
        portfolioA = PortfolioSet.create(ownerA, PORTFOLIO_A_ID , Ownable.class);
        portfolioB = PortfolioSet.create(ownerB, PORTFOLIO_B_ID , Ownable.class);
        StateTestUtils.close(root);
    }

    @Test
    public void testPMKey() {
        // create various keys
        PortfolioManager.PMKey<?> keyA1, keyA2, keyADiff, keyB;
        keyA1 = pm.createPMKey(Ownable.class, ownerA);
        keyA2 = pm.createPMKey(Ownable.class, ownerA);
        keyADiff = pm.createPMKey(OwnableItem.class, ownerA);
        keyB = pm.createPMKey(Ownable.class, ownerB);

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
        assertNotNull(pm.getUnkownOwner());
    }

    @Test
    public void testRemovePortfolio() {
        pm.removePortfolio(portfolioA);
        assertNull(pm.getPortfolio(Ownable.class, ownerA));
        
        // undo and redo check
        StateTestUtils.closeAndUndo(root);
        assertSame(portfolioA, pm.getPortfolio(Ownable.class, ownerA));
        StateTestUtils.redo(root);
        assertNull(pm.getPortfolio(Ownable.class, ownerA));
    }

    @Test
    public void testAddPortfolio() {
        // remove first to prepare
        pm.removePortfolio(portfolioA);
        StateTestUtils.close(root);
        assertNull(pm.getPortfolio(Ownable.class, ownerA));
        
        // then add
        pm.addPortfolio(portfolioA);
        assertSame(portfolioA, pm.getPortfolio(Ownable.class, ownerA));
        
        // undo and redo check
        StateTestUtils.closeAndUndo(root);
        assertNull(pm.getPortfolio(Ownable.class, ownerA));

        // redo check
        StateTestUtils.redo(root);
        assertSame(portfolioA, pm.getPortfolio(Ownable.class, ownerA));
    }
    
    @Test
    public void testGetPortfolio() {
        assertSame(portfolioA, pm.getPortfolio(Ownable.class, ownerA));
        assertSame(portfolioB, pm.getPortfolio(Ownable.class, ownerB));
        assertNull(pm.getPortfolio(OwnableItem.class, ownerA));
        assertNull(pm.getPortfolio(Ownable.class, ownerC));
        assertNull(pm.getPortfolio(Ownable.class, pm.getUnkownOwner()));
    }

}
