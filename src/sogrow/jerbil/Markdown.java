package sogrow.jerbil;

import java.util.Arrays;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.web.WebUtils;

public class Markdown {

	public static String render(String page) {
		MutableDataSet options = new MutableDataSet();

        // uncomment to set optional extensions
        options.set(Parser.EXTENSIONS, Arrays.asList(
        		TablesExtension.create(), 
        		StrikethroughExtension.create()
        		, AnchorLinkExtension.create()
        		));

        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // You can re-use parser and renderer instances
        Node document = parser.parse(page);
        String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"
  
        for(int hi=1; hi<6; hi++) {
	        int indx = 0;   
	        while(true) {
	        	int i = html.indexOf("<h"+hi, indx);
	        	if (i==-1) break;
	        	int tagEnd = html.indexOf("</h"+hi, i);
	        	String h = WebUtils.stripTags(html.substring(i, tagEnd));
	        	String cn = StrUtils.toCanonical(h).replaceAll("\\s+", "-");
	        	int j = html.indexOf("<h1", i+1);
	        	if (j==-1) j = html.length();
	        	String div = "<div class='h"+hi+"-section "+cn+"'>";
	        	html = html.substring(0, i)+div+html.substring(i, j)+"</div><!-- ./"+cn+" -->"
	        			+html.substring(j);
	        	indx = i + div.length() + 1;
	        }
        }
//          System.out.println(html);
        return html;
	}

}
