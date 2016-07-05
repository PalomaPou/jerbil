import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;


public class JerbilTest {

	@Test
	public void testMain() throws IOException {
		File sogive = new File(FileUtils.getWinterwellDir(), "SoGive");
		assert sogive.isDirectory() : sogive;
		Jerbil.main(new String[]{sogive.getAbsolutePath()});
	}

}
