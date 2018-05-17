using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.State
{
    public class ChangeActionImpl : IChangeAction
    {
        virtual public IChangeActionOwner ActionOwner
        {
            get
            {
                return null;
            }
        }
    }
}
