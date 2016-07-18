package sogrow.jerbil;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import com.petebevin.markdown.MarkdownProcessor;
import com.winterwell.utils.io.FileEvent;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.WatchFiles;
import com.winterwell.utils.io.WatchFiles.IListenToFileEvents;

import winterwell.bob.BuildTask;
import winterwell.utils.IFilter;
import winterwell.utils.Utils;
import winterwell.utils.gui.GuiUtils;
import winterwell.utils.reporting.Log;
import winterwell.utils.time.Time;

/**
 * Build a Jerbil website.
 * .txt and .md files in the pages directory are converted into .html in the webroot directory.
 * @author daniel
 *
 */
public class BuildJerbilWebSite extends BuildTask {

	JerbilConfig config;	
	File pages;
	File webroot;
	
	public File getWebroot() {
		return webroot;
	}
	
	public File getProjectDir() {
		return config.projectdir;
	}
	
	public BuildJerbilWebSite(File projectDir) {
		config = new JerbilConfig();
		config.projectdir = projectDir;
		pages = new File(projectDir, "pages");
		webroot = new File(projectDir, "webroot");
	}
	
	public BuildJerbilWebSite(JerbilConfig config) {
		this.config = config;
		pages = new File(config.projectdir, "pages");
		webroot = new File(config.projectdir, "webroot");
	}
	
	@Override
	protected void doTask() throws Exception {
		assert pages != null : this;
		assert pages.isDirectory() : pages;
		doTask2(pages);
		
//		// Slides
//		slideDir = new File(projectDir, "slides");
//		File template = getTemplate(slideDir);		
	}
	
	private void doTask2(File dir) {
		for(File f : dir.listFiles()) {
			if (f.isFile()) {
				doBuildHtml(f);
				continue;
			}
			if (f.isDirectory()) {
				doTask2(dir);
			}
		}
	}

	protected File getTemplate(File f) {
		File dir = f.isDirectory()? f : f.getParentFile();
		File tf = new File(dir, "template.html");
		if (tf.exists()) return tf;
		// default
		return new File(webroot, "template.html");
	}

	private void doBuildHtml(File f) {
		File template = getTemplate(webroot);
		String html = FileUtils.read(template);
		String page = FileUtils.read(f);
		
		if ( ! page.isEmpty() && page.startsWith("<")) {
			// html -- keep page as is
		} else {
			// TODO upgrade to https://github.com/sirthias/pegdown
			MarkdownProcessor mp = new MarkdownProcessor();
			page = mp.markdown(page);
		}
		html = html.replace("$contents", page);
		long modtime = f.lastModified();
		html = html.replace("$modtime", new Time(modtime).toString());
		
		File out = new File(webroot, f.getName());
		out = FileUtils.changeType(out, "html");
		FileUtils.write(out, html);
		Log.i(LOGTAG, "Made "+out);
	}
	
	
	

}
