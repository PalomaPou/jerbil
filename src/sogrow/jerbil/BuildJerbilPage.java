package sogrow.jerbil;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.petebevin.markdown.MarkdownProcessor;
import com.winterwell.utils.IReplace;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;

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

	void run() {
		String html = FileUtils.read(template).trim();
		String page = FileUtils.read(src).trim();
		Map<String,String> vars = new ArrayMap();
		
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
				vars.put(k, v);
				prev = m.end();				
			}			
			page = page.substring(prev).trim();
			
			// TODO upgrade to https://github.com/sirthias/pegdown
			MarkdownProcessor mp = new MarkdownProcessor();
			page = mp.markdown(page);
		}
		
		// Variables
		// TODO key: value at the top of file -> javascript jerbil.key = value variables
		// TODO files -> safely restricted file access??
		html = html.replace("$contents", page);
		html = html.replace("$webroot", ""); // TODO if dir is a sub-dir of webroot, put in a local path here, e.g. ".." 
		long modtime = src.lastModified();
		// vars
		html = html.replace("$modtime", new Time(modtime).toString());
		// TODO refactor ScriptProperties to be creole free, and use that. It handles urls nicely.
		for(String k : vars.keySet()) {
			html = html.replace("$"+k, vars.get(k));
		}
		
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
		
		out.getParentFile().mkdir();
		FileUtils.write(out, html);
		Log.i(LOGTAG, "Made "+out);
	}
	
}
