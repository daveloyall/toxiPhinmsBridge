package us.ne.state.ocio.publichealth.utilities;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class ToxiPhinmsBridge {
	private static final Logger log = LogManager.getLogger(ToxiPhinmsBridge.class);

	
	//Set up some command line parameters to be used later.
	@Parameter(names = "--incoming", description = "PHINMS incoming directory", required=true)
	private String phinmsIncoming;

	@Parameter(names = "--outgoing", description = "PHINMS outgoing directory", required=true)
	private String phinmsOutgoing;

	@Parameter(names = "--toxi", description = "Toxicall bidirectional directory", required=true)
	private String toxiDirectory;

	@Parameter(names = "--site", description = "There are two valid values: pcc or doh", required=true)
	private String site;

	@Parameter(names = "--cooldown", description = "Ignore files modified less than [cooldown] seconds ago. (This avoids partial files.)")
	private int coolDownInSeconds = 30;

	@Parameter(names = "--help", help = true, description = "This help message.", hidden = true)
	private boolean help;

	@Parameter(names = "--properLogs", help = true, description = "Emit proper logs (one line per message)")
	private boolean properLogs = false;

	//Describe some filenames with wildcards for later.
	//We're going to pass Pattern.CASE_INSENSITIVE when we compile these regex.
	private static final String sFiles = "^.*[.]ts[fs]([.]zip)?";  // *.tsf, *.tss, *.tsf.zip, *.tss.zip
	private static final String aFiles = "^.*[.]ta[fs]([.]zip)?";  // *.taf, *.tas, *.taf.zip, *.tas.zip
	private static final String tdfFiles = "^.*[.]tdf([.]zip)?";  // *.tss, *.tsf.zip, *.tss.zip
	
	public static void main(String[] args) throws IOException {
		//Create a new instance of the tool class.
		ToxiPhinmsBridge tpb = new ToxiPhinmsBridge();
		// Create an instance of the command line argument handler, feed it the tool and the command line arguments.
		JCommander jc = new JCommander(tpb, args);
		// tell jc how to behave.
		jc.setProgramName("java -jar toxiPhinmsBridge.jar");
		jc.setCaseSensitiveOptions(false);
		
		// run the tool, and if it sends back the magic word, tell the command line handler to describe the command line options for the user.
		if (tpb.run() == 42)
			jc.usage();
	}
	
	private int run() throws IOException {

		//Check how we were invoked and maybe quit.
		if (help || anyNull(phinmsIncoming, toxiDirectory, phinmsOutgoing, site)) {
			return 42; //magic number tells caller to display usage before dying.
		}

		
		//Setup things up based on command line options.
		// There are two possible log formats and the boolean properLogs tells us which to use.
		String logLine = properLogs ? "Attempting to move {} to {}." : "Attempting to move\n\t{}\n  to\n\t{}.";
		// This will be used to determine which file filter to use, later.
		boolean thisIsPCC = site.equalsIgnoreCase("pcc"); // Bug: --site foo would mean the same as --site doh.
		// 
		Filter<Path> aFilesFilter = new CoolDownFilterWithoutDirectories<Path>(coolDownInSeconds,aFiles); 
		Filter<Path> sFilesFilter = new CoolDownFilterWithoutDirectories<Path>(coolDownInSeconds,sFiles); 
		Filter<Path> tdfFilesFilter = new CoolDownFilterWithoutDirectories<Path>(coolDownInSeconds,tdfFiles); 
		
		
		//Real work begins around here.		
		
		int pinToxiCount = 0;
		//Get a list of files in the PHINMS incoming directory...
		for (Path p : Files.newDirectoryStream(Paths.get(phinmsIncoming), thisIsPCC ? aFilesFilter : sFilesFilter)) {
			log.info(logLine,p,toxiDirectory);
			Files.move(p, Paths.get(toxiDirectory).resolve(p.getFileName()));
			pinToxiCount++;
		}
		if (pinToxiCount > 0) { // If we moved some files...
			//get the tdf, too.  Otherwise, don't.
			for (Path p : Files.newDirectoryStream(Paths.get(phinmsIncoming), tdfFilesFilter)) {
				log.info(logLine,p,toxiDirectory);
				Files.move(p, Paths.get(toxiDirectory).resolve(p.getFileName()));
			}
			return 0; //We've received new files from outside, let's not worry about the files from inside for this run.
		}
		
		int toxiPoutCount = 0;
		for (Path p : Files.newDirectoryStream(Paths.get(toxiDirectory), thisIsPCC ? sFilesFilter : aFilesFilter)) {
			log.info(logLine,p,phinmsOutgoing);
			Files.move(p, Paths.get(phinmsOutgoing).resolve(p.getFileName()));
			toxiPoutCount++;
		}
		if (toxiPoutCount > 0) { //if we moved some files...
			for (Path p : Files.newDirectoryStream(Paths.get(toxiDirectory), tdfFilesFilter)) {
				log.info(logLine,p,phinmsOutgoing);
				Files.move(p, Paths.get(phinmsOutgoing).resolve(p.getFileName()));
			}
		}
		
		return 0;
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
		Pattern p;
		
		public CoolDownFilterWithoutDirectories(int coolDownInSeconds, String regexFilter) {
			this.coolDownInSeconds = coolDownInSeconds;
			this.p = Pattern.compile(regexFilter,Pattern.CASE_INSENSITIVE); 
		}

		@Override
		public boolean accept(Path entry) throws IOException {
			boolean filterResult = p.matcher(entry.getFileName().toString()).matches();
			boolean coolDownResult = Files.getLastModifiedTime(entry).toMillis() < System.currentTimeMillis() - coolDownInSeconds * 1000;
			return !Files.isDirectory(entry) && coolDownResult && filterResult; 
		}

	}
	
}

