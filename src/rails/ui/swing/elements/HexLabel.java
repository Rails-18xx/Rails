package rails.ui.swing.elements;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import rails.game.HexUpgrade;
import rails.game.Station;
import rails.game.Tile;

/** JLabel extension to allow attaching the internal hex ID */
public class HexLabel extends JLabel {

    private static final long serialVersionUID = 1L;

    private final HexUpgrade upgrade;

    public HexLabel(ImageIcon hexIcon, HexUpgrade upgrade) {
        this(hexIcon, upgrade, null, null);
    }

    public HexLabel(ImageIcon hexIcon, HexUpgrade upgrade,
            String toolTipHeaderLine, String toolTipBody) {
        super(hexIcon);
        this.upgrade = upgrade;
        Tile tile = upgrade.getUpgrade().getTargetTile();
        this.setText(labelText(tile, true));
        this.setToolTipText(toolTipText(tile, toolTipHeaderLine, toolTipBody));
    }
    
    public HexLabel(ImageIcon hexIcon, Tile tile) {
        super(hexIcon);
        this.upgrade = null;
        this.setText(labelText(tile, false));
        this.setToolTipText(toolTipText(tile, null, null));
    }

    public HexUpgrade getUpgrade() {
        return upgrade;
    }

    private String labelText(Tile tile, boolean vertical) {
        StringBuffer text = new StringBuffer();
        // TODO: Check if this still works, as toText is always defined 
        // if (rails.util.Util.hasValue(tile.getExternalId())) {
            text.append("<HTML><BODY>" + tile.toText());
            if (!tile.isUnlimited()) {
                if (vertical) {
                    text.append("<BR>");
                } else {
                    text.append(" ");
                }
                text.append(" (" + tile.getFreeCount() + ")");
            }
            text.append("</BODY></HTML>");
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