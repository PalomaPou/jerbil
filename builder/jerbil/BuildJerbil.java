package jerbil;

import java.io.File;
import java.util.List;

import com.goodloop.jerbil.JerbilConfig;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.io.FileUtils;

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
		setVersion(JerbilConfig.VERSION);
		setScpToWW(true);
		setMakeFatJar(true);
	}
	

//	@Override
//	public List<BuildTask> getDependencies() {
//		List<BuildTask> list = super.getDependencies();
////		list.add(new BuildUtils());
////		list.add(new BuildWeb());
////		list.add(new BuildBob());
//		return list;
//	}
	


	@Override
	public void doTask() throws Exception {	
		super.doTask();		
	}




}
