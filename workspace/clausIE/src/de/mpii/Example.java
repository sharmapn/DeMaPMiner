package de.mpii;
import java.io.IOException;

import de.mpii.clausie.ClausIE;
import de.mpii.clausie.Clause;
import de.mpii.clausie.Proposition;

public class Example {
    public static void main(String[] args) throws IOException {
    	
        System.out.print("*** Starting ***");
    	
        ClausIE clausIE = new ClausIE();
        clausIE.initParser();
        clausIE.getOptions().print(System.out, "# ");

        // input sentence
        String sentence = "Note that this PEP is not currently accepted , so it is a \\* proposed \\* recommendation , rather than an active one "; 
        //String sentence = "the community could not find a majority";
         //"Bell, a telecommunication company, which is based in Los Angeles, makes and distributes electronic, computer and building products.";
        // String sentence = "There is a ghost in the room";
        // sentence = "Bell sometimes makes products";
        // sentence = "By using its experise, Bell made great products in 1922 in Saarland.";
        // sentence = "Albert Einstein remained in Princeton.";
        //sentence = "Albert is smart.";
        //String sentence = " Bell makes electronic, computer and building products.";

        System.out.println("Input sentence   : " + sentence);

        // parse tree
        System.out.print("Parse time       : ");
        long start = System.currentTimeMillis();
        clausIE.parse(sentence);
        long end = System.currentTimeMillis();
        System.out.println((end - start) / 1000. + "s");
        System.out.print("Dependency parse : ");
        System.out.println(clausIE.getDepTree().pennString()
                .replaceAll("\n", "\n                   ").trim());
        System.out.print("Semantic graph   : ");
        System.out.println(clausIE.getSemanticGraph().toFormattedString()
                .replaceAll("\n", "\n                   ").trim());

        // clause detection
        System.out.print("ClausIE time     : ");
        start = System.currentTimeMillis();
        clausIE.detectClauses();
        
        /*
        System.out.println("---------Going to sleep.");
        try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("---------Waking up.");
		*/
        
        
        clausIE.generatePropositions();
        
        /*
        System.out.println("---------Going to sleep.");
        try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println("---------Waking up.");
        */
        end = System.currentTimeMillis();
        System.out.println((end - start) / 1000. + "s");
        System.out.print("Clauses          : ");
        String sep = "";
        for (Clause clause : clausIE.getClauses()) {
            System.out.println(sep + clause.toString(clausIE.getOptions()));
            sep = "                   ";
        }

        // generate propositions
        System.out.print("Propositions     : ");
        sep = "";
        for (Proposition prop : clausIE.getPropositions()) {
            System.out.println(sep + prop.toString());
            sep = "                   ";
        }
    }
}
