/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/MessagePanel.java,v 1.7 2010/01/05 20:54:05 evos Exp $*/
package net.sf.rails.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MessagePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    //the height of this panel (fixed because scroll bar is used)
    public static final int DEFAULT_HEIGHT = 45;

    //the height of this panel if details are open
    public static final int FULL_HEIGHT = 90;
    public static final int MIN_WIDTH = 100;
    public static final int SCROLL_UNIT = 8;
    public static final int MIN_MARGIN_FOR_FULL_HEIGHT = 8;

    private JLabel message;
    private JScrollPane parentSlider;

    private String currentMessage;
    private StringBuffer currentInformation;
    private String currentDetails;
    private boolean showDetails;

    private Color background = new Color(225, 225, 225);

    public MessagePanel() {
        super();

        setBackground(background);
        setBorder(new EmptyBorder(0,0,0,0));

        //add layout manager
        //necessary for auto word-wrap when diminishing width
        setLayout(new GridLayout(1,1));

        message = new JLabel("");
        message.setBackground(background);
        message.setVerticalAlignment(SwingConstants.CENTER);
        message.setHorizontalAlignment(SwingConstants.CENTER);
        message.setOpaque(true);

        add(message);
        message.setVisible(true);
        setVisible(true);

        this.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                toggleDetailsEnablement();
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {}
            @Override
            public void mouseExited(MouseEvent arg0) {}
            @Override
            public void mousePressed(MouseEvent arg0) {}
            @Override
            public void mouseReleased(MouseEvent arg0) {}
        });

    }

    /**
     * @param parentSlider Component between OR window and the panel
     */
    public void setParentSlider(JScrollPane parentSlider) {
        this.parentSlider = parentSlider;
        parentSlider.setBorder(BorderFactory.createLoweredBevelBorder());
        parentSlider.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT);
        parentSlider.setPreferredSize(new Dimension(MIN_WIDTH, DEFAULT_HEIGHT));
    }

    private void disableDetails() {
        if (showDetails) {
            showDetails = false;
            parentSlider.setPreferredSize(new Dimension(MIN_WIDTH, DEFAULT_HEIGHT));
            ((JComponent)parentSlider.getParent()).revalidate();
        }
    }

    private void enableDetails() {
        if (!showDetails && currentDetails != null) {
            showDetails = true;
            parentSlider.setPreferredSize(new Dimension(MIN_WIDTH, FULL_HEIGHT));
            ((JComponent)parentSlider.getParent()).revalidate();
        }
    }

    private void toggleDetailsEnablement() {
        if (showDetails) {
            disableDetails();
        } else {
            enableDetails();
        }
        updateMessageText();
    }

    private void updateMessageText() {
        StringBuilder messageText = new StringBuilder() ;
        if (currentMessage != null) {
            messageText.append(currentMessage);
        }
        if (currentInformation != null) {
            messageText.append("<span style='color:green'>");
            messageText.append(currentInformation);
            messageText.append("</span>");
        }
        if (showDetails) {
            messageText.append("<span style='color:blue; font-size:80%'>");
            messageText.append(currentDetails);
            messageText.append("</span>");
        } else if (currentDetails != null) {
            messageText.append("<span style='color:blue; font-size:80%'>");
            messageText.append("&nbsp; Click for more details");
            messageText.append("</span>");
        }
        // display
        String text = messageText.toString();
        message.setText("<html><center>" + text + "</center></html>");

    }

    public void setMessage(String messageText) {
        currentMessage = messageText;
        currentInformation = null;
        currentDetails = null;
        disableDetails();
        updateMessageText();
    }

    public void setInformation(String infoText) {
        currentInformation = new StringBuffer();
        currentInformation.append("<BR>" + infoText);
        updateMessageText();
    }

    public void setDetail(String detailText) {
        currentDetails = "<BR>" + detailText;
        updateMessageText();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension nativeSize = super.getPreferredSize();
        if (parentSlider == null) return nativeSize;
        int width = parentSlider.getSize().width
                - parentSlider.getVerticalScrollBar().getWidth()
                - 5;
        if (width <= 0) width = 1;
        return new Dimension (width , nativeSize.height);
    }
}
