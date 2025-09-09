
import java.io.*;
import java.util.*;
import java.nio.file.*;

import java.security.MessageDigest;

public class Filelist implements Runnable, Serializable {

	transient private HashMap<Long,HashSet<File>> flist;
	private ArrayList<ArrayList<File>> duplicatelist;
//	private File scanDir;
	private ArrayList<File>scanDirs;

	transient private ArrayList<File> toProcess;

	transient private int filesProcessed =0;
	transient private int matchesProcessed =0;
	transient private boolean scanCompleted = false;
	transient private boolean compareCompleted = false;


	public Filelist (File dir) { this(); addScanDir(dir); }
	public Filelist (String dir) { this(); addScanDir(dir); }

	public Filelist ()
	{
		scanDirs = new ArrayList<File>();
		flist = new HashMap<Long,HashSet<File>>();
		duplicatelist = new ArrayList<ArrayList<File>>();
	}

	//public void setScanDir (File dir) { scanDir = dir; }
//	public void setScanDir (String dir) { scanDir = new File(dir); }
//	public File getScanDir () { return scanDir; }

	public void addScanDir(File dir) { scanDirs.add(dir); }
	public void addScanDir(String dir) { scanDirs.add(new File(dir)); }

// these are needed for serializable...?
	public ArrayList<ArrayList<File>> getDuplicatelist() { return duplicatelist; }
	public void setDuplicatelist(ArrayList<ArrayList<File>> dl) { duplicatelist = dl; }

	private String calculateFileHash(File file) {
		try (InputStream is = Files.newInputStream(file.toPath())) {
			MessageDigest md5Digest = MessageDigest.getInstance("MD5");
	
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				md5Digest.update(buffer, 0, bytesRead);
			}
	
			byte[] digest = md5Digest.digest();
	
			// Convert byte array to a hex string to use as a Map key
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
	
		} catch (Exception e) {
			// Return a unique string for files that can't be read,
			// so they don't get grouped with anything else.
			return "ERROR_" + file.getAbsolutePath();
		}
	}
	

	public void print() 
	{
		for (ArrayList<File> al : duplicatelist)
		{
			boolean first = true;
			for (File fl : al)
			{
				if(!first)
					System.out.print(" : ");
				first=false;
				try {
					System.out.print(fl.getCanonicalPath());
				} catch (Exception e) { 
					System.out.print("*ERROR*");
				}

			}
			System.out.println("");

		}
	}


	// from  https://stackoverflow.com/questions/27379059/determine-if-two-files-store-the-same-content
	private boolean sameContent(File file1, File file2)  {
		return sameContent (file1.toPath(), file2.toPath());
	}

	private boolean sameContent(Path file1, Path file2)  {

		try {
			final long size = Files.size(file1);
			// zero length check
			if (size < 4096)
				return Arrays.equals(Files.readAllBytes(file1), Files.readAllBytes(file2));

			try (BufferedInputStream is1 = new BufferedInputStream (Files.newInputStream(file1));
				BufferedInputStream is2 = new BufferedInputStream(Files.newInputStream(file2))) {
        // Compare byte-by-byte.
        // Note that this can be sped up drastically by reading large chunks
        // (e.g. 16 KBs) but care must be taken as InputStream.read(byte[])
        // does not neccessarily read a whole array!
				int data;
				while ((data = is1.read()) != -1)
				    if (data != is2.read())
					return false;
			}

			return true;
		} catch (IOException e) {
			// if one file has a read error, it's probably not going to be the same as the other file
			// or it's best not to match it for other reasons.
			return false;
		}
	}


	// public int countfiles() { return flist.size(); }

	
	public int countfiles() 
	{
		int total = 0;
		for (Map.Entry<Long, HashSet<File>> fe : flist.entrySet()) 
			total += fe.getValue().size()>1? 1:0;
		return total;
	}

	public int getProcessed() { return filesProcessed; }
	public int getMatches() { return matchesProcessed; }
	public int getScanRemain() { 
		if (toProcess != null)
			return toProcess.size(); 
			return 0;
		}
	public boolean getScanCompleted() { return scanCompleted; }
	public boolean getCompareCompleted() { return compareCompleted; }

// compare a list of files
	public void filecompare() 
	{
		// some sort of readable progress
		filesProcessed = 0;
		compareCompleted = false;
		for (Map.Entry<Long, HashSet<File>> fe : flist.entrySet()) {
			ArrayList<File> samesize = new ArrayList<File>(fe.getValue());
			if (samesize.size() > 1) {
				filesProcessed ++;

				//System.err.println("files: " + samesize.size() + " bytes: " + fe.getKey());
				if (samesize.size() == 2)
				{
					if (sameContent(samesize.get(0),samesize.get(1)))
					{
						matchesProcessed ++;
						ArrayList<File> samelist = new ArrayList<File>();
						samelist.add(samesize.get(0));
						samelist.add(samesize.get(1));
						duplicatelist.add(samelist);
					}
				//	filesProcessed ++;
					//System.err.println("processed: " + filesProcessed); // tp be handled in a different thread
				} else {
					ArrayList<String>hashFiles = new ArrayList<String>();
					for(File f : samesize)
					{
						hashFiles.add(calculateFileHash(f));
					}

					boolean[] alreadyGrouped = new boolean[samesize.size()];

					for (int i =0; i < hashFiles.size(); i++)
					{
						//System.err.println("" + i + "/" + hashFiles.size() + " multicompare.");
						if (!alreadyGrouped[i])
						{
							ArrayList<File> samelist = new ArrayList<File>();
							samelist.add(samesize.get(i));

							for (int j=i+1; j < hashFiles.size(); j++)
							{
								if (!alreadyGrouped[j])
								{
									if (hashFiles.get(i).equals(hashFiles.get(j)))
									{
										if (sameContent(samesize.get(i),samesize.get(j)))
										{
											samelist.add(samesize.get(j));
										}
									}
								}
							}

							if (samelist.size() > 1)
								duplicatelist.add(samelist);
						}
					}

/* 
					ArrayList<File> duplicateFiles = new ArrayList<File>(samesize);
					while (duplicateFiles.size() > 0)
					{
						ArrayList<File> samelist = new ArrayList<File>();
						samelist.add(duplicateFiles.get(0));
						for (int j =1; j < duplicateFiles.size(); j++)
						{
							if (sameContent(duplicateFiles.get(0),duplicateFiles.get(j)))
							{
								matchesProcessed ++;
								samelist.add(duplicateFiles.get(j));
								duplicateFiles.remove(j);
							}
					//		System.err.println("processed: " + filesProcessed); // tp be handled in a different thread
						}
				//		filesProcessed ++;

						if (samelist.size() > 1)
							duplicatelist.add(samelist);

						duplicateFiles.remove(0);
					}
					*/
				}
			}	

		}
		compareCompleted = true;
	}

// create a list of file, index by filesize
	public void filescan()
	{
		scanCompleted = false;

		toProcess = new ArrayList<File>(scanDirs);
		
		// System.out.println("toProcess: " + toProcess.get(0));

		// toProcess.add(getScanDir());

	//System.out.println("size: " + toProcess.size() + " item[0]: " + toProcess.get(0) +"<");
	// System.out.println("filelist: " + toProcess.get(0).listFiles());
		while (toProcess.size() > 0) {
		//	System.out.println("Processing... " + toProcess.get(0));
		//System.out.println("toprocess: " + toProcess.size());
			File testist[] =  testist = toProcess.get(0).listFiles();  // why is this returning null..?
			if (testist != null) {
		//		System.out.println("container array type: " + testist);
		//		System.out.println("file list count: " + testist.length);
				for (int idx=0; idx<testist.length; ++idx) {
					File sourcefile = testist[idx];
					if (!Files.isSymbolicLink(sourcefile.toPath()))
					//System.out.println(sourcefile.getAbsolutePath());
					if (sourcefile.isFile()) {
						Long fsize = sourcefile.length();
						if (fsize > 0)  // use find -empty
						{
							if (flist.containsKey(fsize))
							{
								flist.get(fsize).add(sourcefile);
							} else {
								HashSet<File> hs = new HashSet<File>();
								hs.add(sourcefile);
								flist.put(fsize,hs);
							}
						}
					} else if ( sourcefile.isDirectory()) {
					      toProcess.add(sourcefile);
					} else {
						System.out.println ("Source is not a file or directory. " + sourcefile.getAbsolutePath());
					}
				}
			}
			toProcess.remove(0);

		}
		scanCompleted = true;

	}
	
	public void run() { 
		filescan(); 
		filecompare();		
	}
	

}
