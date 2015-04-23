package us.ne.state.ocio.publichealth.utilities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class ToxiPhinmsBridge {
	private static final Logger log = LogManager.getLogger(ToxiPhinmsBridge.class);

	public static void main(String[] args) throws IOException {
		ToxiPhinmsBridge shb = new ToxiPhinmsBridge();
		JCommander jc = new JCommander(shb, args);
		jc.setProgramName("java -jar simpleHL7Batcher.jar");
		if (shb.run() == 42)
			jc.usage();
	}

	@Parameter(names = "--input", description = "Input directory")
	private String input;

	@Parameter(names = "--output", description = "Output directory")
	private String output;

	@Parameter(names = "--archive", description = "Archive directory")
	private String archive;

	@Parameter(names = "--cooldown", description = "Ignore files modified less than [cooldown] seconds ago. (This avoids partial files.)")
	private int coolDownInSeconds = 30;

	@Parameter(names = "--batchsize", description = "Number of files per batch")
	private int batchSize = 200;

	@Parameter(names = "--help", help = true, description = "This help message.", hidden = true)
	private boolean help;

	private int run() throws IOException {
		if (help || anyNull(input, output, archive)) {
			return 42; //magic number tells caller to display usage;
		}

		Filter<Path> filter = new CoolDownFilterWithoutDirectories<Path>(coolDownInSeconds); //defined in this file.
		
		TreeSet<Path> files = new TreeSet<Path>(new SlowComparator<Path>()); //defined in this file.
		
	    log.info("Preparing file list...");
		for(Path p : Files.newDirectoryStream(Paths.get(input), filter)) {
	        files.add(p);
	    }
		log.info("Found {} suitable files in {}.",files.size(),input);
		
		int inputCounter = 0;
		int outputCounter = 0;
		Path outputPath = null;
		long batchNumber = 0;
		
		Path archivePath = null;
		for (Path p : files) {
			if(inputCounter >= batchSize) inputCounter = 0;
			if(inputCounter == 0) {
				outputCounter++;
				batchNumber = System.currentTimeMillis();
				archivePath = Files.createDirectory(Paths.get(archive + FileSystems.getDefault().getSeparator() + batchNumber));
				outputPath=getNewOutputFile(batchNumber);
			}
			Files.write(outputPath, Files.readAllLines(p, StandardCharsets.UTF_8), StandardCharsets.UTF_8,
		            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.move(p, archivePath.resolve(p.getFileName()));
			inputCounter++;
		}
		log.debug("created {} batch files in {}.",outputCounter,output);
		return 0;
	}

	private Path getNewOutputFile(long batchNumber) {
		return Paths.get(output + FileSystems.getDefault().getSeparator() + "batch." + batchNumber + ".hl7");
	}

	private static boolean anyNull(Object... args) {
		for (Object o : args) {
			if (o == null)
				return true;
		}
		return false;
	}
	
	class CoolDownFilterWithoutDirectories<T> implements Filter<Path> {
		int coolDownInSeconds;
		
		public CoolDownFilterWithoutDirectories(int coolDownInSeconds) {
			this.coolDownInSeconds = coolDownInSeconds;
		}

		@Override
		public boolean accept(Path entry) throws IOException {
			return !Files.isDirectory(entry) && Files.getLastModifiedTime(entry).toMillis() < System.currentTimeMillis() - coolDownInSeconds * 1000;
		}

	}
	
	//TODO it's slow (on 660 files).
	class SlowComparator<T> implements Comparator<Path> {
		@Override
		public int compare(Path o1, Path o2) {
	        try {
	        	String timePlusName1 = Files.getLastModifiedTime(o1) + o1.getFileName().toString();
	        	String timePlusName2 = Files.getLastModifiedTime(o2) + o2.getFileName().toString();
	            return timePlusName1.compareTo(timePlusName2);
	        } catch (IOException e) {
	            log.error("Sorting file list failed.",e);
	        }
	        return 0;
	    }

	}		
}

