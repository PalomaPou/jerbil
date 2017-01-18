package sogrow.jerbil;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.petebevin.markdown.MarkdownProcessor;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.FileEvent;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.WatchFiles;
import com.winterwell.utils.io.WatchFiles.IListenToFileEvents;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.IFilter;
import com.winterwell.utils.IReplace;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;

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
		
		// TODO also parse html for section tags and variables
		
//		// Slides
//		slideDir = new File(projectDir, "slides");
//		File template = getTemplate(slideDir);		
	}
	
	/**
	 * dir pages
	 * */
	private void doTask2(File dir) {
		for(File f : dir.listFiles()) {
			if (f.isFile()) {
				doBuildHtml(dir, f);
				continue;
			}
			if (f.isDirectory()) {
				doTask2(f);
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

	private void doBuildHtml(final File dir, File f) {
		File template = getTemplate(webroot);
		String html = FileUtils.read(template);
		String page = FileUtils.read(f);
		
		if ( ! page.isEmpty() && page.startsWith("<")) {
			// html -- keep the page as-is
		} else {
			// TODO upgrade to https://github.com/sirthias/pegdown
			MarkdownProcessor mp = new MarkdownProcessor();
			page = mp.markdown(page);
		}
		
		// Variables
		// TODO key: value at the top of file -> javascript jerbil.key = value variables
		// TODO files -> safely restricted file access??
		html = html.replace("$contents", page);
		html = html.replace("$webroot", ""); // TODO if dir is a sub-dir of webroot, put in a local path here, e.g. ".." 
		long modtime = f.lastModified();
		html = html.replace("$modtime", new Time(modtime).toString());
		
		// Recursive fill in of file references
		Pattern SECTION = Pattern.compile("<section\\s+src=['\"]([\\S'\"]+)['\"]\\s*/>", Pattern.CASE_INSENSITIVE+Pattern.DOTALL);
		for(int depth=0; depth<10; depth++) {
			String html2 = StrUtils.replace(html, SECTION, new IReplace() {
				@Override
				public void appendReplacementTo(StringBuilder sb, Matcher match) {
					String insert = match.group(1);
					// TODO security check: not below webroot!
					File fi = new File(insert);
					if ( ! fi.isAbsolute()) fi = new File(dir, insert);
					String text = FileUtils.read(fi);
					// TODO recursive use of this doBuildHtml method -- but we don't want the whole template
					MarkdownProcessor mp2 = new MarkdownProcessor();
					String texthtml = mp2.markdown(text);
					sb.append(texthtml);
				}
			});
			if (html2.equals(html)) {
				break;				
			}
			html = html2;
		}
		
		String relpath = FileUtils.getRelativePath(f, pages);		
		File out = new File(webroot, relpath);		
		out = FileUtils.changeType(out, "html");
		out.getParentFile().mkdir();
		FileUtils.write(out, html);
		Log.i(LOGTAG, "Made "+out);
	}
	
	
	

}
