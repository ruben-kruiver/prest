package cppParser;

import java.util.ArrayList;
import java.util.HashSet;

import cppStructures.*;
import cppParser.utils.VarFinder;
import java.util.List;

/**
 * This class is responsible of analyzing and constructing functions found in the source code.
 * 
 * @author Harri Pellikka
 */
public class FunctionAnalyzer extends Analyzer {
	
	// Keywords that increment the cyclomatic complexity
	private static final String[] inFuncCCKeywords = {"for", "while", "if", "?", "case", "&&", "||", "#ifdef"};
	// private static final String[] inFuncHalsteadOps = {"::", ";", "+", "-", "*", "/", "%", ".", "<<", ">>", "<", "<=", ">", ">=", "!=", "==", "=", "&", "|"};
	
	// Operands found from the sentence that is currently being processed
	private ArrayList<String> currentOperands = null;
	private ArrayList<String> currentOperators = null;
	private HashSet<Integer> handledOperatorIndices = new HashSet<Integer>();
	private HashSet<Integer> handledOperandIndices = new HashSet<Integer>();
	
	// Iteration index for the current sentence
	private int i = -1;
	
	// Tokens for the current sentence
	private String[] tokens = null;
	
	// The function currently under analysis
	private CppFunc func = null;
        
    //Helper class for finding variables
    private VarFinder varFinder = new VarFinder(this);
	
	/**
	 * Constructs a new function analyzer
	 * @param sa The sentence analyzer
	 */
	public FunctionAnalyzer(SentenceAnalyzer sa)
	{
		super(sa);
	}
	
	private String getScope(String[] tokens, int i)
	{
		for(int j = 1; j < i - 1; ++j)
		{
			if(tokens[j].equals("::"))
			{
				return tokens[j-1];
			}
		}
		
		// No scope was found from the tokens, return the currentScope from ParsedObjectManager
		if(ParsedObjectManager.getInstance().currentScope != null)
		{
			return ParsedObjectManager.getInstance().currentScope.getName();
		}
		
		return null;
	}
	
	/**
	 * Analyses the list of tokens to find out whether or not
	 * the tokens form a new function.
	 * @param tokens Tokens that form the sentence
	 * @return 'true' if a new function was found, 'false' otherwise
	 */
	private boolean processNewFunction(String[] tokens)
	{
		// Bail out instantly if there's no body starting
		if(!tokens[tokens.length - 1].equals("{")) return false;
		
		for(int i = 1; i < tokens.length; ++i)
		{
			if(tokens[i].equals("("))
			{
				Log.d("   FUNCTION " + tokens[i-1] + " START (file: " + Extractor.currentFile + " | line: " + Extractor.lineno + ")");
				
				// Get scope
				String scope = getScope(tokens, i);
				if(scope == null) return false; // TODO Fix this
				sentenceAnalyzer.setCurrentScope(scope, false);
				
				String funcName = tokens[i-1];

				String returnType = "";

				// Parse the type backwards
				if(i == 1)
				{
					returnType = "ctor";
					if(funcName.startsWith("~")) returnType = "dtor";
				}
				else
				{
					if(i == 1 && !tokens[0].contains("protected") && !tokens[0].contains("private")) 
						returnType = tokens[0];
					else if(i != 2) 
							returnType = tokens[i-2];
						else returnType = funcName;
					
					if(returnType.equals(tokens[i-1]) && i == 1)

					for(int j = i - 2; j >= 0; --j)

					{
						if(tokens[j].equals(":") || StringTools.isKeyword(tokens[j]))
						{
							break;
						}
						returnType = tokens[j] + (returnType.length() > 0 ? " " : "") + returnType;
					}
				}
				
				if(returnType == "")
				{
					returnType = "ctor";
					if(tokens[i-1].startsWith("~")) returnType = "dtor";
				}
				
				CppFunc func = new CppFunc(returnType, funcName);
				
				// Parse parameters
				if(!tokens[i+1].equals(")"))
				{
					String paramType = "";
					String paramName = "";
					for(int j = i + 1; j < tokens.length - 1; ++j)
					{
						if(tokens[j].equals(")")) break;
						
						if(tokens[j].equals(","))
						{
							CppFuncParam attrib = new CppFuncParam(paramType, paramName);
							func.parameters.add(attrib);
							paramType = "";
							paramName = "";
						}
						else
						{
							if(tokens[j+1].equals(",") || tokens[j+1].equals(")"))
							{
								paramName = tokens[j];
							}
							else
							{
								paramType += (paramType.length() > 0 ? " " : "") + tokens[j];
							}
						}
					}
					
					if(!paramType.equals("") && !paramName.equals(""))
					{
						CppFuncParam attrib = new CppFuncParam(paramType, paramName);
						func.parameters.add(attrib);
					}
				}
				
				ParsedObjectManager.getInstance().currentFunc = func;
				ParsedObjectManager.getInstance().currentScope.addFunc(ParsedObjectManager.getInstance().currentFunc);
				ParsedObjectManager.getInstance().currentFunc.funcBraceCount = sentenceAnalyzer.braceCount;
				ParsedObjectManager.getInstance().currentFunc.fileOfFunc = Extractor.currentFile;
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Processes sentences that belong to a currently open function
	 * @param tokens Tokens that form the sentence to process
	 * @return 'true' if the sentence was processed, 'false' if not
	 */
	private boolean processCurrentFunction(String[] tokens)
	{
		varFinder.clearHandledIndices();
                varFinder.findVariables(tokens);
        
		currentOperands = new ArrayList<String>();
		currentOperators = new ArrayList<String>();
		handledOperatorIndices.clear();
		handledOperandIndices.clear();
		this.i = 0;
		this.tokens = tokens;
		
		// Pull the currentFunc to a local variable for fast and easy access
		func = ParsedObjectManager.getInstance().currentFunc;
		
		for(i = 0; i < tokens.length; ++i)
		{
			// Check for cyclomatic complexity
			checkForCC();
			
			// Check for halstead complexity operators
			checkOpsAndOds();			
			
		}
		/*
		Log.d("        Operands:");
		for(String s : currentOperands)
		{
			Log.d("         - " + s);
		}
		
		Log.d("        Operators:");
		for(String s : currentOperators)
		{
			Log.d("         - " + s);
		}
		*/		
		// Log.d();
		
		// Finally, set varFinder's originalTokens to null
		varFinder.setOriginalTokens(null);
		
		return true;
	}
	
	
	/**
	 * Analyzes a function call.
	 * This method is recursive, meaning that if it finds another function call
	 * in the parameters, it will call itself to parse the inner function call.
	 * @param index The index in tokens if the opening parenthesis
	 * @return The index of the closing parenthesis
	 */
	private int handleFunctionCall(int index)
	{
		// Store the function name
		String funcName = tokens[index-1]; //Array out of bounds if first token is "(" ?
		
		//ParsedObjectManager.getInstance().currentFunc.addOperand(funcName);
		//ParsedObjectManager.getInstance().currentFunc.addOperator(tokens[index]);
		
		// Check if the function call is parameterless
		if(tokens[index+1].equals(")"))
		{
                    if(varFinder.isDefined(funcName)){
                        Log.d(funcName+" is known variable, not function call...");
                    }else{
			Log.d("      (line: " + Extractor.lineno + ") Function call np > " + funcName);
			func.recognizedLines.add("      (line: " + Extractor.lineno + ") Function call > " + funcName);
                    }
			return index + 1;
		}
                // Owners List should contain the owners of the function call eg myObj in "myObj->hello();"
		List<String> owners=new ArrayList<>(); 
		List<List<String>> params = new ArrayList<>();
		List<String> currentParam = new ArrayList<>();
		boolean even;
                int skipped=0;
                for(int j= 2; index-j >= 0;j++){
                    if(tokens[index-j].contentEquals("*"))
                        skipped++;
                    else{
                        if((j+skipped)%2==0)
                            even=true;
                        else even=false;
                        if(even){
                            switch(tokens[index-j]){
                                case "->":
                                case ".":
                                    owners.add(0, tokens[index-j]);
                                    break;
                                case "::":
                                    Log.d("Line:"+Extractor.lineno+ " contains :: when . or -> was expected");
                                    break;
                                default:
                                    break;
                            }
                        }else{
                            owners.add(0, tokens[index-j]);
                        }
                    }
                }
                if(!owners.isEmpty()){
                    String str="";
                    for(String s:owners)
                        str+=s;
                    Log.d("Owner"+str);
                }
                
                // Loop through the parameters
		for(int j = index + 1; j < tokens.length; ++j)
		{
			switch(tokens[j])
			{
			case ")":
				// Close the function call
				if(!currentParam.isEmpty())
				{
					params.add(currentParam);
					//handleParameter(currentParam);
				}
                                if(varFinder.isDefined(funcName)){
                                    Log.d(funcName+" is known variable, not function call...");
                                }else{
                                    Log.d("      (line: " + Extractor.lineno + ") Function call > " + funcName);
                                    func.recognizedLines.add("      (line: " + Extractor.lineno + ") Function call > " + funcName);
                                }
				return j;
			case "(":
				// Recurse through inner function calls
				// j = handleFunctionCall(j);
				j = handleOpeningParenthesis(j);
				break;
			case ",":
				params.add(currentParam);
				//handleParameter(currentParam);
				currentParam = new ArrayList<>();
				break;
			default:
				currentParam.add(tokens[j]);
				break;
			}
			
		}
		
		
		// This should never happen, but if it does, this is the last one that was checked
		return tokens.length - 1;
	}
	
	/**
	 * Disseminates a parameter to see if it includes operators or operands
	 */
	private void handleParameter(String p)
	{
		String[] pTokens = StringTools.split(p, null, true);
	}
	
	/**
	 * Analyzes an opening parenthesis to find out what it means
	 * (function call, cast, ordering...) and calls an appropriate
	 * handling function.
	 * 
	 * Note that this function is a part of a recursive call chain.
	 * 
	 * @param index The index of the opening parenthesis
	 * @return The index of the closing parenthesis
	 */
	private int handleOpeningParenthesis(int index)
	{
		@SuppressWarnings("unused")
		int origIndex = index;
		
		if(index < 1) return index;
		Log.d("hop:"+tokens[index-1]);
		// Check the token before the opening parenthesis
		switch(tokens[index-1])
		{
		case "for":
			// Log.d("      (line: " + Extractor.lineno + ") for-statement");
			func.recognizedLines.add("      (line: " + Extractor.lineno + ") for-statement");
			break;
		case "while":
			// Log.d("      (line: " + Extractor.lineno + ") while-statement");
			func.recognizedLines.add("      (line: " + Extractor.lineno + ") while-statement");
			break;
		case "if":
			// Log.d("      (line: " + Extractor.lineno + ") if-statement");
			func.recognizedLines.add("      (line: " + Extractor.lineno + ") if-statement");
			break;
		default:
			// TODO Change from 'default' case to actual function handling case
			//index = handleFunctionCall(index);
			break;
		}
		
		//ParsedObjectManager.getInstance().currentFunc.addOperator(tokens[index-1]);		//operator at before index
		ParsedObjectManager.getInstance().currentFunc.addOperand(tokens[index]);		//operand at index
		
		return index;
	}
	
	/**
	 * Checks if the operand at index 'index' isn't added already
	 * by VarFinder.
	 * @param index The index of the operand
	 * @return 'true' if the operand isn't yet added, 'false' if it is already added
	 */
	private boolean canAddOperand(int index)
	{
		for(Integer integer : varFinder.getHandledIndices())
		{
			if(integer.intValue() == i - 1)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Constructs an operator from the token at the current index
	 * and stores the related operand, if the operator is a unary operator (!, ++ or --).
	 * 
	 * The main purpose for this method is to check if the token
	 * is part of a multi-token operator, for example when "<" is found,
	 * this method checks if there is a "=" following it.
	 * 
	 * @return String representation of the operator
	 */
	private String constructOperator()
	{
		String op = tokens[i];
		
		switch(op)
		{
		case "+":
		case "-":
			if(tokens[i-1].equals(op))
			{
				op = op + op;
				op += " PRE";
				if(canAddOperand(i+1)) objManager.currentFunc.addOperand(tokens[i+1]);
				i++;
			}
			else if(tokens[i+1].equals(op))
			{
				op = op + op;
				op += " POST";
				if(canAddOperand(i-1)) objManager.currentFunc.addOperand(tokens[i-1]);
				i++;
			}
			break;
		case "=":
			if(tokens[i-1].equals(op))
			{
				op = op + op;
				
			}
			else if(tokens[i+1].equals(op))
			{
				op = op + op;
				i++;
			}
			else switch(tokens[i-1])
			{
			case "<":
			case ">":
				op = tokens[i-1] + op;
				
				break;
			}
			break;
		case "<":
		case ">":
			if(tokens[i+1].equals("="))
			{
				op += "=";
				i++;
			}
			else if(tokens[i+1].equals(op) || tokens[i-1].equals(op))
			{
				op = op + op;
				i++;
			}
			break;
		}
		
		return op;
	}
	
	/**
	 * Constructs a string literal from the given index onwards until the closing '"' is found.
	 * @param index The index of the beginning '"'
	 * @param reverse If 'true', the search is done backwards (the current index is the ending '"')
	 * @return The string literal
	 */
	private String constructStringLiteral(int index, boolean reverse)
	{
		String literal = "";
		if(!reverse)
		{
			
			for(int j = index; j < tokens.length; ++j)
			{
				literal += tokens[j];
				if(j != index && tokens[j].equals("\""))
				{
					i = j;
					break;
				}
			}
		}
		else
		{
			for(int j = index; j >= 0; --j)
			{
				literal = tokens[j] + literal;
				if(j != index && tokens[j].equals("\""))
				{
					i = index ;
					break;
				}
			}
		}
		if(!literal.startsWith("\"") || !literal.endsWith("\"")) return null;
		return literal;
	}
	
	/**
	 * When an operator is found, this method handles the line,
	 * searching for operators and operands in the sentence.
	 */
	private void handleOperator()
	{
		if(i < 1) return;
		
		int origIndex = i;
		String op = constructOperator();
		
		objManager.currentFunc.addOperator(op);
		
		if(!op.startsWith("++") && !op.startsWith("--"))
		{
			String leftSide = tokens[origIndex-1];
			// Log.d("Leftside: " + leftSide);
			
			if(leftSide.equals("\""))
			{
				leftSide = constructStringLiteral(origIndex-i, true);
			}
			
			// Add the leftside operand
			if(!StringTools.isOperator(leftSide))
			{
				if(leftSide != null && canAddOperand(i-1)) 
				{
					objManager.currentFunc.addOperand(leftSide);
				}
			}
		}
		
		// Process the rest of the tokens
		for(i = i + 1; i < tokens.length; ++i)
		{
			if(StringTools.isOperator(tokens[i]))
			{
				//Log.d("Found operator: " + tokens[i]);
				op = tokens[i];
				if(tokens[i-1].equals(op) || tokens[i+1].equals(op))
				{
					op = op + op;
				}
				objManager.currentFunc.addOperator(op);
			}
			else
			{
				//Log.d("Found something else: " + tokens[i]);
				if(tokens[i].equals(";")) continue;
				
				if(StringTools.isOperator(tokens[i-1]))
				{
					// TODO Construct a whole operand, if it consists of multiple tokens
					String operand = tokens[i];
					if(operand.equals("\"")) operand = constructStringLiteral(i, false);
					if(operand != null && canAddOperand(i))
					{
						objManager.currentFunc.addOperand(operand);
					}
					
				}
			}
		}
	}
	
	private boolean openString = false;
	
	private void checkOpsAndOds()
	{
            Log.d("coao "+tokens[i]);
		// Early bail out on tokens that are too long to be delimiters
		if(tokens[i].length() > 2) return;
		
		if(tokens[i].equals("\"")) openString = !openString;
		
		if(!openString && StringTools.isOperator(tokens[i]))
		{
			handleOperator();
		}
                else if(tokens[i].equals("("))
		{
			handleOpeningParenthesis(i);
		}
		
		/*
		if(tokens[i].startsWith("++") || tokens[i].startsWith("--"))
		{
			String op = tokens[i];			
			
			if(op.length() > 2)
			{
				op = op.substring(0, 3);
			}
			
			addOperator(i, op);
			addOperand(i, tokens[i].substring(2));
			
			return;
		}
		else if(tokens[i].endsWith("++") || tokens[i].endsWith("--"))
		{
			addOperator(i, tokens[i].substring(tokens[i].length() - 2));
			addOperand(i, tokens[i].substring(0, tokens[i].indexOf(tokens[i].charAt(tokens[i].length() - 1))));
			Log.d("        Op: ++ or -- (post)");
			
			return;
		}
		
		if(tokens[i].contains("->"))
		{
			// TODO Handle pointer operator
			//ParsedObjectManager.getInstance().currentFunc.addOperator(tokens[i]);
		}
		else if(tokens[i].equals("("))
		{
			i = handleOpeningParenthesis(i);
		}
		
		// Check for a string literal
		else if(tokens[i].startsWith("\""))
		{
			String stringLiteral = tokens[i] + " ";
			if(tokens[i].endsWith("\""))
			{
				stringLiteral = stringLiteral.substring(0, stringLiteral.length());
			}
			else
			{
				for(int j = i + 1; j < tokens.length; ++j)
				{
					if(tokens[j].endsWith("\""))
					{
						stringLiteral += tokens[j].substring(0, tokens[j].length());
						break;
					}
					else
					{
						stringLiteral += tokens[j] + " ";
						i++;
					}					
				}
			}
			addOperand(i, stringLiteral);
			
		}
		*/
		
		// Check for operators
		/*
		if(tokens[i].length() < 3)
		{
			for(int j = 0; j < inFuncHalsteadOps.length; ++j)
			{
				if(inFuncHalsteadOps[j].equals(tokens[i]))
				{
					// Add the operator
					String op = tokens[i];
					if(tokens[i].equals("+") || tokens[i].equals("-"))
					{
						if(i > 0 && tokens[i-1].equals(tokens[i])) return;
						if(i < tokens.length - 1 && tokens[i+1].equals(tokens[i]))
						{
							op += tokens[i+1];
						}
					}
					
					// func.addOperator(op);
					addOperator(i, op);
					
					// Check for operand(s)
					checkForOperands(op);
				}
			}
		}
		*/
	}
	
	/**
	 * Adds an operand to the list of operands in the current line
	 * @param i Index of the operand in the token list
	 * @param t The operand token
	 */
	private void addOperand(int i, String t)
	{
		Integer integer = new Integer(i);
		if(!handledOperandIndices.contains(integer))
		{
			handledOperandIndices.add(integer);
			currentOperands.add(t);
		}
		else
		{
			Log.d("TRIED TO ADD AN OPERAND OF AN EXISTING INDEX");
		}
	}
	
	/**
	 * Adds an operator to the list of operators in the current line
	 * @param i Index of the operator in the token list
	 * @param t The operator token
	 */
	private void addOperator(int i , String t)
	{
		Integer integer = new Integer(i);
		if(!handledOperatorIndices.contains(integer))
		{
			handledOperatorIndices.add(integer);
			currentOperators.add(t);
		}
		else
		{
			Log.d("TRIED TO ADD AN OPERATOR OF AN EXISTING INDEX");
		}
	}
	
	/*
	private void checkForOperands(String op)
	{
		switch(op)
		{
		case "=":
		case "!=":
		case "==":
		case "&&":
		case "<=":
		case "<":
		case ">":
		case ">=":
			addOperand(i-1, tokens[i-1]);
			addOperand(i+1, tokens[i+1]);			
			break;
		case "::":
			if(i < tokens.length - 3)
			{
				if(tokens[i+3].equals(";"))
				{
					addOperand(i+2, tokens[i+2]);					
				}
				else if(tokens[i+2].equals("("))
				{
					addOperand(i+1, tokens[i+1]);					
				}
			}
			break;
		case ";":
			if(i > 0 && !tokens[i-1].equals(")"))
			{
				if(i > 1 && !tokens[i-2].equals("="))
				{
					addOperand(i-1, tokens[i-1]);					
				}
			}
			/*
			else
			{
				for(int j = i - 2; j > 0; --j)
				{
					if(tokens[j].equals("("))
					{
						if(tokens[j-1].contains("."))
						{
							addOperator(j-1, tokens[j-1]);
						}
						break;
					}
				}
			}
			
			break;
		}
		
		//ParsedObjectManager.getInstance().currentFunc.addOperator(op);
	}
	*/
	
	/**
	 * Checks if the given token should increase the function's cyclomatic complexity
	 * @param func The function under analysis
	 * @param tokens The tokens to inspect
	 * @param i The iterator position for tokens
	 */
	private void checkForCC()
	{
		for(int j = 0; j < inFuncCCKeywords.length; ++j)
		{
			if(tokens[i].equals(inFuncCCKeywords[j]))
			{
				// TODO: Check that && and || are only inside if clauses (or where they matter / change the path)
				func.incCC();
			}
		}
	}
	
	/**
	 * Decides whether or not the tokens should be interpreted as a possible
	 * new function or as a sentence in a currently open function
	 */
	public boolean processSentence(String[] tokens)
	{
		if(ParsedObjectManager.getInstance().currentFunc == null)
		{
			return processNewFunction(tokens);
		}
		else
		{
			return processCurrentFunction(tokens);
		}
	}
}
