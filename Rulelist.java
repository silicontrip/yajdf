
import java.util.ArrayList;
import java.util.HashMap;

import java.io.File;

public class Rulelist {


//	private ArrayList<ArrayList<File>> duplicatelist;

// RULE STRATEGY
	private HashMap<String,Stringcompare> compMap;
	private HashMap<String,Filepart> partMap;

	private HashMap<String,Rule> groupRuleMap;
	private HashMap<String,Filter> filterMap;

	public Rulelist () {


// still would like to automate this even more
		compMap = new HashMap<String,Stringcompare>();
		// E M C S
		compMap.put("Equals", new StringEquals()); 
		compMap.put("Matches", new StringMatches());
		compMap.put("Contains", new StringContains());
		compMap.put("StartsWith",  new StringStartsWith());

		compMap.put("e", new StringEquals()); 
		compMap.put("m", new StringMatches());
		compMap.put("c", new StringContains());
		compMap.put("s",  new StringStartsWith());

		partMap = new HashMap<String,Filepart>();
		// C N P
		partMap.put("Canonical" , new FilepartCanonical());
		partMap.put("Name",  new FilepartName());
		partMap.put("Parent" , new FilepartParent());

		partMap.put("c" , new FilepartCanonical());
		partMap.put("n",  new FilepartName());
		partMap.put("p" , new FilepartParent());

		groupRuleMap = new HashMap<String,Rule>();
		filterMap = new HashMap<String,Filter>();


		filterMap.put("newest", new FilterNewest());
		filterMap.put("oldest", new FilterOldest());
		filterMap.put("first", new FilterFirst());
		filterMap.put("last", new FilterLast());

		for (String partName: partMap.keySet())
		{
			for (String compName: compMap.keySet())
			{

				//N O A
				groupRuleMap.put ("Any" + partName + compName, new RuleAny(partMap.get(partName),compMap.get(compName)));
				groupRuleMap.put ("One" + partName + compName, new RuleOne(partMap.get(partName),compMap.get(compName)));
				groupRuleMap.put ("All" + partName + compName, new RuleAll(partMap.get(partName),compMap.get(compName)));

				groupRuleMap.put ("n" + partName + compName, new RuleAny(partMap.get(partName),compMap.get(compName)));
				groupRuleMap.put ("o" + partName + compName, new RuleOne(partMap.get(partName),compMap.get(compName)));
				groupRuleMap.put ("a" + partName + compName, new RuleAll(partMap.get(partName),compMap.get(compName)));

				//A O
				filterMap.put("Any" + partName + compName, new FilterAny(partMap.get(partName),compMap.get(compName)));
				filterMap.put("One" + partName + compName, new FilterOne(partMap.get(partName),compMap.get(compName)));
				filterMap.put("a" + partName + compName, new FilterAny(partMap.get(partName),compMap.get(compName)));
				filterMap.put("o" + partName + compName, new FilterOne(partMap.get(partName),compMap.get(compName)));
			}
		}
	}

	// maybe return a string instead.
	public void printHelp() 
	{
		System.out.println("Group filter commands");
		for (String k : groupRuleMap.keySet())
			System.out.println("group:"+k+":<argument>");

		System.out.println("File filter commands");
			for (String k : filterMap.keySet())
				System.out.println("filter:"+k+":<argument>");

	}

	public ArrayList<ArrayList<File>> evalGroup (ArrayList<ArrayList<File>> duplicatelist, String groupCommand, String groupArgument)
	{
		if (!groupRuleMap.containsKey(groupCommand))
			return duplicatelist;
		ArrayList<ArrayList<File>> ndl = new ArrayList<ArrayList<File>>();
		for (ArrayList<File> al : duplicatelist)
			if (groupRuleMap.get(groupCommand).eval(groupArgument,al))
				ndl.add(al);	
		return ndl;
	}

	public ArrayList<ArrayList<File>> evalIgnore (ArrayList<ArrayList<File>> duplicatelist, String groupCommand, String groupArgument)
	{
		if (!groupRuleMap.containsKey(groupCommand))
			return duplicatelist;
		ArrayList<ArrayList<File>> ndl = new ArrayList<ArrayList<File>>();
		for (ArrayList<File> al : duplicatelist)
			if (!groupRuleMap.get(groupCommand).eval(groupArgument,al))
				ndl.add(al);	
		return ndl;
	}


	public ArrayList<ArrayList<File>> evalFilter(ArrayList<ArrayList<File>> duplicatelist, String groupCommand, String groupArgument)
	{
		if (!filterMap.containsKey(groupCommand))
		return duplicatelist;
		ArrayList<ArrayList<File>> ndl = new ArrayList<ArrayList<File>>();
		for (ArrayList<File> al : duplicatelist)
		{
			ArrayList<File> nl = filterMap.get(groupCommand).eval(groupArgument,al);
			if (nl.size() > 0)
				ndl.add(nl);	
		}
		// need to check that this doesn't delete all files.
		return ndl;
	}

	public ArrayList<ArrayList<File>> evalKeep(ArrayList<ArrayList<File>> duplicatelist, String groupCommand, String groupArgument)
	{
		if (!filterMap.containsKey(groupCommand))
		return duplicatelist;
		ArrayList<ArrayList<File>> ndl = new ArrayList<ArrayList<File>>();
		for (ArrayList<File> al : duplicatelist)
		{
			ArrayList<File> nl = filterMap.get(groupCommand).eval(groupArgument,al);
			
			// al is the superset of nl.  We only want elements in al which are not in nl
			ArrayList<File> nal = new ArrayList<File>();

			for (File f : al)
			{
				if (!nl.contains(f))
					nal.add(f);
			}
			ndl.add(nal);
		}

		// need to check that this doesn't delete all files.
		return ndl;
	}

}
