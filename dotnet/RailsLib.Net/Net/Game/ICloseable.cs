using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    public interface ICloseable
    {
        void Close();

        string GetClosingInfo();
    }
}
