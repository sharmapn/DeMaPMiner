package readCommits.Input;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import readCommits.structs.Commit;

public class Reader {

	String fileName;
	List<Commit> commits;
	
	private static final String  regPat = "commit .*\\nAuthor: .*\\nDate:";
	private static Pattern pattern;
	
	public Reader(String fileName){
		this.fileName = fileName;
		commits = new ArrayList<Commit>();
		pattern = Pattern.compile(regPat);
	}
	
	//START HERE
	public void run(){
		System.out.println("Processing new File"); 
		int startIndex = 0;
		int endIndex = 0;
		final int shift = 5;
		String file;
		String newFile;
		
		try {
			file = readFile();
		} catch (IOException e) {

			e.printStackTrace();
			return;
		}
		
		newFile = file.substring(shift);
		System.out.println("b"); 
		boolean trigger = true;
		while(trigger){
		//for(int c = 0; c < 150; c++){
			System.out.println("b1");
			endIndex = indexOf(newFile);
			if(endIndex == -1){
				trigger = false;
				endIndex = file.length() - shift;
			}
			//endIndex += startIndex;
			System.out.println("b2");
			Commit addComm = new Commit(file.substring(0, endIndex + shift));
			System.out.println("b3");
			commits.add(addComm);
			
			if(trigger == false)
				break;
			
			startIndex = endIndex + shift;
			file = file.substring(endIndex + shift);
			newFile = file.substring(shift);
			
			//System.out.println("file = " + file.length() + "\nnewFile = " +  newFile.length());
		}
		System.out.println("c"); 
	}
	
    /** @return index of pattern in s or -1, if not found */
	private static int indexOf(String s) {
	    Matcher matcher = pattern.matcher(s);
	    return matcher.find() ? matcher.start() : -1;
	}
	
	private String readFile() throws IOException 
	{
		  byte[] encoded = Files.readAllBytes(Paths.get(fileName));
		  return Charset.defaultCharset().decode(ByteBuffer.wrap(encoded)).toString();
	}

	public List<Commit> getCommits() {
		return commits;
	}
}
