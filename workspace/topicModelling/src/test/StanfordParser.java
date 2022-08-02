package test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import com.google.common.io.Files;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
/** A simple corenlp example ripped directly from the Stanford CoreNLP website using text from wikinews. */

public class StanfordParser {
	static StanfordCoreNLP pipeline;

	public static void main(String[] args) throws IOException {
		init();
		ConvertRAWFileIntoTrainingFile rf = new ConvertRAWFileIntoTrainingFile();
		// read some text from the file..
		//File inputFile = new File("C:\\Users\\psharma\\Google Drive\\PhDOtago\\Code\\workspaces\\fifth\\malletTest\\src\\test\\sample.txt");
		//String text = Files.toString(inputFile, Charset.forName("UTF-8"));
		
		String labelsFilename	= "C:\\Users\\psharma\\Google Drive\\PhDOtago\\Code\\inputFiles\\reason\\input-ReasonSentences.txt";	//exactly same as the one in google drive, but modified as well		
		String inputFilename	= "C:\\Users\\psharma\\Google Drive\\PhDOtago\\Code\\inputFiles\\reason\\input.txt";
		String text = "Based on the favorable feedback, Guido has accepted the PEP for Py2 .4.";
		boolean includeTermOnly = true, includeTerm = false, includePOS =false;
		
		try {
//$$			String temp  = rf.readLabelsFromFile(inputFilename,true); //labelsFilename,true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// create an empty Annotation just with the given text
		parseSentence(text,includeTermOnly,includeTerm, includePOS);

	}

	public static void init() {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		pipeline = new StanfordCoreNLP(props);
	}

	//returns an arraylist of terms and pos
	//can be called from another class
	public static ArrayList<String> parseSentence(String text,boolean includeTermOnly, boolean includeTerm, boolean includePOS) {
		ArrayList<String> list=new ArrayList<String>();//Creating arraylist  
		
		Annotation document = new Annotation(text);
		// run all Annotators on this text
		pipeline.annotate(document);
		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
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

//$$				System.out.println("word: " + word + " pos: " + pos + " ne:" + ne);
				if (includeTermOnly) {
					list.add(word);
				}
				
				else if(includeTerm) {
					//System.out.println(word + " " + pos + " ");
					list.add(word + " " + pos);
				}
				else if(includePOS) {
					//System.out.println(word + " " + pos + " ");
					list.add(pos);
				}
				
			}
			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class);
//$$			System.out.println("parse tree:\n" + tree);
			// this is the Stanford dependency graph of the current sentence
			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
//$$			System.out.println("dependency graph:\n" + dependencies);
		}
		// This is the coreference link graph
		// Each chain stores a set of mentions that link to each other,
		// along with a method for getting the most representative mention
		// Both sentence and token offsets start at 1!
		Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		//System.out.println();
		return list;
	}
}