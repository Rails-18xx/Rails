using GameLib.Net.Game;
using GameLib.Net.Game.State;

/**
 * Interface for rails.game components which can be configured from an XML
 * element.
 */
namespace GameLib.Net.Common.Parser
{
    public interface IConfigurable : ICreatable
    {
        /**
          * Instructs the component to configure itself from the provided XML
          * element.
          * 
          * @param element the XML element containing the configuration
          * @throws ConfigurationException
          */
        void ConfigureFromXML(Tag tag);

        /**
         * This method is intended to be called for each configurable
         * component, to perform any initialization activities that
         * require any other components to be initialized first. 
         * This includes creating any required relationships to other 
         * configured components and objects. 
         * <p>This method should be called where necessary after all 
         * XML file parsing has completed, so that all objects that
         * need to be related to do exist. 
         * @param parent The 'parent' configurable component is passed to allow 
         * the 'child' to access any other object without the need to resort to
         * static calls where possible. 
         */
        void FinishConfiguration(RailsRoot parent);
    
    }
}
