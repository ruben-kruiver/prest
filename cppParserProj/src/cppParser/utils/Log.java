package cppParser.utils;

/**
 * Static logger class for writing verbose information
 * during the processing and for "easy silencing".
 * 
 * @author Harri Pellikka
 */
public class Log {

	// If 'true', no output is done
	public static boolean isSilent = false;
	
	/**
	 * An empty override of 'd'
	 */
	public static void d()
	{
		d("");
	}
	
	/**
	 * Prints out the given string
	 * @param s The string to print
	 */
	public static void d(String s)
	{
		if(!isSilent)
		{
			System.out.println(s);
		}
	}
	
	/**
	 * Prints out an array of strings
	 * @param a Array of strings
	 */
	public static void d(String[] a)
	{
		String s = "[";
		for(int i = 0; i < a.length; ++i)
		{
			s += a[i];
			if(i < a.length - 1) s += " ";
		}
		s += "]";
		d(s);
	}
}
