import java.util.ArrayList;


public class CppFunc {

	private String type = "void";
	private String name = "";
	// private MetricsParam[] params;
	
	private ArrayList<MemberVariable> members = new ArrayList<MemberVariable>();
	
	private ArrayList<String> operators = new ArrayList<String>();
	private ArrayList<String> operands = new ArrayList<String>();
	private ArrayList<String> uniqueOperators = new ArrayList<String>();
	private ArrayList<String> uniqueOperands = new ArrayList<String>();
	
	// Cyclomatic complexity
	private int cyclomaticComplexity = 1;
	
	// Halstead-related metrics
	private int vocabulary = 0;
	private int length = 0;
	private double calculatedLength = 0.0;
	private double volume = 0.0;
	private double difficulty = 0.0;
	private double effort = 0.0;
	private double timeToProgram = 0.0;
	private double deliveredBugs = 0.0;
	
	public CppFunc(String type, String name)
	{
		this.type = type;
		this.name = name;
	}
	
	public String getType()
	{
		return type;
	}
	
	public String getName()
	{
		return name;
	}
	
	public int getCyclomaticComplexity()
	{
		return cyclomaticComplexity;
	}
	
	public void addMember(MemberVariable mv)
	{
		members.add(mv);
	}
	
	public ArrayList<MemberVariable> getMembers()
	{
		return members;
	}
	
	public MemberVariable getMember(String name)
	{
		for(MemberVariable mv : members)
		{
			if(mv.getName().equals(name)) return mv;
		}
		return null;
	}
	
	public boolean isMember(String name)
	{
		for(MemberVariable mv : members)
		{
			if(mv.getName().equals(name)) return true;
		}
		return false;
	}
	
	public void addOperator(String op)
	{
		
		boolean isUnique = true;
		for(String s : operators)
		{
			if(s.equals(op))
			{
				isUnique = false;
				break;
			}
		}
		operators.add(op);
		if(isUnique) uniqueOperators.add(op);
	}
	
	public void addOperand(String od)
	{
		
		boolean isUnique = true;
		for(String s : operands)
		{
			if(s.equals(od))
			{
				isUnique = false;
				break;
			}
		}
		operands.add(od);
		if(isUnique) uniqueOperands.add(od);
		
	}
	
	public int getOperatorCount()
	{
		return operators.size();
	}
	
	public int getOperandCount()
	{
		return operands.size();
	}
	
	public int getUniqueOperatorCount()
	{
		return uniqueOperators.size();
	}
	
	public int getUniqueOperandCount()
	{
		return uniqueOperands.size();
	}
	
	public int getVocabulary()
	{
		if(vocabulary == 0)
		{
			vocabulary = getUniqueOperatorCount() + getUniqueOperandCount();
		}
		return vocabulary;
	}
	
	public int getLength()
	{
		if(length == 0)
		{
			length = getOperatorCount() + getOperandCount();
		}
		return length;
	}
	
	public double getCalculatedLength()
	{
		if(calculatedLength == 0.0)
		{
			int uopcount = getUniqueOperatorCount();
			int uodcount = getUniqueOperandCount();
			calculatedLength = uopcount * (Math.log(uopcount) / Math.log(2)) + uodcount * (Math.log(uodcount) / Math.log(2));
		}
		return calculatedLength;
	}
	
	public double getVolume()
	{
		if(volume == 0.0)
		{
			int length = getLength();
			int vocab = getVocabulary();
			volume = (double)length * (Math.log((double)vocab) / Math.log(2));
		}
		return volume;
	}
	
	public double getDifficulty()
	{
		if(difficulty == 0.0)
		{
			difficulty = ((double)getUniqueOperatorCount() / 2.0) * ((double)getOperandCount() / (double)getUniqueOperandCount());
		}
		return difficulty;
	}
	
	public double getEffort()
	{
		if(effort == 0.0)
		{
			effort = getDifficulty() * getVolume();
		}
		return effort;
	}
	
	public double getTimeToProgram()
	{
		if(timeToProgram == 0.0)
		{
			timeToProgram = getEffort() / 18.0;
		}
		return timeToProgram;
	}
	
	public double getDeliveredBugs()
	{
		if(deliveredBugs == 0.0)
		{
			deliveredBugs = Math.pow(getEffort(), 2.0/3.0) / 3000.0;
		}
		return deliveredBugs;
	}
}