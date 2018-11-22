package jerbil;

import java.io.File;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.io.FileUtils;

import com.winterwell.bob.wwjobs.BuildUtils;
import com.winterwell.bob.wwjobs.BuildWeb;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;

/**
 * The latest Jerbil bundle can be downloaded from
 * https://www.winterwell.com/software/downloads/jerbil-all.jar
 * 
 * @author daniel
 *
 */
public class BuildJerbil extends BuildWinterwellProject {

	public BuildJerbil() {
		super(new File(FileUtils.getWinterwellDir(), "jerbil"));
		setIncSrc(true);
		setMainClass("Jerbil");
		setScpToWW(true);
		setMakeFatJar(true);
	}
	

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		ArraySet list = new ArraySet(super.getDependencies());
//		list.add(new BuildUtils());
//		list.add(new BuildWeb());
//		list.add(new BuildBob());
		return list;
	}
	


	@Override
	public void doTask() throws Exception {	
		super.doTask();		
	}




}
