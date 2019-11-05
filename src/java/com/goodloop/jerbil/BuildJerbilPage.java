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
import com.winterwell.utils.web.WebUtils2;
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
	private String srcText;

	/**
	 * 
	 * @param src e.g. pages/mypage.md
	 * @param out e.g. webroot/mypage.html
	 * @param template
	 */
	public BuildJerbilPage(File src, File out, File template) {
		this(src, null, out, template);
	}

	public BuildJerbilPage(File src, String srcText, File out, File template) {
		this.src = src;
		this.srcText = srcText;
		this.dir = src==null? null : src.getParentFile();
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
		
		String page = srcText==null? FileUtils.read(src) : srcText;
		page = page.trim();
		
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
		// $title (done before looking at the local vars so they could override it)
		if (src != null) {
			var.put("title", StrUtils.toTitleCasePlus(FileUtils.getBasename(src)));
		}
		
		if (applyMarkdown) {
			// Strip out variables
			srcPage = chopSetVars(srcPage, var);
			
			// TODO upgrade to https://github.com/vsch/flexmark-java 
			// or https://github.com/atlassian/commonmark-java
			srcPage = Markdown.render(srcPage);
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
		if ( ! files.isEmpty()) {
			return files.get(0);			
		}
		
		// 4. a webroot file
		File outDir = out.getParentFile();
		JerbilConfig config = Dep.get(JerbilConfig.class);
		File webroot = config.getWebRootDir();
		while(outDir!=null) {
			File wf = new File(outDir, insert);
			if (wf.isFile()) {
				return wf;
			}
			outDir = outDir.getParentFile();
			if ( ! FileUtils.contains(webroot, outDir)) {
				break;
			}
		}
		
		throw Utils.runtime(new FileNotFoundException(insert+" referenced in "+src));
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
				int cnt=0;
				while(var.containsKey(v)) {
					v = var.get(v);
					// avoid loops
					cnt++;
					if (cnt>10) break;
				}
				// make the value regex and html safe
				String vs = escapeValue(v);
				
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
			String vs = escapeValue(v);
			html = html.replace("$"+k, vs);
		}
		return html;
	}

	/**
	 * 
	 * @param v
	 * @return html safe and regex safe
	 */
	String escapeValue(Object v) {
		if (v==null) return "";
		String _vs = Printer.toString(v);
		// NB: md doesnt handle £s -s and puts in annoying tags
//		String _vs0 = Markdown.render(_vs).trim(); // This will handle html entities
//		// pop the outer tag that Markdown puts in
//		if (_vs.startsWith("<p>")) _vs = StrUtils.substring(_vs, 3, -4);		
//		String _vs1 = WebUtils2.attributeEncode(_vs);
		
		String _vs2 = WebUtils2.htmlEncodeWithUrlProtection(_vs);
		_vs = _vs2;
		_vs = Matcher.quoteReplacement(_vs);
		return _vs;
	}

	private void checkTemplate(String html) {
		if ( ! html.contains("$contents") && ! html.contains("$nocontents")) {
			throw new IllegalStateException("The template file MUST contain the $contents or $nocontents variable: "+template);
		}
	}

	public void setBaseVars(Map<String, String> vars) {
		baseVars = new HashMap(vars); // paranoid copy
	}
	
}
