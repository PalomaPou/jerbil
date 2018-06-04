package com.goodloop.jerbil;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.LinkResolver;
import com.vladsch.flexmark.html.LinkResolverFactory;
import com.vladsch.flexmark.html.HtmlRenderer.Builder;
import com.vladsch.flexmark.html.HtmlRenderer.HtmlRendererExtension;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.html.renderer.LinkType;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

/**
 * Wow this is a complex mess to implement a link-resolver. 
 * See https://github.com/vsch/flexmark-java/blob/master/flexmark-java-samples/src/com/vladsch/flexmark/samples/PegdownCustomLinkResolverOptions.java
 * https://github.com/vsch/flexmark-java/wiki/Extensions#wikilinks
 * @author daniel
 *
 */
public class JerbilLinkResolverExtension implements Parser.ParserExtension, HtmlRendererExtension {


	public static Parser.ParserExtension create() {
		return new JerbilLinkResolverExtension();
	}

	@Override
	public void extend(Builder arg0, String arg1) {
		LinkResolverFactory lrf = new JerbilLinkResolverFactory();
		arg0.linkResolverFactory(lrf);
	}

	@Override
	public void rendererOptions(MutableDataHolder arg0) {
	}

	@Override
	public void extend(com.vladsch.flexmark.parser.Parser.Builder arg0) {
		// huh?
	}

	@Override
	public void parserOptions(MutableDataHolder arg0) {		
	}
	
}


class JerbilLinkResolverFactory implements LinkResolverFactory {

	@Override
	public boolean affectsGlobalScope() {
		return false;
	}

	@Override
	public LinkResolver create(LinkResolverContext arg0) {
		return new JerbilLinkResolver();
	}

	@Override
	public Set<Class<? extends LinkResolverFactory>> getAfterDependents() {
		return null;
	}

	@Override
	public Set<Class<? extends LinkResolverFactory>> getBeforeDependents() {
		return null;
	}
	
}



class JerbilLinkResolver implements LinkResolver {

	JerbilConfig config = Dep.get(JerbilConfig.class);
	
	Map<String,String> urlForLink = new HashMap();
	
	@Override
	public ResolvedLink resolveLink(Node arg0, LinkResolverContext arg1, ResolvedLink arg2) {
		Log.d("linkresolver", Printer.str(Arrays.asList(arg0, arg1, arg2)));
		String target = arg2.getTarget();
		LinkType lt = arg2.getLinkType();
		String url = arg2.getUrl(); // e.g. Publishers-How-to-install-Good-Loop-on-your-site
		// no protocol or path?
		if ( ! url.startsWith("http") && ! url.startsWith("/") && ! url.startsWith("#")) {			
			File pd = config.getPagesDir();
			
			String canonUrl = canon(url);
			List<File> match = FileUtils.find(pd, f -> canonUrl.equals(canon(f.getName())));
			if (match.isEmpty()) {
				Log.d("linkresolver", Printer.str(Arrays.asList(url))+" - no match");
				return arg2;
			}
			String rpath = FileUtils.getRelativePath(match.get(0), config.getPagesDir());
			File htmlpath = FileUtils.changeType(new File(rpath), "html");
			String url2 = htmlpath.toString();
			if ( ! url2.endsWith(".html")) {
				url2 += ".html"; // NB: if no type, changeType doesnt add??
			}
			Log.d("linkresolver", Printer.str(Arrays.asList(url))+" -> "+match.get(0));
			ResolvedLink resolved = arg2.withUrl(url2);
			return resolved;
		}
		return arg2;
	}

	private String canon(String url) {
		String bn = FileUtils.getBasename(url);
		return StrUtils.toCanonical(bn).replaceAll("\\s", "");
	}
	
	
}