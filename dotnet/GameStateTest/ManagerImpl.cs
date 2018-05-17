using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameStateTest
{
    public class ManagerImpl : Manager
    {
        public ManagerImpl(IItem parent, string id) : base(parent, id)
        {
        }
    }
}
