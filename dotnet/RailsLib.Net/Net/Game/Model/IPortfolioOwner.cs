using System;
using System.Collections.Generic;
using System.Text;

/**
 * PortfolioOwner does not hold Portfolios directly, but indirect via PortfolioModel
 */

namespace GameLib.Net.Game.Model
{
    public interface IPortfolioOwner : IRailsOwner
    {
        PortfolioModel PortfolioModel { get; }
    }
}
