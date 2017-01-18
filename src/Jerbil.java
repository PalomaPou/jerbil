import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.http.HttpServlet;

import com.winterwell.utils.Utils;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.io.FileEvent;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.WatchFiles;
import com.winterwell.utils.io.WatchFiles.IListenToFileEvents;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.JettyLauncher;

import sogrow.jerbil.BuildJerbilWebSite;
import sogrow.jerbil.GitCheck;
import sogrow.jerbil.JerbilConfig;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;

import com.winterwell.utils.time.TimeUtils;

import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.JettyLauncher;



/**
 * Command line entry point for Jerbil:
 * 
 * java -cp jerbil.jar:lib/* Jerbil
 * 
 * @author daniel
 *
 */
public class Jerbil {

	private static BuildJerbilWebSite b;
	private static GitCheck gitCheck;

	/**
	 * Watch for edits and keep rebuilding!
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		JerbilConfig config = getConfig(args);		
		if (config.projectdir==null) {
			System.out.println("Run in a Jerbil website project directory -- or with the path to one as a parameter.");
			return;
		}
		// build
		b = new BuildJerbilWebSite(config);		
		b.run();
		// exit?
		if (config.exit) {
			return;
		}
		// run a web server?
		if (config.server) {
			runServer(config);
		}
		
		// watch for file edits
		runWatcher(config);
		// watch for git edits
		if (config.gitcheck!=null) {
			gitCheck = new GitCheck(config.projectdir, config.gitcheck);
			gitCheck.start();
		}
		
		if (config.server && ! config.preview) {
			WebUtils2.display(WebUtils.URI("http://localhost:"+config.port));
		}
		// spin the main thread
		while(true) {
			Utils.sleep(10000);
		}
	}

	private static void runServer(JerbilConfig config) {
		JettyLauncher jl = new JettyLauncher(config.projectdir, config.port);
		jl.setWebRootDir(b.getWebroot());
		jl.setWebXmlFile(null);
		jl.setCanShutdown(false);
		jl.setup();		
		
		HttpServlet fileServer = new FileServlet();
		//		DynamicHttpServlet dynamicRouter = new DynamicHttpServlet();
//		jl.addServlet("/*", dynamicRouter);
		jl.addServlet("/*", fileServer);
		Log.report("web", "...Launching Jetty web server on port "+config.port, Level.INFO);
		
		jl.run();		
	}

	/**
	 * Where is the project
	 * @param args
	 * @return Can be null
	 */
	private static JerbilConfig getConfig(String[] args) {
		JerbilConfig config = new JerbilConfig();
		List<String> leftoverArgs = new ArrayList();
		config = ArgsParser.getConfig(config, args, null, leftoverArgs);
		File dir = config.projectdir!=null? config.projectdir : getConfig2_dir(leftoverArgs);
		config.projectdir = dir;
		// add config properties
		File f = new File(dir, "config/jerbil.properties").getAbsoluteFile();
		if (f.exists()) {
			new ArgsParser(config).set(f);
		}
		return config;	
	}

	private static File getConfig2_dir(List<String> leftoverArgs) {
		if ( ! leftoverArgs.isEmpty()) {
			File dir =new File(leftoverArgs.get(0));
			return dir;
		}		
		// are we in a Jerbil dir?
		if (new File(FileUtils.getWorkingDirectory(), "webroot").isDirectory()) {
			return FileUtils.getWorkingDirectory();
		}			
		// Ask
		File dir = GuiUtils.selectFile("Pick website project's base directory", null, new FileFilter() {				
			@Override
			public boolean accept(File pathname) {
				return pathname!=null && pathname.isDirectory();
			}
		});
		return dir;
	}

	protected static void runWatcher(JerbilConfig config) throws IOException {		
		System.out.println("Watching "+b.getProjectDir()+"/pages for edits");
		
		WatchFiles watch = new WatchFiles();
		watch.addFile(new File(b.getProjectDir(), "pages"));
		watch.addListener(new IListenToFileEvents() {
			@Override
			public void processEvent(FileEvent pair2) {
				b.run();
			}			
		});				
		
		Thread watchThread = new Thread(watch);
		watchThread.start();		
	}
}
