package uk.ac.ucl.cs.mr.util;

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.trees.*;
import java.io.*;
import java.util.*;

import org.json.*;


public class Text2Parsed2JSON {

	// this holds the main pipeline for the processing 
	private StanfordCoreNLP mainPipeline;
	
    public static String readTextFromFile(File textFileName) throws IOException {
    	BufferedReader textFile = new BufferedReader(new FileReader(textFileName));
    	String line;
    	StringBuffer result = new StringBuffer();
    	while ((line = textFile.readLine() ) != null){
    		result.append(line);
    	}
    	textFile.close();
    	return result.toString();
    }
    
    public Text2Parsed2JSON(){
		// Initialize the parser:
		Properties parser_props = new Properties();
		parser_props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		parser_props.put("tokenize.whitespace", "true");
		parser_props.put("ssplit.isOneSentence", "true");
		mainPipeline = new StanfordCoreNLP(parser_props);		
    	
    }
    
    // this takes text, runs the main processor and returns the result 
    public JSONArray processText(String text){
    	// create an empty Annotation just with the given text
        Annotation annotatedText = new Annotation(text);
        
        // TODO: what about the filtering?
        // run all Annotators on this text
        mainPipeline.annotate(annotatedText);    	
    	
        // TODO: convert this into a sensible JSON form
        // TODO: separate function, just from Stanford to JSON?
    	JSONArray result = null;
    	return result;
    }

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		// get the directory with the text files
		File extractsDirectory = new File(args[0]);

		// get the output directory
		File outputDirectory = new File(args[1]);
		outputDirectory.mkdir();

		// TODO: check from here 
		// get a list of files:
		File[] textFileNames = extractsDirectory.listFiles();

		// For each text file:
		for (int i = 0; i < textFileNames.length; i++){
			
			
			// First get the filename
			String filename = textFileNames[i].getName();
			// Read in the text
			System.out.println(textFileNames[i]);
			    
			// Create the file for the output
			File labeledFile = new File(outputDirectory, entityName + "_" + slotFiller);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(labeledFile), "UTF-8"));
			    
			String text = readTextFromFile(textFileNames[i]);
		
		}
	}

}
