/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/Caption.java,v 1.4 2008/06/04 19:00:38 evos Exp $*/
package rails.ui.swing.elements;

import javax.swing.ImageIcon;

public class Caption extends Cell {
    private static final long serialVersionUID = 1L;

    public Caption(String text) {
        super(text, true);
    }

    public Caption (ImageIcon icon) {
        super (icon, true);
    }

}
