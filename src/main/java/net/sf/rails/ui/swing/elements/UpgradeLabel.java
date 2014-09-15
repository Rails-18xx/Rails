package net.sf.rails.ui.swing.elements;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import rails.game.action.LayTile;
import net.sf.rails.game.MapUpgrade;
import net.sf.rails.game.TileHexUpgrade;
import net.sf.rails.game.Station;
import net.sf.rails.game.Tile;

/** JLabel extension to allow attaching the internal hex ID */
public class UpgradeLabel extends JLabel {
    private static final long serialVersionUID = 1L;

    private static final Border BORDER = new EtchedBorder();

    private final MapUpgrade upgrade;

    private UpgradeLabel(ImageIcon hexIcon, TileHexUpgrade upgrade,
            String toolTipHeaderLine, String toolTipBody) {
        super(hexIcon);
        this.upgrade = upgrade;
        Tile tile = upgrade.getUpgrade().getTargetTile();
        this.setText(labelText(upgrade, true));
        this.setToolTipText(toolTipText(tile, toolTipHeaderLine, toolTipBody));
        this.setOpaque(true);
        this.setVisible(true);
        this.setBorder(BORDER);
    }

    public static UpgradeLabel create(ImageIcon icon, TileHexUpgrade upgrade,
            String toolTipHeaderLine, String toolTipBody) {
        return new UpgradeLabel(icon, upgrade, toolTipHeaderLine, toolTipBody);
    }
    
    public static UpgradeLabel create(ImageIcon icon, TileHexUpgrade upgrade) {
        return UpgradeLabel.create(icon, upgrade, null, null);
    }

    public MapUpgrade getUpgrade() {
        return upgrade;
    }

    private String labelText(TileHexUpgrade upgrade, boolean vertical) {
        Tile tile = upgrade.getUpgrade().getTargetTile();

        StringBuilder text = new StringBuilder();
        // TODO: Check if this still works, as toText is always defined 
        // if (rails.util.Util.hasValue(tile.getExternalId())) {
            text.append("<HTML>" + tile.toText());
            if (!tile.isUnlimited()) {
                if (vertical) {
                    text.append("<BR>");
                } else {
                    text.append(" ");
                }
                text.append(" (" + tile.getFreeCount() + ")");
            }
            LayTile action = upgrade.getAction();
            if (action.getSpecialProperty() != null) {
                text.append(
                        "<BR> <font color=red> ["
                                + action.getSpecialProperty().getOriginalCompany().getId()
                                + "] </font>" );
            }
            text.append("</HTML>");
        //}
        return text.toString();
    }

    private String toolTipText(Tile tile, String headerLine, String bodyText) {
        StringBuffer tt = new StringBuffer("<html>");
        if (headerLine != null && !headerLine.equals("")) {
            tt.append("<b><u>" + headerLine + "</u></b><br>");
        }
        if (bodyText != null) {
            tt.append("<b>" + bodyText + "</b>");
        }
        tt.append("<b>Tile</b>: ").append(tile.toText());
        if (tile.hasStations()) {
            int cityNumber = 0;
            // Tile has stations, but
            for (Station st : tile.getStations()) {
                cityNumber++; // = city.getNumber();
                tt.append("<br>  ").append(st.toText()).append(
                        cityNumber) // .append("/").append(st.getNumber())
                .append(": value ");
                tt.append(st.getValue());
                if (st.getBaseSlots() > 0) {
                    tt.append(", ").append(st.getBaseSlots()).append(
                            " slots");
                }
            }
        }
        tt.append("</html>");
        return tt.toString();
    }

}