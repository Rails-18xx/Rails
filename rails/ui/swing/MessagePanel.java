/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/MessagePanel.java,v 1.7 2010/01/05 20:54:05 evos Exp $*/
package rails.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MessagePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    //the height of this panel (fixed because scroll bar is used)
    public static final int defaultHeight = 45;

    //the height of this panel if details are open
    public static final int fullHeight = 90;
    public static final int minWidth = 100;
    public static final int scrollUnit = 8;
    public static final int minMarginForFullHeight = 8;

    private JLabel message;
    private JScrollPane parentSlider;
    
    private String currentMessage;
    private StringBuffer currentInformation;
    private String currentDetails;
    private boolean showDetails;

    Color background = new Color(225, 225, 225);

    public MessagePanel() {
        super();

        setBackground(background);
        setBorder(new EmptyBorder(0,0,0,0));
        
        message = new JLabel("");
        message.setBackground(background);
        message.setVerticalAlignment(SwingConstants.CENTER);
        message.setHorizontalAlignment(SwingConstants.CENTER);
        message.setOpaque(true);
        
        add(message);
        message.setVisible(true);
        setVisible(true);
        
        this.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent arg0) {
                toggleDetailsEnablement();
            }

            public void mouseEntered(MouseEvent arg0) {}
            public void mouseExited(MouseEvent arg0) {}
            public void mousePressed(MouseEvent arg0) {}
            public void mouseReleased(MouseEvent arg0) {}
        });
        
    }

    /**
     * @param parentSlider Component between OR window and the panel
     */
    public void setParentSlider(JScrollPane parentSlider) {
        this.parentSlider = parentSlider;
        parentSlider.setBorder(BorderFactory.createLoweredBevelBorder());
        parentSlider.getVerticalScrollBar().setUnitIncrement(scrollUnit);
        parentSlider.setPreferredSize(new Dimension(minWidth,defaultHeight));
    }
    
    private void disableDetails() {
        if (showDetails) {
            showDetails = false;
            parentSlider.setPreferredSize(new Dimension(minWidth,defaultHeight));
            ((JComponent)parentSlider.getParent()).revalidate();
        }
    }
    
    private void enableDetails() {
        if (!showDetails && currentDetails != null) {
            showDetails = true;
            parentSlider.setPreferredSize(new Dimension(minWidth,fullHeight));
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
        StringBuffer messageText = new StringBuffer() ;
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

}
