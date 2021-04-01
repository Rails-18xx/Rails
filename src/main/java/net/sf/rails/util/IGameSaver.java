package net.sf.rails.util;

import java.io.File;
import java.io.IOException;

public interface IGameSaver {
    void saveGame(File file) throws IOException;
}
