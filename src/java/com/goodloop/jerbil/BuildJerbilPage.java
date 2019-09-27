package com.goodloop.jerbil;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.Dep;
import com.winterwell.utils.Environment;
import com.winterwell.utils.IReplace;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.web.fields.SField;

/**
 * This is where the text -> html magic happens 
 */
public class BuildJerbilPage {

	static final Pattern KV = Pattern.compile("^([A-Za-z0-9\\-_]+):(.*)$", Pattern.MULTILINE);
	private static final String LOGTAG = "jerbil";
	private File src;
	private File dir;
	private File out;
	private File template;
	private Map<String, String> baseVars = new ArrayMap();

	/**
	 * 
	 * @param src e.g. pages/mypage.md
	 * @param out e.g. webroot/mypage.html
	 * @param template
	 */
	public BuildJerbilPage(File src, File out, File template) {
		this.src = src;
		this.dir = src.getParentFile();
		this.out = out;
		this.template = template;
	}
	
	public File getOut() {
		return out;
	}

	@Override
	public String toString() {
		return "BuildJerbilPage [src=" + src + ", out=" + out + ", template=" + template + "]";
	}
	
//	Map<String, Object> var = new HashMap();
	
//	public void setVars(Map<String, ?> vars) {
//		this.var = new HashMap(vars); // defensive copy to avoid accidental shared structure
//	}

	void run() {
		String html = FileUtils.read(template).trim();
		// check the template
		checkTemplate(html);
		
		String page = FileUtils.read(src).trim();		
		
		// NB: leave html as-is
		boolean applyMarkdown = ! (FileUtils.getType(src).equals("html") || FileUtils.getType(src).equals("htm"));
		html = run2_render(applyMarkdown, page, html, baseVars);
				
		out.getParentFile().mkdir();
		FileUtils.write(out, html);
		Log.i(LOGTAG, "Made "+out);
	}

	/**
	 * 
	 * @param applyMarkdown Also strip out vars
	 * @param srcPage Text contents
	 * @param html
	 * @return
	 */
	private String run2_render(boolean applyMarkdown, String srcPage, String templateHtml, Map var) {
		if (applyMarkdown) {
			// Strip out variables
			srcPage = chopSetVars(srcPage, var);
			
			// TODO upgrade to https://github.com/vsch/flexmark-java 
			// or https://github.com/atlassian/commonmark-java
			srcPage = Markdown.render(srcPage);
		}
		// $title
		if (var!=null && ! var.containsKey("title")) {
			var.put("title", StrUtils.toTitleCasePlus(FileUtils.getBasename(src)));
		}
		
		// Variables
		String html = insertVariables(templateHtml, srcPage, var);
		
		// Recursive fill in of file references
		html = run3_fillSections(html, var);

		// Jerbil version marker
		html = addJerbilVersionMarker(html);
		
		return html;
	}

	private String run3_fillSections(String html, Map var) {
		Pattern SECTION = Pattern.compile(
				"<section\\s+src=['\"]([\\S'\"]+)['\"]\\s*(/>|>(.*?)</section\\s*>)", Pattern.CASE_INSENSITIVE+Pattern.DOTALL);
//		for(int depth=0; depth<10; depth++) {
		final String fhtml = html;
		String html2 = StrUtils.replace(fhtml, SECTION, new IReplace() {
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				// src=
				String insert = match.group(1);
				// tag contents?
				String contents;
				if (match.group().endsWith("/>")) {
					// self closing
					contents = "";
				} else {
					// need a close
					String m2 = match.group(2);
					contents = m2.substring(1); // chop the > from the front of m2
					Matcher chopOff = Pattern.compile("</section\\s*>$", Pattern.CASE_INSENSITIVE).matcher(contents);
					if (chopOff.find()) {
						contents = contents.substring(0, chopOff.start());
					}
					contents = contents.trim();
				}				
				
				File file = resolveRef(insert);
				String ftype = FileUtils.getType(file);
				String text = FileUtils.read(file);
				
				boolean srcIsHtml = ftype.endsWith("html") || ftype.endsWith("htm");
				Map varmap2 = new ArrayMap(var); // copy vars then modify (so we don't pass one sections vars into another section)
				
				if (srcIsHtml) {
					// src is html or a template
					String shtml = run2_render(true, contents, text, varmap2);					
					sb.append(shtml);
				} else {
					// src is a markdown file -- run it through a dummy template
					// ...chop contents into vars / contents					
					contents = chopSetVars(contents, varmap2);
					String sectionHtml = run2_render(true, text+contents, "<section>$contents</section>", varmap2);
					sb.append(sectionHtml);					
				}				
			}
		});
		return html2;
	}

	protected String chopSetVars(String srcPage, Map varmap) {
		Matcher varm = KV.matcher(srcPage);
		int prev=0;
		while(varm.find()) {
			// +2 to allow for \r\n endings (untested)
			if (varm.start() > prev+2) break;
			String k = varm.group(1);
			String v = varm.group(2).trim();
			varmap.put(k, v);
			prev = varm.end();
		}			
		srcPage = srcPage.substring(prev).trim();
		return srcPage;
	}

	protected File resolveRef(String insert) {
		// TODO a better security check: not below webroot!
		if (insert.contains("..")) {
			throw new SecurityException("Illegal section src in "+src+": "+insert);
		}
		
		// 1. a template file??
		
		// 2. a local file?
		File fi = new File(dir, insert);
		if (fi.isFile()) return fi;
		
		// 3. a sloppy reference
		JerbilLinkResolver jlr = new JerbilLinkResolver();
		List<File> files = jlr.findFilesFromRef(insert);
		if (files.isEmpty()) {
			throw Utils.runtime(new FileNotFoundException(insert+" referenced in "+src));
		}
		return files.get(0);
	}

	private String addJerbilVersionMarker(String html) {
		JerbilConfig jc = Dep.get(JerbilConfig.class);
		if (jc.noJerbilMarker) return html;
		String v = Environment.get().get(new SField("jerbil.version"));
		String markerInfo = "<meta name='generator' content='Jerbil v"+v+"' />\n";
		html = html.replace("</head>", markerInfo+"</head>");
		html = html.replace("</HEAD>", markerInfo+"</HEAD>");
		return html;
	}

	private String insertVariables(String html, String page, Map<String,Object> var) {
		// TODO key: value at the top of file -> javascript jerbil.key = value variables
		// TODO files -> safely restricted file access??
		html = html.replace("$generator", "Jerbil version "+JerbilConfig.VERSION);
		html = html.replace("$contents", page);
		html = html.replace("$webroot", ""); // TODO if dir is a sub-dir of webroot, put in a local path here, e.g. ".." 
		long modtime = src.lastModified();
		// vars
		if ( ! var.containsKey("date")) {			
			var.put("date", new Time(modtime).format("d MMM yyyy"));
		}
		if ( ! var.containsKey("modtime")) {
			var.put("modtime", new Time(modtime).toString());
		}
		// TODO use SimpleTemplateVars.java. It handles urls nicely.
		// First, word boundaries
		for(String k : var.keySet()) {
			try {
				Object v = var.get(k);			
				// slightly recursive HACK
				if (var.containsKey(v)) {
					v = var.get(v);
				}
				String vs = Matcher.quoteReplacement(Printer.toString(v));				
				String ps = "\\$"+Pattern.quote(k)+"\\b";
				html = html.replaceAll(ps, vs);
			} catch(Exception ex) {
				// Paranoia: it should not be possible to upset the regex handling above.
				Log.e(LOGTAG, ex+" from ["+k+"] in "+src);
			}
		}
		// now anything goes
		for(String k : var.keySet()) {
			Object v = var.get(k);
			String vs = Printer.toString(v);
			html = html.replace("$"+k, vs);
		}
		return html;
	}

	private void checkTemplate(String html) {
		if ( ! html.contains("$contents")) {
			throw new IllegalStateException("The template file MUST contain the $contents variable: "+template);
		}
	}

	public void setBaseVars(Map<String, String> vars) {
		baseVars = vars;
	}
	
}
