package jerbil;

import java.io.File;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.io.FileUtils;

import jobs.BuildBob;
import jobs.BuildUtils;
import jobs.BuildWeb;
import jobs.BuildWinterwellProject;

/**
 * ??How to build jerbil-all.jar??
 * @author daniel
 *
 */
public class BuildJerbil extends BuildWinterwellProject {

	public BuildJerbil() {
		super(new File(FileUtils.getWinterwellDir(), "jerbil"));
		setIncSrc(true);
		setMainClass("Jerbil");
	}
	

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		ArraySet list = new ArraySet(super.getDependencies());
		list.add(new BuildUtils());
		list.add(new BuildWeb());
		list.add(new BuildBob());
		return list;
	}
	


	@Override
	public void doTask() throws Exception {	
//		super.doTask();		
		// bundle
		doFatJar();
	}




}
