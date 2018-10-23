package jerbil;

import java.io.File;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.io.FileUtils;

import com.winterwell.bob.wwjobs.BuildBob;
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
		super.doTask();		
		// bundle
		File fatJar = doFatJar();
		// ship?
		if (scpToWW) {
			String remoteJar = "/home/winterwell/public-software/"+fatJar.getName();
			SCPTask scp = new SCPTask(fatJar, "winterwell@winterwell.com",				
					remoteJar);
			// this is online at: https://www.winterwell.com/software/downloads
			scp.setMkdirTask(false);
			scp.run();
//			scp.runInThread(); no, wait for it to finish
			report.put("scp to remote", "winterwell.com:"+remoteJar);
		}
	}




}
