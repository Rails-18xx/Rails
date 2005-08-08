/*
 * Created on Apr 29, 2005
 */
package ui;

import game.*;

import java.awt.*;
import javax.swing.*;

/**
 * @author blentz
 */
public class LogWindow extends JFrame
{
   private JLabel message;
   private JScrollPane messageScroller;
   private JScrollBar vbar;
   private JPanel messagePanel;
   private static LogWindow messageWindow; 

   private static StringBuffer buffer = new StringBuffer("<html></html>");
   
   public LogWindow ()
   {
      messageWindow = this;
      
      message = new JLabel("");
      message.setBackground(Color.WHITE);
      message.setOpaque(true);
      message.setVerticalAlignment(SwingConstants.TOP);
      //message.setAutoscrolls(true);
      messagePanel = new JPanel(new GridBagLayout());
      messageScroller = new JScrollPane(message, 
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      vbar = messageScroller.getVerticalScrollBar();
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = gbc.gridy = 0;
      gbc.weightx = gbc.weighty = 0.5;
      gbc.fill = GridBagConstraints.BOTH;
      messagePanel.add(messageScroller, gbc);
      setContentPane (messagePanel);
      
      //updateStatus();
      setSize (400,400);
      setLocation(600,400);

      messagePanel.setBorder(BorderFactory.createEtchedBorder());
      
      

      setTitle("Rails: Game log");
      setVisible(true);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      //pack();
   }
   
   
     public static void addLog () {
         String newText = Log.getMessageBuffer();
         if (newText.length() > 0) {
             //buffer.insert (buffer.length()-7, "<br>");
             buffer.insert (buffer.length()-7, newText.replaceAll("\n", "<br>"));
         
             messageWindow.message.setText(buffer.toString());
             messageWindow.vbar.setValue(messageWindow.vbar.getMaximum());
         }
    }
}
