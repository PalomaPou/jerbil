package jerbil;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;

import com.winterwell.bob.tasks.CopyTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.bob.tasks.RSyncTask;
import com.winterwell.bob.tasks.SCPTask;


import com.winterwell.utils.io.FileUtils;

import jobs.BuildBob;
import jobs.BuildUtils;
import jobs.BuildWeb;
import jobs.BuildWinterwellProject;

public class BuildJerbil extends BuildWinterwellProject {

	public BuildJerbil() {
		super(new File(FileUtils.getWinterwellDir(), "jerbil"));
		setIncSrc(true);
		setMainClass("Jerbil");
	}
	

	


	@Override
	public void doTask() throws Exception {	
		super.doTask();
		// dependencies
		File libdir = new File("lib");
//		FileUtils.deleteDir(libdir);
		libdir.mkdirs();
		assert libdir.isDirectory() : libdir;
		for(BuildWinterwellProject bwp : new BuildWinterwellProject[]
				{
					new BuildUtils(),
					new BuildWeb(),
					new BuildBob(),
				}) 
		{
			bwp.setIncSrc(true);
			bwp.run();
			File jar = bwp.getJar();
			FileUtils.copy(jar, new File(libdir, jar.getName()));
		}
				
//		// 3rd party
//		EclipseClasspath ec = new EclipseClasspath(FileUtils.getWorkingDirectory());		
//		Set<File> jars = ec.getCollectedLibs();
//		System.out.println("Required Jars: "+jars);												
//		CopyTask copy = new CopyTask(jars, libdir).setOverwriteIfNewer(true);	
//		copy.setExceptionOnDuplicate(true);
//		copy.run();			
	}

}
