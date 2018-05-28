package com.goodloop.jerbil;


import java.io.File;

import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.bob.BuildTask;
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
		pages = config.getPagesDir();
		webroot = config.getWebRootDir();
	}
	
	public BuildJerbilWebSite(JerbilConfig config) {
		this.config = config;
		pages = config.getPagesDir();
		webroot = config.getWebRootDir();
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
		if ( ! dir.isDirectory()) {
			throw Utils.runtime(new IOException("Not a directory: "+dir));
		}
		for(File f : dir.listFiles()) {
			if (f.isFile()) {				
				String relpath = FileUtils.getRelativePath(f, pages);		
				File out = new File(webroot, relpath);		
				out = FileUtils.changeType(out, "html");
				
				File template = getTemplate(out);
				assert template != null : "No html template?! "+webroot;
				
				BuildJerbilPage bjp = new BuildJerbilPage(f, out, template);
				Map<String, String> vars = config.var;
				bjp.setVars(vars);
				bjp.run();
				continue;
			}
			if (f.isDirectory()) {
				doTask2(f);
			}
		}
	}

	protected File getTemplate(File outputFile) {
		File dir = outputFile.isDirectory()? outputFile : outputFile.getParentFile();
		File tf = new File(dir, "template.html");
		if (tf.exists()) return tf;
		// recurse??
		// default
		return new File(webroot, "template.html");
	}

	
	

}
