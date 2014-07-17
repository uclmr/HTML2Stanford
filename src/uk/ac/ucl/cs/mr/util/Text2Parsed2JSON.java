package uk.ac.ucl.cs.mr.util;

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.*;

import java.io.*;
import java.util.*;

import com.google.gson.*;



public class Text2Parsed2JSON {
	
	public class MyToken{
		public String word;
		public String lemma;
		public String pos;
		public String ner;
	}
	
	public class MyDependency{
		public int head;
		public int dep;
		public String label;
		
	}
	
	public class MySentence{
		
		public List<MyToken> tokens;
		public List<MyDependency> dependencies;
		
		
	}

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
    
    // dummy function that returns the same text that was passed as input.
    // to be over-ridden to do more interesting things. might need to add to the initialization.
    private String filterText(String text){
    	return text;
    }
    
    public Text2Parsed2JSON(){
		// Initialize the parser:
		Properties parser_props = new Properties();
		parser_props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		parser_props.put("tokenize.whitespace", "true");
		parser_props.put("ssplit.isOneSentence", "true");
		mainPipeline = new StanfordCoreNLP(parser_props);		
    	
    }
    
    // this takes text, runs the main processor and returns the Stanford annotations
    // for the sentences kept 
    public Annotation processText2Annotations(String text){
    	// filter the text
    	String filteredText = filterText(text); 
    	// create an empty Annotation just with the given text
        Annotation annotatedText = new Annotation(filteredText);
                
        mainPipeline.annotate(annotatedText);

        return annotatedText;
    }

    // TODO: convert this into a sensible JSON form
    public JSONArray processAnnotations2JSON(Annotation annotatedText){
    	
    	Array JSONSentences = new Array();
    	
    	// get the sentences 
        List<CoreMap> sentences = annotatedText.get(SentencesAnnotation.class);
        
        for(CoreMap sentence: sentences) {
          // traversing the words in the current sentence
          // a CoreLabel is a CoreMap with additional token-specific methods
          for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
            // this is the text of the token
            String word = token.get(TextAnnotation.class);
            // this is the POS tag of the token
            String pos = token.get(PartOfSpeechAnnotation.class);
            // this is the NER label of the token
            String ne = token.get(NamedEntityTagAnnotation.class);       
          }


          // this is the Stanford dependency graph of the current sentence
          SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
        }
        
        
    	
    	
    	return JSONSentences;
    }

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// initialize
		Text2Parsed2JSON processor = new Text2Parsed2JSON();
		
		// get the directory with the text files
		File extractsDirectory = new File(args[0]);

		// get the output directory
		File outputDirectory = new File(args[1]);
		outputDirectory.mkdir();

		// get a list of files:
		File[] textFileNames = extractsDirectory.listFiles();

		// For each text file:
		for (int i = 0; i < textFileNames.length; i++){
			
			// First get the filename
			String filename = textFileNames[i].getName();
			System.out.println(textFileNames[i]);
			// Read in the text
			String text;
			try {
				text = readTextFromFile(textFileNames[i]);
				// process
				Annotation annotatedText = processor.processText2Annotations(text);
				JSONArray JSONsentences = processor.processAnnotations2JSON(annotatedText);
				    
				// Create the file for the output
				File JSONFile = new File(outputDirectory, textFileNames[i] + ".json");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			    

		
		}
	}

}
