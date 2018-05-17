using System;
using System.Collections.Generic;
using System.Text;

/**
 * Classes that change properties of the revenue calculation
 * before the actual calculation starts implement a the static modifier.
 *  
 * They have to register themselves to the RevenueManager via the GameManager instance.
 * @author freystef
 *
 */

namespace GameLib.Net.Algorithms
{
    public interface IRevenueStaticModifier
    {
        /** method called after the setup of the revenueAdapter, but before the actual calculation
         * Allows to call methods of the revenue adapter to change the content
         * @param revenueAdapter
         * @return if true a pretty print text is required
         */
        bool ModifyCalculator(RevenueAdapter revenueAdapter);

        /** 
         * Allows to append additional text
         * Only called if modifyCalculator returned true
         * @return string output for display in Rails */
        string PrettyPrint(RevenueAdapter revenueAdapter);
    }
}
