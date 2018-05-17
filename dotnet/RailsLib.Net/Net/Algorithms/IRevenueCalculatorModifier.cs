using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Algorithms
{
    public interface IRevenueCalculatorModifier
    {
        /**
        * Allows to replace the usual calculation process (evaluate all trains simultaneously)
        * If several dynamic modifier have their own method, their prediction values are added up.  
        * @return optimal value
        */
        int CalculateRevenue(RevenueAdapter revenueAdpater);
    }
}
