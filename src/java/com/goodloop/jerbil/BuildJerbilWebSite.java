package com.goodloop.jerbil;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.web.WebUtils2;

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
			if (f.isDirectory()) {
				doTask2(f);
				continue;
			}
			if ( ! f.isFile()) {
				// huh?
				continue;
			}
			// "mail merge"?
			if ( f.getName().endsWith(".csv")) {
				doTask3_CSV(f);
				continue;
			}
			// a stray binary or other file? just copy it
			String type = FileUtils.getType(f).toLowerCase();
			// see https://fileinfo.com/filetypes/text
			if ( ! type.isEmpty() && ! "txt md markdown text html htm rtf wiki me 1st ascii asc eml".contains(type)) {
				Log.d(LOGTAG, "Copy as-is "+f);
				String relpath = FileUtils.getRelativePath(f, pages);		
				File out = new File(webroot, relpath);
				FileUtils.copy(f, out);
				continue;
			}
			doTask3_oneFile(f);
		}
	}

	
	void doTask3_oneFile(File f) {
		// Process a file!
		File out = getOutputFileForSource(f);
		
		File template = getTemplate(out);
		assert template != null : "No html template?! "+webroot;
		
		BuildJerbilPage bjp = new BuildJerbilPage(f, out, template);
		Map<String, String> vars = config.var;
		bjp.setBaseVars(vars);
		bjp.run();		
	}

	private void doTask3_CSV(File csvFile) {
		try {
			CSVSpec spec = new CSVSpec();
			CSVReader r = new CSVReader(csvFile,spec);
			// the 1st line MUST be column headers
			String[] header = r.next();
			// TODO case etc flexible header handling (as SoGive's csv code does)
			// 2 stage templating?
			File mdtemplateFile = getSrcTemplate(csvFile);
			String mdtemplate = mdtemplateFile != null? FileUtils.read(mdtemplateFile) : null;
			
			for (String[] row : r) {
				if (row.length==0) continue;
				doTask4_CSV_row(csvFile, header, mdtemplate, row, r.getLineNumber());
			}
		} catch(Exception ex) {
			Log.e(ex+" from "+csvFile); // TODO better error handling in Jerbil??
		}
	}

	private void doTask4_CSV_row(File csvFile, String[] header, String mdtemplate, String[] row, int rowNum) {
		// turn a row into a map of key:value variables
		HashMap map = new HashMap();
		for (int i = 0; i < header.length; i++) {
			String hi = header[i];
			if (Utils.isBlank(hi)) continue;
			hi = hi.trim();
			hi = hi.replaceAll("\\s+", "_"); // no whitespace in variable names
			hi = hi.replaceAll("\\W+", ""); // no punctuation
			map.put(hi, row[i]);
		}
		String srcText = Printer.toString(map, "\n", ":");
		srcText = StrUtils.substring(srcText, 1, -1); // chop the wrappping {}
		
		// now process into the template
		// ...2 stage template? .md then .html?				
		if (mdtemplate != null) {
			srcText += "\n\n"+mdtemplate;
		}
		// ...normal
		File out = getOutputFileForSource(csvFile);				
		out = FileUtils.changeType(out, rowNum+row[0]+".html");
		File template = getTemplate(out);
		assert template != null : "No html template?! "+webroot;
		// ...run
		BuildJerbilPage bjp = new BuildJerbilPage(csvFile, srcText, out, template);
		Map<String, String> vars = config.var;
		bjp.setBaseVars(vars);
		bjp.run();
	}

	protected File getOutputFileForSource(File f) {
		String relpath = FileUtils.getRelativePath(f, pages);		
		File out = new File(webroot, relpath);		
		out = FileUtils.changeType(out, "html");
		return out;
	}

	protected File getTemplate(File outputFile) {
		File dir = outputFile.isDirectory()? outputFile : outputFile.getParentFile();
		File tf = new File(dir, "template.html");
		if (tf.exists()) return tf;
		// recurse??
		// default
		return new File(webroot, "template.html");
	}

	/**
	 * A src template is a markdown file used to process .csv
	 * @param outputFile
	 * @return can be null
	 */
	protected File getSrcTemplate(File csvFile) {
		File dir = csvFile.getParentFile();
		// allow a few options
		File tf1 = FileUtils.changeType(csvFile, "md");
		File tf2 = FileUtils.changeType(csvFile, "txt");
		File tf3 = new File(dir, "template.md");
		File tf4 = new File(dir, "template.html");		
		List<File> templates = Containers.filter(Arrays.asList(tf1,tf2,tf3,tf4), f -> f.isFile());
		if (templates.isEmpty()) return null;		
		if (templates.size() > 1) {
			Log.w(LOGTAG, "Multiple source templates available (only the first will be used) for "+csvFile+": "+templates);
		}
		return templates.get(0);
	}
	
	

}
