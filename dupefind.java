import java.io.*;
import java.util.ArrayList;
import javax.lang.model.util.ElementScanner6;
import  java.nio.file.Files;

//public Filelist fll;
//public File fl;

public class dupefind {


	private static boolean findFile (ArrayList<File> sl, ArrayList<File> al)
	{
		// any entry in al is found in sl
		for  (File f : al)
			if (sl.contains(f))
				return true;

		return false;
	}

	private static ArrayList<File> findEntry(ArrayList<File> al, ArrayList<ArrayList<File>> aaf)
	{
		int count = 0;
		ArrayList<File> entry = null;
		for (ArrayList<File> sl: aaf)
		{
			if (findFile(sl,al))
			{
				count ++;
				entry = sl;
				// will return entry here, but doing testing first.
			}

		}
		assert(count <= 1); 
		// 0 <= count <= 1
		//if (count == 0)
			
		return entry;
	}

	private static String readln() {
		String ln = "";

		try {
			BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
			ln = is.readLine();
                // if (ln.length() == 0 ) return null;
		} catch (IOException e) {
			System.out.println("IOException: " + e);
		}
		return ln;
	}

	private static void printFile(File f)
	{
		// make an ls -l type display
	}

	private static void printParentLine (ArrayList<File> al)
	{
		if (al != null )
		{
		boolean first = true;
		for (File lfl : al)
					{
							if(!first)
									System.out.print(" : ");
							first=false;
							try {
									System.out.print(lfl.getParent());
							} catch (Exception e) {
									System.out.print("*ERROR*");
							}
					}
					System.out.println("");
				}
				else      
				System.out.println("null");
	}
	public static void printParent(ArrayList<ArrayList<File>> duplicatelist)
	{
			for (ArrayList<File> al : duplicatelist)
				printParentLine(al);
	}

	private static void printLine (ArrayList<File> al)
	{
		if (al != null )
		{
		boolean first = true;
		for (File lfl : al)
					{
							if(!first)
									System.out.print(" : ");
							first=false;
							try {
									System.out.print(lfl.getCanonicalPath());
							} catch (Exception e) {
									System.out.print("*ERROR*");
							}
					}
					System.out.println("");
				}
				else      
				System.out.println("null");
	}
	public static void print(ArrayList<ArrayList<File>> duplicatelist)
	{
			for (ArrayList<File> al : duplicatelist)
				printLine(al);
	}
	public static void main(String[] args) {
		try {
			Rulelist  rulelist  = new Rulelist();
			System.out.println("args len:" + args.length);
			final Filelist fll = new Filelist();
			if (args.length == 0)
				fll.addScanDir(".");
			else 
			{
				for (String sd: args)
					fll.addScanDir(sd);
			}

			System.out.println("-- SCAN --");
			// start this in thread

			new Thread(new Runnable() {
				@Override
				public void run() { 
					// System.out.println("fll -> " + fll);
					fll.filescan();
				}
			   }).start();

			// loop progress...
			while (!fll.getScanCompleted()) { 
				try {
					System.out.println("Files Remaining... " + fll.getScanRemain());
					Thread.sleep(1000); 
				} catch (InterruptedException ie) { ; }
			}
		
			int totalfiles = fll.countfiles();

			System.out.println("Files Remaining to Compare: " + totalfiles);
			System.out.println("-- COMPARE --");
			// start this in thread

			new Thread(new Runnable() {
				@Override
				public void run() {
					fll.filecompare();
				}
			   }).start();

			// loop progress...
			System.out.println("     Processed/Total, Matches");

			while (!fll.getCompareCompleted()) { 
				try {
					//System.out.println("comparing... " + fll.getProcessed() + "/" + totalfiles + " " + fll.getMatches());
					System.out.println(fll.getStatus());
					Thread.sleep(1000); 
				} catch (InterruptedException ie) { ; }
			}

		//	fl.print();
			ArrayList<ArrayList<File>> allList = fll.getDuplicatelist();

			ArrayList<ArrayList<File>> currentList = allList;

			while (true) {
				
				System.out.print("" + currentList.size() + "> ");	
				String argument = readln();

				// going to need a command parser class...
				String[] arguments = argument.split(":");
				if (arguments.length == 1) {
					if ("help".equals(arguments[0]) || "?".equals(arguments[0]) || "h".equals(arguments[0]) )
					{
						System.out.println("This tool works by providing a list of files to delete.");
						System.out.println("This list can be progressively refined with the following commands:");
						System.out.println("");
						System.out.println(":: - Show Predicate Commands");

						System.out.println("all (a) - ");
						System.out.println("compare (c) - ");
						System.out.println("delete - delete selection and reset");
						System.out.println("filter (f) - select file from duplicate groups which match predicate");

						System.out.println("group (g) - select duplicate groups which match predicate");
						System.out.println("ignore (i) - ignore duplicate groups which match predicate");
						System.out.println("keep (k) - keep files (remove from deletion) matching the filter predicate");


						System.out.println("parent - show parent of current selection");
						System.out.println("print - show current selection");
						System.out.println("quit (q) - ");

						System.out.println("reset (r) - reset selection");


					} 
					else if ("reset".equals(arguments[0]) ||  "r".equals(arguments[0]) )
					{
						currentList = allList;
					} else if ("delete".equals(arguments[0])) {
						// check current delete list with alllist
						ArrayList<ArrayList<File>> newList = new ArrayList<ArrayList<File>>();
						for (ArrayList<File> al : allList)
						{
							//System.out.println("Checking Group for deletion: ");
							ArrayList<File> deleteList = findEntry(al,currentList);
							if (deleteList != null && deleteList.size() < al.size())
							{
								for (File f: deleteList)
								{
									try {
										Files.delete(f.toPath());
										System.out.println("Deleted the file: " + f.getCanonicalPath());
									} catch (Exception e) {
										System.out.println(e.getMessage());
									}
									// System.out.println("Deleting... " + f.getCanonicalPath());
									//if(f.delete()) {
									//	System.out.println("Deleted the file: " + f.getCanonicalPath());
									//  } else {
									//	System.out.println("Failed to delete: " + f.getCanonicalPath());
									//  } 
								}
								if(al.size() - deleteList.size() >= 1 )
								{
									ArrayList<File> nal = new ArrayList<File>();
									// add remaining duplicates
									for (File f: al)
									{
										boolean found = false;
										for (File delf: deleteList)
											if (delf.equals(f))
												found = true;
												
										if (!found)
											nal.add(f);
									}
									newList.add(nal);
								}
							} else {
								if (deleteList != null) {
									System.out.print("WARNING not deleting all: "  );
									printLine(deleteList);
								}
								newList.add(al);
							}
						}
						allList = newList;
						currentList = newList;
					} else if ("compare".equals(arguments[0]) ||  "c".equals(arguments[0])) {
						for (ArrayList<File> al : allList)
						{
							ArrayList<File> deleteList = findEntry(al,currentList);
							if (deleteList != null)
							{
								if (al.size() == deleteList.size())
									System.out.println("-- ALL --");
								else
									System.out.println("--");
								printLine(al);
								printLine(deleteList);
							}
						}
					} else if ("all".equals(arguments[0]) ||  "a".equals(arguments[0])) {
						for (ArrayList<File> al : allList)
						{
							ArrayList<File> deleteList = findEntry(al,currentList);
							if (deleteList != null)
							{
								if (al.size() == deleteList.size())
								printLine(al);
							}
						}
					} else if ("parent".equals(arguments[0])) {
							printParent(currentList);
					} else if ("print".equals(arguments[0])) {
							print(currentList);	
					} else if ("quit".equals(arguments[0]) ||  "q".equals(arguments[0])) {
						System.exit(0);
					}
				} else if (arguments.length == 2) {
					if ("keep".equals(arguments[0]) || "k".equals(arguments[0])) {

						String filterCommand = arguments[1];
						// Handle optional third argument for rules that need it
						String filterArgument = (arguments.length > 2) ? arguments[2] : "";
					
						// A single, clean call to the engine.
						currentList = rulelist.evalKeep(currentList, filterCommand, filterArgument);
					} else  {
						rulelist.printHelp();
					}
				} else if (arguments.length == 3) {
					if ("group".equals(arguments[0]) || "g".equals(arguments[0])) {
						currentList = rulelist.evalGroup(currentList, arguments[1],arguments[2]);
					} else if ("ignore".equals(arguments[0]) || "i".equals(arguments[0])) {
							currentList = rulelist.evalIgnore(currentList, arguments[1],arguments[2]);
					} else if ("filter".equals(arguments[0]) || "f".equals(arguments[0])) {
						// remember to check that we're not deleting all duplicates
						currentList = rulelist.evalFilter(currentList, arguments[1], arguments[2]);
					} else if ("keep".equals(arguments[0]) || "k".equals(arguments[0])) {

						String filterCommand = arguments[1];
						// Handle optional third argument for rules that need it
						String filterArgument = (arguments.length > 2) ? arguments[2] : "";
					
						// A single, clean call to the engine.
						currentList = rulelist.evalKeep(currentList, filterCommand, filterArgument);
					} else {
						rulelist.printHelp();
					}
 				} else {
					rulelist.printHelp();
				}

			}

		} catch (Exception e) {
			System.out.println ("An error occurred: " + e );
		}
	}
}
