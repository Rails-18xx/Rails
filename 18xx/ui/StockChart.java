/*
 * Created on Feb 28, 2005
 */
package ui;

import java.awt.*;
import javax.swing.*;

/**
 * @author Brett Lentz
 */ 
public class StockChart extends JFrame
{

   private int numRows, numCols, 
   				hgap, vgap, 
   				bhoriz, bvert;
   private JPanel jContentPane = null;
   
   private JButton up;
   private JButton down;
   private JButton fwd;
   private JButton back;

   /*
    * adds values to the stockchart
    * defaults to right-to-left placement
    * 
    * should we add stock objects to the chart, 
    * and set toString()in the stock object to 
    * get its value?
    */
   public void addToStockChart(int value)
   {
      jContentPane.add(new JLabel(Integer.toString(value)));
   }
   
   public void addToStockChart(String string)
   {
      jContentPane.add(new JLabel(string));
   }
   
   private void initialize()
   {
      this.setSize(300, 200);
      this.setContentPane(getJContentPane());
      this.setTitle("Stock Chart");
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      up   = new JButton("Up");
      down = new JButton("Down");
      fwd  = new JButton("Forward");
      back = new JButton("Back");
      
      up.setSize(bhoriz, bvert);
      down.setSize(bhoriz, bvert);
      fwd.setSize(bhoriz, bvert);
      back.setSize(bhoriz, bvert);
   }

   private JPanel getJContentPane()
   {
      if (jContentPane == null)
      {
         jContentPane = new javax.swing.JPanel();
         jContentPane.setLayout(new GridLayout(numRows, numCols, hgap, vgap));
      }
      return jContentPane;
   }
   
   public StockChart()
   {
      super();
      
      hgap = 5;
      vgap = 5;
      bhoriz = 50;
      bvert = 100;
      
      //Need to create these methods, not necessarily with these 
      //method names.
      //numRows = getRowsFromXML();
      //numCols = getColsFromXML();
      
      numRows = 5;
      numCols = 5;
      
      initialize();
      
      for (int x = 0; x < 10; x++)
      {
         addToStockChart(x);
      }
      
      addToStockChart("");
      addToStockChart("");
      
      jContentPane.add(up);
      jContentPane.add(down);
      jContentPane.add(fwd);
      jContentPane.add(back);
      
      this.pack();
      this.setVisible(true);
   }
}