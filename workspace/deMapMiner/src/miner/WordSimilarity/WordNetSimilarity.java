/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package miner.WordSimilarity;

import java.io.File;

import wordnet.WordNet.WordNetSense;
import wordnet.WordNet.WordNetType;
//import edu.smu.tspell.wordnet.*;
import edu.sussex.nlp.jws.*;

/**
 *
 * @author shrutiranjans
 */
public class WordNetSimilarity {
                       //C:\Users\psharma\semantic\JavaWordNetSimilarity\WordNet
    public String dir = "C:\\Users\\psharma\\semantic\\JavaWordNetSimilarity\\WordNet";
    public JWS ws = new JWS(this.dir, "3.0");
    
    public static void main(String args[]) {
    	try {
  //  	File f =new File("C:\\Users\\psharma\\Google Drive\\PhDOtago\\code\\scripts\\inputFiles\\WordNet-3.0\\dict");
   // 	System.setProperty("wordnet.database.dir", f.toString());	//setting path for the WordNet Directory
    	System.out.println("## s:");
//        WordNetDatabase database = WordNetDatabase.getFileInstance();
        System.out.println("## t:");
        //WordNetSimilarity wnsTest = new WordNetSimilarity();
        System.out.println("## u:");
        
        String word1 = "run";
//        Synset[] wordSynsets = database.getSynsets(word1, SynsetType.VERB);
        int sense1 = 1;
        //System.out.println(word1 + ", " + wordSynsets.length);
        //word1 = wordSynsets[sense1].getWordForms()[0];
        //word1 = "run";
        WordNetType type1 = WordNetType.VERB;
        
        String word2 = "slicing";
        int sense2 = 1;
        //word2 = wordSynsets[sense2].getWordForms()[0];
        WordNetType type2 = WordNetType.VERB;
        
        WordNetSense wordSense1 = new WordNetSense(word1, sense1, type1);
        WordNetSense wordSense2 = new WordNetSense(word2, sense2, type2);
        
        double wuPalmerScore = wuPalmerSimilarity(wordSense1, wordSense2);
//        double leacockChodorowScore = wnsTest.leacockChodorowSimilarity(wordSense1, wordSense2);
//        double hirstStOngeScore = wnsTest.hirstStOngeSimilarity(wordSense1, wordSense2);
//        double resnikScore = wnsTest.resnikSimilarity(wordSense1, wordSense2);
//        double linScore = wnsTest.linSimilarity(wordSense1, wordSense2);
//        double jiangConrathScore = wnsTest.jiangConrathSimilarity(wordSense1, wordSense2);
        
        System.out.println("WuAndPalmer similarity score : " + wuPalmerScore);
//        System.out.println("LeacockChodorow similarity score : " + leacockChodorowScore);
//        System.out.println("HirstStOnge similarity score : " + hirstStOngeScore);
//        System.out.println("Resnik similarity score : " + resnikScore);
//        System.out.println("Lin similarity score : " + linScore);
//        System.out.println("JiangConrath similarity score : " + jiangConrathScore);
    	}  //end try       
		catch (Exception e){ 
			System.out.println(" ______here  " + e.toString() + "\n" );
			//System.out.println(Thread.currentThread().getStackTrace()  );		//+  " \n" + e.printStackTrace()
			System.out.println(StackTraceToString(e)  );	
			//continue;
		} 
    }
    
    public static String StackTraceToString(Exception ex) {
		String result = ex.toString() + "\n";
		StackTraceElement[] trace = ex.getStackTrace();
		for (int i=0;i<trace.length;i++) {
			result += trace[i].toString() + "\n";
		}
		return result;
	}
	    
    public static double wuPalmerSimilarity(WordNetSense sense1, WordNetSense sense2) {
        double returnScore;
        
        WordNetType type1 = sense1.type;
        WordNetType type2 = sense2.type;
        
        if (type1.equals(type2)) {
            WordNetType type = type1;
            if (type.equals(WordNetType.NOUN) || type.equals(WordNetType.VERB)) {
                WuAndPalmer wp = getWuAndPalmer();
                String pos;
                
                if (type.equals(WordNetType.NOUN)) {
                    pos = "n";
                }
                
                else {
                    pos = "v";
                }
                
                returnScore = wp.wup(sense1.word, sense1.sense, sense2.word, sense2.sense, pos);
            }
            
            else {
                returnScore = -1;
            }
        }
        
        else {
            returnScore = -1;
        }
        
        return returnScore;
    }
    
    private static WuAndPalmer getWuAndPalmer() {
		// TODO Auto-generated method stub
		return null;
	}

	public double leacockChodorowSimilarity(WordNetSense sense1, WordNetSense sense2) {
        double returnScore;
        
        WordNetType type1 = sense1.type;
        WordNetType type2 = sense2.type;
        
        if (type1.equals(type2)) {
            WordNetType type = type1;
            if (type.equals(WordNetType.NOUN) || type.equals(WordNetType.VERB)) {
                LeacockAndChodorow lc = this.ws.getLeacockAndChodorow();
                String pos;
                
                if (type.equals(WordNetType.NOUN)) {
                    pos = "n";
                }
                
                else {
                    pos = "v";
                }
                
                returnScore = lc.lch(sense1.word, sense1.sense, sense2.word, sense2.sense, pos);
            }
            
            else {
                returnScore = -1;
            }
        }
        
        else {
            returnScore = -1;
        }
        
        return returnScore;
    }
    
    public double hirstStOngeSimilarity(WordNetSense sense1, WordNetSense sense2) {
        double returnScore;
        
        WordNetType type1 = sense1.type;
        WordNetType type2 = sense2.type;
        
        if (type1.equals(type2)) {
            WordNetType type = type1;
            if (type.equals(WordNetType.NOUN) || type.equals(WordNetType.VERB)) {
                HirstAndStOnge hs = this.ws.getHirstAndStOnge();
                String pos;
                
                if (type.equals(WordNetType.NOUN)) {
                    pos = "n";
                }
                
                else {
                    pos = "v";
                }
                
                returnScore = hs.hso(sense1.word, sense1.sense, sense2.word, sense2.sense, pos);
            }
            
            else {
                returnScore = -1;
            }
        }
        
        else {
            returnScore = -1;
        }
        
        return returnScore;
    }
    
    public double resnikSimilarity(WordNetSense sense1, WordNetSense sense2) {
        double returnScore;
        
        WordNetType type1 = sense1.type;
        WordNetType type2 = sense2.type;
        
        if (type1.equals(type2)) {
            WordNetType type = type1;
            if (type.equals(WordNetType.NOUN) || type.equals(WordNetType.VERB)) {
                Resnik rs = this.ws.getResnik();
                String pos;
                
                if (type.equals(WordNetType.NOUN)) {
                    pos = "n";
                }
                
                else {
                    pos = "v";
                }
                
                returnScore = rs.res(sense1.word, sense1.sense, sense2.word, sense2.sense, pos);
            }
            
            else {
                returnScore = -1;
            }
        }
        
        else {
            returnScore = -1;
        }
        
        return returnScore;
    }
    
    public double linSimilarity(WordNetSense sense1, WordNetSense sense2) {
        double returnScore;
        
        WordNetType type1 = sense1.type;
        WordNetType type2 = sense2.type;
        
        if (type1.equals(type2)) {
            WordNetType type = type1;
            if (type.equals(WordNetType.NOUN) || type.equals(WordNetType.VERB)) {
                Lin li = this.ws.getLin();
                String pos;
                
                if (type.equals(WordNetType.NOUN)) {
                    pos = "n";
                }
                
                else {
                    pos = "v";
                }
                
                returnScore = li.lin(sense1.word, sense1.sense, sense2.word, sense2.sense, pos);
            }
            
            else {
                returnScore = -1;
            }
        }
        
        else {
            returnScore = -1;
        }
        
        return returnScore;
    }
    
    public double jiangConrathSimilarity(WordNetSense sense1, WordNetSense sense2) {
        double returnScore;
        
        WordNetType type1 = sense1.type;
        WordNetType type2 = sense2.type;
        
        if (type1.equals(type2)) {
            WordNetType type = type1;
            if (type.equals(WordNetType.NOUN) || type.equals(WordNetType.VERB)) {
                JiangAndConrath jcn = this.ws.getJiangAndConrath();
                String pos;
                
                if (type.equals(WordNetType.NOUN)) {
                    pos = "n";
                }
                
                else {
                    pos = "v";
                }
                
                returnScore = jcn.jcn(sense1.word, sense1.sense, sense2.word, sense2.sense, pos);
            }
            
            else {
                returnScore = -1;
            }
        }
        
        else {
            returnScore = -1;
        }
        
        return returnScore;
    }
    
    
    
}
