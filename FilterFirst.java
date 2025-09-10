import java.io.File;
import java.util.ArrayList;
import java.util.Collections; // Import the Collections utility

public class FilterFirst extends Filter {

    /**
     * A rule that selects the "first" file from a list of duplicates.
     * It sorts the list alphabetically by path to ensure it always finds
     * the true first file, regardless of input order.
     */
    public FilterFirst() {
        super(null, null);
    }

    @Override
    public ArrayList<File> eval(String ignoredArgument, ArrayList<File> duplicateSet) {

        ArrayList<File> fileToKeep = new ArrayList<>();

        if (duplicateSet == null || duplicateSet.isEmpty()) {
            return fileToKeep;
        }

        // The crucial correction: Sort the list first to ensure determinism.
        // File's natural order is alphabetical by path.
        Collections.sort(duplicateSet);

        // Now, we can be certain we are selecting the true "first" file.
        fileToKeep.add(duplicateSet.get(0));

        return fileToKeep;
    }
}
