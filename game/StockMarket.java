/*
 * Created on Feb 23, 2005
 *
 */
package game;

import java.util.ArrayList;

/**
 * @author a-blentz
 */
public class StockMarket
{
   int[][] stockChart;
   ArrayList ipoPile;
   ArrayList companiesStarted;
   
   /**
    * @return Returns the companiesStarted.
    */
   public ArrayList getCompaniesStarted()
   {
      return companiesStarted;
   }
   /**
    * @param companiesStarted The companiesStarted to set.
    */
   public void setCompaniesStarted(BigCompany companyStarted)
   {
      companiesStarted.add(companyStarted);
   }
   /**
    * @return Returns the ipoPile.
    */
   public ArrayList getIpoPile()
   {
      return ipoPile;
   }
   /**
    * @param ipoPile The ipoPile to set.
    */
   public void addShareToPile(Stock stock)
   {
      ipoPile.add(stock);
   }
   public Stock removeShareFromPile(Stock stock)
   {
      if (ipoPile.contains(stock))
      {
         int index = ipoPile.lastIndexOf(stock);
         stock = (Stock) ipoPile.get(index);
         ipoPile.remove(index);
         return stock;
      }
      else
      {
         return null;
      }

   }
   public void moveChitUp(BigCompany company)
   {
      //if not at the top
      //move chit up
      //else
      //move chit forward
   }
   public void moveChitDown(BigCompany company)
   {
      //if not on a ledge
      //move chit down
      //
      //for games like 1870, we'll need to know if there's any values 
      //below the ledge that can be moved down if the player sells >2
      //shares of a company's stock during his turn
      //or if the ledge is the end of the road.
   }
   public void moveChitForward(BigCompany company)
   {
      //if not on a ledge
      //move chit forward
      //else
      //move chit up
   }
   public void moveChitBackward(BigCompany company)
   {
      //if not on the edge (rare)
      //move chit back
      //else
      //move chit down
   }
}
