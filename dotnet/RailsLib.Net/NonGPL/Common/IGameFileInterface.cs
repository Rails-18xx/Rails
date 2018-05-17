using System;


namespace GameLib.Net.Common
{
    public interface IGameFile
    {
        string LoadFile();
        string Name { get; }
        bool Delete();
    }

    public interface IGameFileInterface
    {
        
        // Get system specific folder that stores application data folders
        IGameFile GetAppDataDir();
        
       // Returns the folder that contains all rails specific user data
       // Returns null if the operations fails 
       // @param create set to true creates the folder if it does not exist 
       // @return rails specific configuration folder     
        IGameFile GetConfigurationFolder(bool create);
        /**
         * Returns a sub-folder inside the Rails configuration folder
         * Returns null if the operations fails 
         * @param subFolder the folder inside
         * @param create set to true creates the subFolder and/or
         * configFolder if it does not exist 
         * @return rails specific configuration folder 
         */
        IGameFile GetConfigurationFolder(string subFolder, bool create);
    }
}
