using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameStateTest
{
    public static class StateTestUtils
    {
        public static Root SetUpRoot()
        {
            Root root = Root.Create();
            Close(root);
            return root;
        }

        public static void Close(Root root)
        {
            // starts a non-initial ChangeSet
            root.StateManager.ChangeStack.Close(new ChangeActionImpl());
        }

        public static void Undo(Root root)
        {
            root.StateManager.ChangeStack.Undo();
        }

        public static void CloseAndUndo(Root root)
        {
            root.StateManager.ChangeStack.Close(new ChangeActionImpl());
            root.StateManager.ChangeStack.Undo();
        }

        public static void Redo(Root root)
        {
            root.StateManager.ChangeStack.Redo();
        }

        public static ChangeSet GetPreviousChangeSet(Root root)
        {
            return root.StateManager.ChangeStack.GetClosedChangeSet();
        }

        public static int SubListIndex<T>(this IList<T> list, int start, IList<T> sublist)
        {
            for (int listIndex = start; listIndex < list.Count - sublist.Count + 1; listIndex++)
            {
                int count = 0;
                while (count < sublist.Count && sublist[count].Equals(list[listIndex + count]))
                    count++;
                if (count == sublist.Count)
                    return listIndex;
            }
            return -1;
        }

        public static bool DictionaryEquals<K, V>(IDictionary<K,V> dic1, IDictionary<K,V> dic2)
        {
            return dic1.Count == dic2.Count && !dic1.Except(dic2).Any();
        }

        public static bool CollectionEquals<T>(IReadOnlyCollection<T> a, IReadOnlyCollection<T>b)
        {
            return !(a.Except(b).Any() || b.Except(a).Any());
        }

        public static bool ContainsOnly<T>(IReadOnlyCollection<T> a, T item)
        {
            return a.Count == 1 && a.Contains(item);
        }

        public static bool ContainsAllItems<T>(IEnumerable<T> a, IEnumerable<T> b)
        {
            return !b.Except(a).Any();
        }
    }
}
