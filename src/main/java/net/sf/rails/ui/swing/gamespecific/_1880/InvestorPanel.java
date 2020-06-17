package net.sf.rails.ui.swing.gamespecific._1880;

import net.sf.rails.game.specific._1880.GameManager_1880;
import net.sf.rails.game.specific._1880.Investor_1880;
import net.sf.rails.ui.swing.core.GridAxis;
import net.sf.rails.ui.swing.core.GridTable;
import net.sf.rails.ui.swing.core.TableUI;

import javax.swing.*;
import java.util.List;

public class InvestorPanel {

    private enum Rows {HEADER}

    private enum Cols {INVESTOR, COMPANY, PLAYER}

    private final JPanel panel;

    public InvestorPanel(GameManager_1880 gameManager) {

        List<Investor_1880> investors = Investor_1880.getInvestors(gameManager.getRoot().getCompanyManager());

        GridAxis rows = GridAxis.builder()
                .add(Rows.HEADER).add(investors, Investor_1880.class)
                .build();

        GridAxis cols = GridAxis.builder()
                .add(Cols.INVESTOR).add(Cols.COMPANY).add(Cols.PLAYER)
                .build();

        GridTable gridTable = GridTable.builder(rows, cols)
                .row().add("Investor").add("Company").add("Owner")
                .row().add(InvestorACCs.INVESTOR).headerFormat().add(InvestorACCs.COMPANY).add(InvestorACCs.PLAYER)
                .build();

        TableUI table = TableUI.from(gridTable);

        panel = table.convertToPanel();
        panel.setBorder(BorderFactory.createEtchedBorder());
    }

    public JPanel getPanel() {
        return panel;
    }
}
