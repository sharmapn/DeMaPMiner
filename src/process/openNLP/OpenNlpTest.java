package Process.openNLP;

import opennlp.tools.cmdline.PerformanceMonitor;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

public class OpenNlpTest {
	
	//noun and verb identification with this Open NLP is not as required. SNLP is much better.
	
public static void main(String[] args) throws IOException {
    POSModel model = new POSModelLoader().load(new File("c:\\lib\\opennlp\\nl-pos-maxent.bin"));
    PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
    POSTaggerME tagger = new POSTaggerME(model);

    String input = "Propose rejection of PEP 303 "; //"Can anyone help me dig through OpenNLP's horrible documentation?";
    ObjectStream<String> lineStream =
            new PlainTextByLineStream(new StringReader(input));

    perfMon.start();
    String line;
    while ((line = lineStream.read()) != null) {

        String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE.tokenize(line);
        String[] tags = tagger.tag(whitespaceTokenizerLine);

        POSSample sample = new POSSample(whitespaceTokenizerLine, tags);
        System.out.println(sample.toString());

        perfMon.incrementCounter();
    }
    perfMon.stopAndPrintFinalResult();
}
}
