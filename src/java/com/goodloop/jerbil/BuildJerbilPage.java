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
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.web.fields.SField;

/**
 * This is where the text -> html magic happens 
 */
public class BuildJerbilPage {

	private static final String LOGTAG = "jerbil";
	private File src;
	private File dir;
	private File out;
	private File template;

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
	
	Map<String, Object> var = new HashMap();
	
	public void setVars(Map<String, ?> vars) {
		this.var = new HashMap(vars); // defensive copy to avoid accidental shared structure
	}

	void run() {
		String html = FileUtils.read(template).trim();
		// check the template
		checkTemplate(html);
		
		String page = FileUtils.read(src).trim();		
		
		// NB: leave html as-is
		boolean applyMarkdown = ! (FileUtils.getType(src).equals("html") || FileUtils.getType(src).equals("htm"));
		html = run2_render(applyMarkdown, page, html);
				
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
	private String run2_render(boolean applyMarkdown, String srcPage, String templateHtml) {
		if (applyMarkdown) {
			// Strip out variables
			Pattern KV = Pattern.compile("^(\\w+):(.*)$", Pattern.MULTILINE);
			Matcher m = KV.matcher(srcPage);
			int prev=0;			
			while(m.find()) {
				// +2 to allow for \r\n endings (untested)
				if (m.start() > prev+2) break;
				String k = m.group(1);
				String v = m.group(2).trim();
				var.put(k, v);
				prev = m.end();				
			}			
			srcPage = srcPage.substring(prev).trim();
			
			// TODO upgrade to https://github.com/vsch/flexmark-java 
			// or https://github.com/atlassian/commonmark-java
			srcPage = Markdown.render(srcPage);
//			MarkdownProcessor mp = new MarkdownProcessor();
//			page = mp.markdown(page);
		}
		// $title
		if (var!=null && ! var.containsKey("title")) {
			var.put("title", StrUtils.toTitleCasePlus(FileUtils.getBasename(src)));
		}
		
		// Variables
		String html = insertVariables(templateHtml, srcPage);
		
		// Recursive fill in of file references
		html = run3_fillSections(html);

		// Jerbil version marker
		html = addJerbilVersionMarker(html);
		
		return html;
	}

	private String run3_fillSections(String html) {
		Pattern SECTION = Pattern.compile(
				"<section\\s+src=['\"]([\\S'\"]+)['\"]\\s*(/>|>(.*)</section\\s*>)", Pattern.CASE_INSENSITIVE+Pattern.DOTALL);
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
					contents = fhtml.substring(match.start()+8, match.end()-8);
				}				
				
				File file = resolveRef(insert);
				String ftype = FileUtils.getType(file);
				String text = FileUtils.read(file);
				
				boolean srcIsHtml = ftype.endsWith("html") || ftype.endsWith("htm");
				if (srcIsHtml) {
					// src is html or a template
					String shtml = run2_render(true, contents, text);					
					sb.append(shtml);
				} else {
					// src is a markdown file -- run it through a dummy template
					String sectionHtml = run2_render(true, text+contents, "<section>$contents</section>");
					sb.append(sectionHtml);					
				}				
			}
		});
		return html2;
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

	private String insertVariables(String html, String page) {
		// TODO key: value at the top of file -> javascript jerbil.key = value variables
		// TODO files -> safely restricted file access??
		html = html.replace("$generator", "Jerbil version "+JerbilConfig.VERSION);
		html = html.replace("$contents", page);
		html = html.replace("$webroot", ""); // TODO if dir is a sub-dir of webroot, put in a local path here, e.g. ".." 
		long modtime = src.lastModified();
		// vars
		html = html.replace("$modtime", new Time(modtime).toString());
		if ( ! var.containsKey("date")) {			
			var.put("date", new Time(modtime).format("d MMM yyyy"));
		}
		// TODO use SimpleTemplateVars.java. It handles urls nicely.
		// First, word boundaries
		for(String k : var.keySet()) {
			Object v = var.get(k);
			String vs = Printer.toString(v);
			html = html.replaceAll("\\$"+Pattern.quote(k)+"\\b", vs);
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
	
}
