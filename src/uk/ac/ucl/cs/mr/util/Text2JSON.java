package uk.ac.ucl.cs.mr.util;

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.trees.*;
import java.io.*;
import java.util.*;
import java.lang.*;

public class Text2JSON {
	
	public static interface Filter { 
		// takes as argument a sentence parsed up to a point
	    public boolean goodToParse(CoreMap Sent);
	}
	
	public static class AlwaysTrue implements Filter { 
	    public boolean goodToParse(CoreMap Sent) {
	        System.out.println("Always true");
	        return true;
	    } 
	}
	
	public static class AtLeastOneLocationOneNumber implements Filter { 
	    public boolean goodToParse(CoreMap Sent) {
	        System.out.println("Always true");
	        return true;
	    } 
	}


    public static String readTextFromFile(File textFileName) throws IOException {
    	BufferedReader textFile = new BufferedReader(new FileReader(textFileName));
    	String line;
    	StringBuffer result = new StringBuffer();
    	while ((line = textFile.readLine() ) != null){
    		result.append(line);
    	}
    	return result.toString();
    }
    
    


	/**
	 * @param args
	 * the first argument is the directory with the text files 
	 * the second argument is the output directory for the jsons
	 * the third argument should be a filtering option (not everything should be parsed usually)
	 */
	public static void main(String[] args) {
		// get the directory with the text files
		File extractsDirectory = new File(args[0]);

		// get the output directory
		File outputDirectory = new File(args[1]);
		outputDirectory.mkdir();
		
		// check the filtering option
		Filter sentenceFilter;
		switch (args[2]){
		case "all": sentenceFilter = new AlwaysTrue();
		case "OneLocOneNumber": sentenceFilter = new AtLeastOneLocationOneNumber();
		}
		
		// TODO: separate function
		// Initialize the sentence splitter
		Properties ss_props = new Properties();
		ss_props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		StanfordCoreNLP ss_pipeline = new StanfordCoreNLP(ss_props);

		// TODO: separate function
		// Initialize the parser:
		Properties parser_props = new Properties();
		parser_props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		parser_props.put("tokenize.whitespace", "true");
		parser_props.put("ssplit.isOneSentence", "true");
		StanfordCoreNLP parser_pipeline = new StanfordCoreNLP(parser_props);		
		
		// TODO: From here onwards we need to restructure the code.
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
			    
			// Apply the cheap processing step
			List<CoreMap> sentences = text2Sentences(text, ss_pipeline);
			// Then filter
			// For each sentence
			    for(CoreMap sentence: sentences) {
				// Check whether it is a reasonable sentece
				if (isSentenceGood(sentence)) {
				    // Check whether it mentions the entity
				    ArrayList<ArrayList<Integer>> entityTokensSets = findEntityTokens(sentence, entityName);
				
				    if (entityTokensSets.size()>0){
					//System.out.println("Found the entity");
					List<CoreLabel> tokens_t = sentence.get(CoreAnnotations.TokensAnnotation.class);
					//System.out.println(Sentence.listToString(tokens_t));
					// Parse the sentence:
					Annotation parsed_sentences = new Annotation(Sentence.listToString(tokens_t));
					parser_pipeline.annotate(parsed_sentences);
					// The document is just one sentence:
					// If not avoid further processing
					if (parsed_sentences.get(CoreAnnotations.SentencesAnnotation.class).size() == 1) {	
					    CoreMap parsed_sentence = parsed_sentences.get(CoreAnnotations.SentencesAnnotation.class).get(0);
					    
					    List<CoreLabel> tokens = parsed_sentence.get(CoreAnnotations.TokensAnnotation.class);
					    //System.out.println(Sentence.listToString(tokens));				

					    // HACK: redo the entity token search, since parsing messes things up a bit...
					    entityTokensSets = findEntityTokens(parsed_sentence, entityName);
					    
					    for (Iterator<ArrayList<Integer>> entityTokensIter = entityTokensSets.iterator(); entityTokensIter.hasNext(); ){
						ArrayList<Integer> entityTokens = entityTokensIter.next();
						String entityTokenArray = entityTokens.toString();
						//System.out.println(entityTokenArray);
						// Get the tokens, the lemmas and the PoS 
						StringBuffer wordBuf = new StringBuffer();
						StringBuffer posBuf = new StringBuffer();
						StringBuffer lemmaBuf = new StringBuffer();
						for (CoreLabel token: parsed_sentence.get(CoreAnnotations.TokensAnnotation.class)) {
						    String word = token.get(CoreAnnotations.TextAnnotation.class);
						    wordBuf.append(word.replaceAll("\\s","") + " ");
						    // this is the POS tag of the token
						    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
						    posBuf.append(pos.replaceAll("\\s","") + " ");
						    String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
						    lemmaBuf.append(lemma.replaceAll("\\s","") + " ");			    
						}
						String wordStr = wordBuf.toString().trim();
						String posStr = posBuf.toString().trim();
						String lemmaStr = lemmaBuf.toString().trim();
						
						// This is to get the the parse tree:
						//Tree tree = parsed_sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
						// this is the Stanford dependency graph of the current sentence
						SemanticGraph dependencies = parsed_sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
						// Look for candidates slot fillers
						ArrayList<ArrayList<Integer>> slotTokensSets = null;
						if(slotFillerType.equals("number")){
						    slotTokensSets = getAllNumberTokens(parsed_sentence);
						}
						if(slotFillerType.equals("name")){
						    slotTokensSets = getAllNNPTokenSequences(parsed_sentence, entityTokens);
						}
						
						// for each of the candidate slot fillers check if it should be a positive or a negative example.
						for (Iterator<ArrayList<Integer>> slotTokensIter = slotTokensSets.iterator(); slotTokensIter.hasNext(); ){
						    ArrayList<Integer> slotTokens = slotTokensIter.next();
						    String slotTokenArray = slotTokens.toString();
						    boolean label = false;
						    // if we are labeling the data
						    //System.out.println();
						    //System.out.println(wordStr);
						    //System.out.println(slotTokenArray);
						    if (labelData){
							
							if (slotFillerType.equals("number")){
							    // if we are doing numbers, then there is only one token:
							    String tokenStr = tokens.get(slotTokens.get(0)).get(CoreAnnotations.TextAnnotation.class);
							    label = (tokenStr.equals(slotFiller));
							}
							
							if(slotFillerType.equals("name")){
							    String upperCaseStr = new String();
							    for (Iterator<Integer> slotTokenIter = slotTokens.iterator(); slotTokenIter.hasNext(); ){
								upperCaseStr += tokens.get(slotTokenIter.next()).get(CoreAnnotations.TextAnnotation.class) + " ";
							    }
							    upperCaseStr.trim();
							    label = (matchNameWithString(slotFiller, upperCaseStr));
							}
						    }
						    //else{
						    //label = false;
						    //}
						
						    // Create labeled instances for each of the slot fillers
						    out.write((new Boolean(label)).toString() + "\t" + entityTokenArray + "\t" + slotTokenArray  + "\t" + wordStr + "\t" + filename.replace("|","/")  +"\n");
						    out.write(lemmaStr + "\n");
						    out.write(posStr + "\n");
						    if (dependencies != null){
							out.write(dependencies.toString("plain"));
						    }
						    out.newLine();
						}
					    }
					}
				    }
				}
			    }
			}
		    out.close();
		    }
		}
	}

}
