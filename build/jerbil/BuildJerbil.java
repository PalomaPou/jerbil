package jerbil;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;

import winterwell.bob.tasks.CopyTask;
import winterwell.bob.tasks.EclipseClasspath;
import winterwell.bob.tasks.JarTask;
import winterwell.bob.tasks.RSyncTask;
import winterwell.bob.tasks.SCPTask;

import com.winterwell.utils.io.FileUtils;

import jobs.BuildBob;
import jobs.BuildDepot;
import jobs.BuildMaths;
import jobs.BuildStat;
import jobs.BuildUtils;
import jobs.BuildWeb;
import jobs.BuildWinterwellProject;
import sogrow.jerbil.JerbilConfig;

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
