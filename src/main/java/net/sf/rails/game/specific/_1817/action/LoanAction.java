package net.sf.rails.game.specific._1817.action;

/**
 * Interface to provide safe access to 1817 loan data without reflection.
 */
public interface LoanAction {
    String getCompanyId();
    int getMaxLoansAllowed();
    void setLoansToTake(int count);
}