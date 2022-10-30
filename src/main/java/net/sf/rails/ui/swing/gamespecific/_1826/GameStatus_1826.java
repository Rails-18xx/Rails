package net.sf.rails.ui.swing.gamespecific._1826;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.ui.swing.GameStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class GameStatus_1826 extends GameStatus {

    private static final Logger log = LoggerFactory.getLogger(GameStatus_1826.class);

    public GameStatus_1826 () {
        super();
    }

    protected void initGameSpecificActions() {

        PublicCompany company;
        int compIndex, playerIndex;

        // Initialize all bonds buttons to not highlighted
        for (PublicCompany c : companyBondsRow.keySet()) {
            int i = c.getPublicNumber();
            setPoolBondsButton(i, false);
            for (int j=0; j < players.getNumberOfPlayers(); j++) {
                setPlayerCertButton (i, j, false);
            }
            if (compCanHoldOwnShares) setTreasuryBondsButton(i, false);

        }

        List<BuyBonds> buyableBonds =
                possibleActions.getType(BuyBonds.class);
        if (buyableBonds != null && !buyableBonds.isEmpty()) {
            for (BuyBonds buyBond : buyableBonds) {
                company = buyBond.getCompany();
                RailsOwner from = buyBond.getFrom();
                //RailsOwner to = buyBond.getTo();
                //PortfolioModel portfolio;
                compIndex = company.getPublicNumber();
                if (from instanceof BankPortfolio) {
                    if ((((BankPortfolio) from).getPortfolioModel()) == pool) {
                        setPoolBondsButton(compIndex, true, buyBond);
                    }
                } else if (from instanceof Player) {
                    playerIndex = ((Player)from).getIndex();
                    setPlayerBondsButton(compIndex, playerIndex, true, buyBond);
                }
            }
        }

        List<SellBonds> sellableBonds =
                possibleActions.getType(SellBonds.class);
        if (sellableBonds != null && !sellableBonds.isEmpty()) {
            for (SellBonds sellBonds : sellableBonds) {
                company = sellBonds.getCompany();
                compIndex = company.getPublicNumber();
                playerIndex = sellBonds.getPlayer().getIndex();
                setPlayerBondsButton(compIndex, playerIndex, true, sellBonds);
            }
        }
    }

    protected PossibleAction processGameSpecificActions(ActionEvent actor,
                                                        PossibleAction chosenAction) {

        if (chosenAction instanceof BuyBonds) {

            BuyBonds action = (BuyBonds) chosenAction;
            log.debug("Bonds action: {}", action);

            PublicCompany company = action.getCompany();
            int price = action.getPrice();
            int maxNumber = action.getMaxNumber();
            String fromId = action.getFromId();

            List<String> options = new ArrayList<>();

            for (int i = 1; i <= maxNumber; i++) {
                options.add(LocalText.getText(
                        (i == 1 ? "BuyBond" : "BuyBonds"),
                        i, company, fromId,
                        gameUIManager.format(i * price)));

            }
            int index = 0;
            if (options.size() > 1) {
                String message = LocalText.getText("PleaseSelect");
                String sp =
                        (String) JOptionPane.showInputDialog(this, message,
                                message, JOptionPane.QUESTION_MESSAGE,
                                null, options.toArray(new String[0]),
                                options.get(0));
                index = options.indexOf(sp);
            } else if (options.size() == 1) {
                String message = LocalText.getText("PleaseConfirm");
                int result =
                        JOptionPane.showConfirmDialog(this, options.get(0),
                                message, JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                index = (result == JOptionPane.OK_OPTION ? 0 : -1);
            }
            if (index < 0) {
                // cancelled
            } else {
                action.setNumberBought(index + 1);
            }
            return chosenAction;

        } else if (chosenAction instanceof SellBonds) {

            SellBonds action = (SellBonds) chosenAction;
            log.debug("Bonds action: {}", action);

            PublicCompany company = action.getCompany();
            int price = action.getPrice();
            int maxNumber = action.getMaxNumber();

            List<String> options = new ArrayList<>();

            for (int i = 1; i <= maxNumber; i++) {
                options.add(LocalText.getText(
                        (i == 1 ? "SellBond" : "SellBonds"),
                        i, company,
                        gameUIManager.format(i * price)));

            }
            int index = 0;
            if (options.size() > 1) {
                String message = LocalText.getText("PleaseSelect");
                String sp =
                        (String) JOptionPane.showInputDialog(this, message,
                                message, JOptionPane.QUESTION_MESSAGE,
                                null, options.toArray(new String[0]),
                                options.get(0));
                index = options.indexOf(sp);
            } else if (options.size() == 1) {
                String message = LocalText.getText("PleaseConfirm");
                int result =
                        JOptionPane.showConfirmDialog(this, options.get(0),
                                message, JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                index = (result == JOptionPane.OK_OPTION ? 0 : -1);
            }
            if (index < 0) {
                // cancelled
            } else {
                action.setNumberSold(index + 1);
            }
            return chosenAction;

        }
        return null;
    }

    protected void setPoolBondsButton(int i, boolean clickable, Object o) {

        setPoolBondsButton(i, clickable);
        if (clickable) syncToolTipText (bondsInPool[i], bondsInPoolButton[i]);
        if (clickable && o != null) {
            if (o instanceof PossibleAction)
                bondsInPoolButton[i].setPossibleAction((PossibleAction) o);
        }
    }

    protected void setPoolBondsButton(int i, boolean clickable) {
        boolean visible = bondsRowVisibilityObservers[i].lastValue();
        if (clickable) {
            bondsInPoolButton[i].setText(bondsInPool[i].getText());
            syncToolTipText (certInIPO[i], certInIPOButton[i]);
        } else {
            bondsInPoolButton[i].clearPossibleActions();
        }
        bondsInPool[i].setVisible(visible && !clickable);
        bondsInPoolButton[i].setVisible(visible && clickable);
    }

    protected void setPlayerBondsButton(int i, int j, boolean clickable, Object o) {

        if (j < 0) return;
        setPlayerCertButton(i, j, clickable);
        if (clickable) syncToolTipText (bondsPerPlayer[i][j], bondsPerPlayerButton[i][j]);
        if (clickable && o != null) {
            if (o instanceof PossibleAction) {
                bondsPerPlayerButton[i][j].setPossibleAction((PossibleAction) o);
                if (o instanceof SellBonds) {
                    addToolTipText (bondsPerPlayerButton[i][j], LocalText.getText("ClickForSell"));
                } else if (o instanceof BuyBonds) {
                    addToolTipText (bondsPerPlayerButton[i][j], LocalText.getText("ClickToSelectForBuying"));
                }
            }
        }
    }

    protected void setPlayerBondsButton(int i, int j, boolean clickable) {
        if (j < 0) return;
        boolean visible = bondsRowVisibilityObservers[i].lastValue();

        if (clickable) {
            bondsPerPlayerButton[i][j].setText(bondsPerPlayer[i][j].getText());
            syncToolTipText (certPerPlayer[i][j], certPerPlayerButton[i][j]);
        } else {
            bondsPerPlayerButton[i][j].clearPossibleActions();
        }
        bondsPerPlayer[i][j].setVisible(visible && !clickable);
        bondsPerPlayerButton[i][j].setVisible(visible && clickable);
    }

    protected void setTreasuryBondsButton(int i, boolean clickable) {

        boolean visible = bondsRowVisibilityObservers[i].lastValue();
        if (clickable) {
            bondsInTreasuryButton[i].setText(bondsInPool[i].getText());
            syncToolTipText (certInIPO[i], certInIPOButton[i]);
        } else {
            bondsInTreasuryButton[i].clearPossibleActions();
        }
        bondsInTreasury[i].setVisible(visible && !clickable);
        bondsInTreasuryButton[i].setVisible(visible && clickable);
    }

}
