import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.http.HttpServlet;

import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.io.FileEvent;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.WatchFiles;
import com.winterwell.utils.io.WatchFiles.IListenToFileEvents;
import com.winterwell.utils.web.WebUtils2;

import sogrow.jerbil.BuildJerbilWebSite;
import sogrow.jerbil.GitCheck;
import sogrow.jerbil.JerbilConfig;
import winterwell.utils.Utils;
import winterwell.utils.gui.GuiUtils;
import winterwell.utils.reporting.Log;
import winterwell.utils.web.WebUtils;
import winterwell.web.app.FileServlet;
import winterwell.web.app.JettyLauncher;


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
		
		if (config.server) {
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
		config = ArgsParser.getConfig(config, args, new File("config/jerbil.properties"), leftoverArgs);		
		if (config.projectdir!=null) return config;
		
		if ( ! leftoverArgs.isEmpty()) {
			File dir =new File(leftoverArgs.get(0));
			config.projectdir = dir;
			return config;
		}		
		// are we in a Jerbil dir?
		if (new File(FileUtils.getWorkingDirectory(), "webroot").isDirectory()) {
			config.projectdir = FileUtils.getWorkingDirectory();
			return config;
		}			
		// Ask
		File dir = GuiUtils.selectFile("Pick website project's base directory", null, new FileFilter() {				
			@Override
			public boolean accept(File pathname) {
				return pathname!=null && pathname.isDirectory();
			}
		});
		config.projectdir = dir;
		return config;	
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
