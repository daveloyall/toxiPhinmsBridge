package us.ne.state.ocio.publichealth.utilities;

import java.io.File;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class SimpleHL7Batcher {
	private static final Logger log = LogManager.getLogger(SimpleHL7Batcher.class);
	
	public static void main(String[] args) {
		SimpleHL7Batcher shb = new SimpleHL7Batcher();
		JCommander jc = new JCommander(shb, args);
		jc.setProgramName("java -jar simpleHL7Batcher.jar");
		if (shb.run() == 42) jc.usage();
	}

	@Parameter(names = "--input", description = "Input directory")
	private String input;

	@Parameter(names = "--output", description = "Output directory")
	private String output;
	
	@Parameter(names = "--fhs3", description = "FHS-3 value")
	private String fhs3;
	
	@Parameter(names = "--help", help = true, description = "This help message.", hidden = true) //TODO don't hide but also don't show "Default: false"
	private boolean help;
	
	private int run() {
		if(help || anyNull(input,output,fhs3)) {
			return 42; //magic number tells caller to display usage;
		}
		
		
		
		File[] inputFiles = new File(input).listFiles();
		
		if (inputFiles == null) {
			log.error("ERROR: Invalid input directory: " + input);
			return 42;
		}
		
		log.info("input directory: {}", input);
		
		for (File f : inputFiles)
				log.info(f.getName());

		return 0;
	}

	

	private static boolean anyNull(Object... args) {
		for (Object o : args) {
			if (o == null) return true;
		}
		return false;
	}
}
