/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/MessagePanel.java,v 1.7 2010/01/05 20:54:05 evos Exp $*/
package rails.ui.swing;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public class MessagePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private JLabel message;
    
    private String currentMessage;
    private StringBuffer currentInformation;
    private List<String> currentDetails = new ArrayList<String>();
    private boolean showDetails;

    Color background = new Color(225, 225, 225);

    public MessagePanel() {
        super();

        setBackground(background);
        setLines(1);
        setBorder(BorderFactory.createLoweredBevelBorder());

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
                showDetails = !showDetails;
                updateMessageText();
            }

            public void mouseEntered(MouseEvent arg0) {}
            public void mouseExited(MouseEvent arg0) {}
            public void mousePressed(MouseEvent arg0) {}
            public void mouseReleased(MouseEvent arg0) {}
        });
        
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
            for (String detail:currentDetails) {
                messageText.append(detail);
            }
            messageText.append("</span>");
        } else if (currentDetails.size() != 0) {
            messageText.append("<span style='color:blue; font-size:80%'>");
            messageText.append("<BR> Click for more details");
            messageText.append("</span>");
        }
        // display
        String text = messageText.toString();
        //int lines = text.split("<[Bb][Rr]>").length + 1;
//        setLines(lines);
        message.setText("<html><center>" + text + "</center></html>");
        
    }
    
    public void setMessage(String messageText) {
        currentMessage = messageText;
        currentInformation = null;
        currentDetails.clear();
        showDetails = false;
        updateMessageText();
    }
    
    public void addInformation(String infoText) {
        if (currentInformation == null) {
            currentInformation = new StringBuffer();
        }
        currentInformation.append("<BR>" + infoText);
        updateMessageText();
    }
    
    public void addDetail(String detailText) {
        currentDetails.add("<BR>" + detailText);
        updateMessageText();
    }

    public void setLines(int numberOfLines) {
        setSize(1000, numberOfLines * 12);
    }

}
