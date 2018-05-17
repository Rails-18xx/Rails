using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Algorithms
{
    public interface IRevenueListener
    {
        void RevenueUpdate(int revenue, bool finalResult);
    }

    public interface IRevenueListenerArgs
    {
        int Revenue { get; set; }
        bool FinalResult { get; set; }
    }
}
