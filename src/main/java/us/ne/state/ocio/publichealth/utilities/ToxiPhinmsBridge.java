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

	public static void main(String[] args) throws IOException {
		ToxiPhinmsBridge tpb = new ToxiPhinmsBridge();
		JCommander jc = new JCommander(tpb, args);
		jc.setProgramName("java -jar toxiPhinmsBridge.jar");
		if (tpb.run() == 42)
			jc.usage();
	}

	@Parameter(names = "--incoming", description = "PHINMS incoming directory")
	private String input;

	@Parameter(names = "--outgoing", description = "PHINMS outgoing directory")
	private String outgoing;

	@Parameter(names = "--output", description = "Toxicall bidirectional directory")
	private String toxiDirectory;

	@Parameter(names = "--cooldown", description = "Ignore files modified less than [cooldown] seconds ago. (This avoids partial files.)")
	private int coolDownInSeconds = 30;

	@Parameter(names = "--help", help = true, description = "This help message.", hidden = true)
	private boolean help;

	private static final String sFiles = "^.*[.](ts[fs]|tdf)([.]zip)?";  //We're going to pass Pattern.CASE_INSENSITIVE when we compile these.
	private static final String aFiles = "^.*[.](ta[fs]|tdf)([.]zip)?";
	
	private int run() throws IOException {
		if (help || anyNull(input, toxiDirectory, outgoing)) {
			return 42; //magic number tells caller to display usage;
		}

		Filter<Path> aFilesFilter = new CoolDownFilterWithoutDirectories<Path>(coolDownInSeconds,aFiles); 
		Filter<Path> sFilesFilter = new CoolDownFilterWithoutDirectories<Path>(coolDownInSeconds,sFiles); 
		
		//These two loops take care of the PCC side...
		//   ...And the DOH side.  Cool.  the --input directory means one thing on PCC and another on DOH...
		
		for (Path p : Files.newDirectoryStream(Paths.get(toxiDirectory), sFilesFilter)) {
			log.info("Attempting to move {} to {}.",p,outgoing);
			Files.move(p, Paths.get(outgoing).resolve(p.getFileName()));
		}
		
		for (Path p : Files.newDirectoryStream(Paths.get(input), aFilesFilter)) {
			log.info("Attempting to move {} to {}.",p,toxiDirectory);
			Files.move(p, Paths.get(toxiDirectory).resolve(p.getFileName()));
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

