package com.goodloop.jerbil;

import java.io.File;

import com.winterwell.utils.Dep;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;

public class PageEditorServlet implements IServlet {

	@Override
	public void process(WebRequest state) throws Exception {
		// TODO Auto-generated method stub
		JerbilConfig jc = Dep.get(JerbilConfig.class);
		String slug = state.getSlug();
		System.out.println(slug);
		String body = state.getPostBody();
		File f = jc.getWebRootDir();
	}

}
