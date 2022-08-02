package connections;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertiesFile {
													//DEMAPMinerPEPs2018.prop";		DEMAPMinerNewToReadML.prop
													//DEMAPMinerPEPsNew.prop  - Main one
													// PM file for process mining
													//DEMAPMinerJEPs2019.prop
													//  DEMAPMinerPEPsNew.prop";  //MAIN FILE
	//static String propertiesFileName = "C:\\DeMap_Miner\\conf\\spbooks.prop";
	//static String propertiesFileName = "D:\\DeMap_Miner\\conf\\DEMAPMinerBIPs2021.prop";
	static String propertiesFileName = "C:\\DeMapMiner\\conf\\DEMAPMinerPEPsNew.prop";
										// "C:\\DeMap_Miner\\conf\\DEMAPMinerPEPsNew.prop";  //// normally usedfile for peps file 
										// "C:\\DeMap_Miner\\conf\\DEMAPMinerBIPs2021.prop"; 
	//static String propertiesFileName = "C:\\DeMap_Miner\\conf\\DEMAPMinerJEPs2019.prop";      //DEMAPMinerNewToReadML.prop"  - created to read just additional Mailing list python 3000;
	
	public PropertiesFile() {
		//System.out.format("%50s%50s", "propertiesFileName", propertiesFileName); System.out.println();
		//System.out.println("Thread.currentThread().getStackTrace(); "+ Thread.currentThread().getStackTrace().toString());
		//System.out.println("Thread.currentThread().getStackTrace(); "+ Thread.dumpStack());
	}
	
    public static void WriteToPropertiesFile(String key, String data) {
        FileOutputStream fileOut = null;        FileInputStream fileIn = null;
        try {
            Properties configProperty = new Properties();

            File file = new File(propertiesFileName);        fileIn = new FileInputStream(file);
            configProperty.load(fileIn);         			 configProperty.setProperty(key, data);
            fileOut = new FileOutputStream(file);            configProperty.store(fileOut, "sample properties");
        } catch (Exception ex) {
            Logger.getLogger(PropertiesFile.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {   fileOut.close();
            } catch (IOException ex) {
                Logger.getLogger(PropertiesFile.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static String readFromPropertiesFile(String key, boolean allowSpaces){ 
    	String value = null;
    		try {	
    			File file = new File(propertiesFileName);
    			FileInputStream fileInput = new FileInputStream(file);    			Properties properties = new Properties();    			properties.load(fileInput);
    			fileInput.close();

    			Enumeration enuKeys = properties.keys();
    			while (enuKeys.hasMoreElements()) {
    				String keyTemp = (String) enuKeys.nextElement();
    				if(keyTemp.equals(key)){
    					value = properties.getProperty(key);
    					//output just before returning
    					if(allowSpaces) {
    						System.out.format("%50s%50s", key, value); System.out.println();
    						//System.out.println(key + " read in from file : " + value); 
    					}
    					else {
    						if(value.contains(" ")) {
	    						String val[] = value.split(" ");
	    						for(String v: val) {
	    							System.out.println(key + " read in from file : " + v);
	    						}
    						}
    					}    					
    					return value;
    				}
    			}
    		} catch (FileNotFoundException e) {    			e.printStackTrace();    		} 
    		  catch (IOException e) {	e.printStackTrace();    		}
    	System.out.println(key + " read in from file : " + value);
    	return value;
    }

    public static void main(String[] args) {
       // WriteToPropertiesFile help = new WriteToPropertiesFile();
        WriteToPropertiesFile("appwrite1", "write1");        WriteToPropertiesFile("appwrite2", "write2");        WriteToPropertiesFile("appwrite3", "write3");        
        readFromPropertiesFile("appwrite3",false);        
    }
}