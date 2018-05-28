package com.goodloop.jerbil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.Dep;
import com.winterwell.utils.Environment;
import com.winterwell.utils.IReplace;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.web.fields.SField;

// TODO mo
public class BuildJerbilPage {

	private static final String LOGTAG = "jerbil";
	private File src;
	private File dir;
	private File out;
	private File template;

	public BuildJerbilPage(File src, File out, File template) {
		this.src = src;
		this.dir = src.getParentFile();
		this.out = out;
		this.template = template;
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
		
		html = run2_render(src, page, html);
				
		out.getParentFile().mkdir();
		FileUtils.write(out, html);
		Log.i(LOGTAG, "Made "+out);
	}

	private String run2_render(File src2, String page, String html) {
		if (FileUtils.getType(src).equals("html") || FileUtils.getType(src).equals("htm")) {
			// html -- keep the page as-is
		} else {
			// Strip out variables
			Pattern KV = Pattern.compile("^(\\w+):(.*)$", Pattern.MULTILINE);
			Matcher m = KV.matcher(page);
			int prev=0;			
			while(m.find()) {
				// +2 to allow for \r\n endings (untested)
				if (m.start() > prev+2) break;
				String k = m.group(1);
				String v = m.group(2).trim();
				var.put(k, v);
				prev = m.end();				
			}			
			page = page.substring(prev).trim();
			
			// TODO upgrade to https://github.com/vsch/flexmark-java 
			// or https://github.com/atlassian/commonmark-java
			page = Markdown.render(page);
//			MarkdownProcessor mp = new MarkdownProcessor();
//			page = mp.markdown(page);
		}
		// $title
		if (var!=null && ! var.containsKey("title")) {
			var.put("title", StrUtils.toTitleCasePlus(FileUtils.getBasename(src)));
		}
		
		// Variables
		html = insertVariables(html, page);
		
		// Recursive fill in of file references
		Pattern SECTION = Pattern.compile("<section\\s+src=['\"]([\\S'\"]+)['\"]\\s*/>", Pattern.CASE_INSENSITIVE+Pattern.DOTALL);
//		for(int depth=0; depth<10; depth++) {
		final String fhtml = html;
		String html2 = StrUtils.replace(fhtml, SECTION, new IReplace() {
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				String insert = match.group(1);
				// TODO security check: not below webroot!
				File fi = new File(insert);
				if ( ! fi.isAbsolute()) fi = new File(dir, insert);
				String text = FileUtils.read(fi);
				String sectionHtml = run2_render(fi, text, fhtml.substring(match.start(), match.end()-2)+"$contents</section>");
				sb.append(sectionHtml);
			}
		});
//			if (html2.equals(html)) {
//				break;				
//			}
		html = html2;
//		}
		// Jerbil version marker
		html = addJerbilVersionMarker(html);
		
		return html;
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
