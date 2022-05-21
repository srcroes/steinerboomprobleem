package utils.stp;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by Stefan Croes
 */
public class STPFileNameFilter implements FilenameFilter {
    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".stp");
    }
}
