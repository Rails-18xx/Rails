using GameLib.Net.Game;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Configure provides static methods that come along with the Configurable and Creatable Interfaces
 * 
 * Remark: Collects code from various mathods in ComponentManager, GameManager and other places in Rails1.x

 */
namespace GameLib.Net.Common.Parser
{
    public class Configure
    {
        static Logger<Configure> log = new Logger<Configure>();

        /** Check if a classname can be instantiated.
         * Throws a ConfiguratioNException if not.
         * @param className
         * @throws ConfigurationException
         */
        public static void CanTypeBeInstantiated(string className)
        {
            try
            {
                var t = Type.GetType(className, true);
            }
            //catch (ClassNotFoundException e) {
            catch (Exception e)
            {
                throw new ConfigurationException("Cannot find class "
                        + className, e);
            }
        }

        public static Type GetTypeForName<T>(string className) where T : ICreatable
        {
            Type ret;
            try
            {
                ret = Type.GetType(className, true);
                if (!typeof(T).Equals(Type.GetType(className)) && !ret.IsSubclassOf(typeof(T)))
                {
                    throw new ConfigurationException(LocalText.GetText("ClassNotSubclass"));
                }
            }
            catch (Exception e)
            {
                throw new ConfigurationException("Can't create subclass", e);
            }
            return ret;
        }
        //public static <T extends Creatable> Class<? extends T> GetClassForName(Class<T> clazz, string className)
        //{
        //    Class<? extends T> subClazz;
        //        try {
        //        subClazz = (Class <? extends T >) Class.forName(className).asSubclass(clazz);
        //    } catch (Exception e) {
        //        throw new ConfigurationException(LocalText.getText(
        //                "ComponentHasNoClass", className), e);
        //    }
        //        return subClazz;
        //}

        //public static T Create<T>(string className, IRailsItem parent, string id)
        //    where T : ICreateable
        //{
        //    return (T)Create(Type.GetType(className), parent, id);
        //}

        public static object Create(Type type, IRailsItem parent, string id)
        {
            return Activator.CreateInstance(type, new object[] { parent, id });
        }

        public static object Create(string typeName, IRailsItem parent, string id)
        {
            //return Activator.CreateInstance(type, new object[] { parent, id });
            return Create(Type.GetType(typeName), parent, id);
        }

        public static T Create<T>(string typeName, IRailsItem parent, string id) where T : ICreatable
        {
            return (T)Create(typeName, parent, id);
        }

        public static T Create<T>(Type type, IRailsItem parent, string id) where T : ICreatable
        {
            return (T)Create(type, parent, id);
        }

        //public static T Create<T, P>(Type type, P parent, string id)
        //    where T : ICreatable
        //    where P : IRailsItem
        //{
        //    return (T)Create(type, parent, id);
        //}

        //        public static <T extends Creatable> T create(Class<T> clazz, string className, RailsItem parent, string id)
        //{
        //    Class<? extends T> subClazz = getClassForName(clazz, className);
        //        return create(subClazz, RailsItem.class, parent, id);
        //    }

        //    public static <T extends Creatable, P extends RailsItem> T create(Class<T> clazz, string className, Class<P> parentClazz, P parent, string id)
        //{
        //    Class<? extends T> subClazz = getClassForName(clazz, className);
        //        return create(subClazz, parentClazz, parent, id );
        //}

        //public static <T extends Creatable> T create(Class<T> clazz, RailsItem parent, string id)
        //{
        //        return create(clazz, RailsItem.class, parent, id);
        //    }


        //    public static <T extends Creatable, P extends RailsItem> T create(
        //        Class<T> clazz, Class<P> parentClazz, P parent, string id)
        //{
        //    T component;
        //        try {
        //        Constructor <? extends T > subConstructor = clazz.getConstructor(parentClazz, string.class);
        //            component = subConstructor.newInstance(parent, id);
        //        } catch (Exception ex) {
        //            throw new ConfigurationException(LocalText.getText(
        //                    "ComponentHasNoClass", clazz.getName()), ex);
        //        }
        //        return component;
        //    }

    }
}
