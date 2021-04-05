package net.sf.rails.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class XmlConverter {
    public static void main(String[] args) {
        Collection<File> railsFiles = FileUtils.listFiles(
                new File("src/test/resources"),
                new RegexFileFilter("(.+).rails"),
                DirectoryFileFilter.DIRECTORY
        );

        for (File file : railsFiles) {
            System.out.println(file.toString());

            GameLoader loader = new GameLoader();

            loader.createFromFile(file);

            XmlGameSaver saver = new XmlGameSaver(loader);

            try {
                saver.saveGame(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
