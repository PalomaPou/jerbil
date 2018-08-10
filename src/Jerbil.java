import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.http.HttpServlet;

import com.goodloop.jerbil.BuildJerbilWebSite;
import com.goodloop.jerbil.GitCheck;
import com.goodloop.jerbil.JerbilConfig;
import com.goodloop.jerbil.SimpleManifestServlet;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Environment;
import com.winterwell.utils.Utils;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileEvent;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.WatchFiles;
import com.winterwell.utils.io.WatchFiles.IListenToFileEvents;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.HttpServletWrapper;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.web.fields.SField;



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
		Environment.get().put(new SField("jerbil.version"), JerbilConfig.VERSION);
		JerbilConfig config = getConfig(args);
		if (args.length==1 && "--help".equals(args[0])) {
			System.out.println(new ConfigBuilder(config).getOptionsMessage());
			return;
		}
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
		
		if (config.server && config.preview) {
			WebUtils2.display(WebUtils.URI("http://localhost:"+config.port));
		}
		// spin the main thread
		while(true) {
			Utils.sleep(10000);
		}
	}

	private static void runServer(JerbilConfig config) {
		File webroot = b.getWebroot();
		JettyLauncher jl = new JettyLauncher(webroot, config.port);
		jl.setWebXmlFile(null);
		jl.setCanShutdown(false);
		jl.setup();				
		HttpServlet fileServer = new FileServlet().setBaseDir(webroot);
		// servlets
		jl.addServlet("/manifest", new HttpServletWrapper(SimpleManifestServlet.class));
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
		ConfigFactory cf = ConfigFactory.get().setArgs(args);
		ConfigBuilder cb = cf.getConfigBuilder(JerbilConfig.class);
		JerbilConfig config = cb.get();
		
		List<String> leftoverArgs = cb.getRemainderArgs();
		File dir = config.projectdir!=null? config.projectdir : getConfig2_dir(leftoverArgs);
		config.projectdir = dir;
		// add dir's config properties, which could have been missed by the "global" files above
		File f1 = new File(dir, "config/jerbil.properties").getAbsoluteFile();
		File f2 = new File(dir, "config/"+cf.getMachine()+".properties").getAbsoluteFile();
		for(File f : new File[] {f1,f2}) {
			if (f.exists()) {
				cb = new ConfigBuilder(config);
				config = cb
						.set(f)
						.setFromMain(args) // args walways win
						.get();
			}
		}
		
		Log.d("init", "Config:	"+config);
		Dep.set(JerbilConfig.class, config);
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
		System.out.println("Watching "+b.getProjectDir()+"/"+config.pages+" + html templates for edits");
		
		{
			WatchFiles watch = new WatchFiles();
			watch.addFile(config.getPagesDir());
			watch.addListener(new IListenToFileEvents() {
				@Override
				public void processEvent(FileEvent pair2) {
					b.run();
				}			
			});						
			Thread watchThread = new Thread(watch);
			watchThread.setName("watch-"+b.getProjectDir().getName());
			watchThread.start();
		}
		{	// templates
			WatchFiles watch = new WatchFiles();
			watch.addFile(new File(b.getProjectDir(), config.webroot));
			watch.addListener(new IListenToFileEvents() {
				@Override
				public void processEvent(FileEvent fe) {
					if (fe.file.getName().equals("template.html")) {
						b.run();
					}
					// TODO less compilation also
				}			
			});						
			Thread watchThread = new Thread(watch);
			watchThread.setName("watch-"+b.getProjectDir().getName());
			watchThread.start();
		}
	}
}
