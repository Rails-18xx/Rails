using System;


namespace GameLib.Net.Game.State
{
    public interface IChangeReporter
    {
        void Init(ChangeStack stack);

        void UpdateOnClose();

        void InformOnUndo();

        void InformOnRedo();

        void UpdateAfterUndoRedo();
    }
}
