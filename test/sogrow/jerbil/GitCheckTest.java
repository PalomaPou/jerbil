package sogrow.jerbil;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import winterwell.utils.time.TUnit;

import com.winterwell.utils.io.FileUtils;

public class GitCheckTest {

	@Test
	public void testIsGitDir() {
		GitCheck gc = new GitCheck(new File("/home"), TUnit.MINUTE.dt);
		assert ! gc.isGitDir();
		
		GitCheck gc2 = new GitCheck(FileUtils.getWorkingDirectory(), TUnit.MINUTE.dt);
		assert gc2.isGitDir();
	}

}
