import java.io.File;
import java.util.ArrayList;
import java.util.Collections; // Import the Collections utility

public class FilterNewest extends Filter {

    // The constructor can be empty or call super(null, null) since we don't use the parts.
    public FilterNewest() {
        super(null, null);
    }

    @Override
    public ArrayList<File> eval(String ignoredArgument, ArrayList<File> duplicateSet) {
        if (duplicateSet == null || duplicateSet.isEmpty()) {
            return new ArrayList<>();
        }

        File newestFile = duplicateSet.get(0);
        for (File file : duplicateSet) {
            if (file.lastModified() > newestFile.lastModified()) {
                newestFile = file;
            }
        }

        // The filter's only job is to return a list containing the one file it selected.
        ArrayList<File> result = new ArrayList<>();
        result.add(newestFile);
        return result;
    }
}
