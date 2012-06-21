package test.integ.be.fedict.commons.eid.client;

import org.junit.Test;

import be.fedict.commons.eid.client.LibJ2PCSCGNULinuxFix;

public class LibJ2PCSCGNULinuxFixTest
{
	private void _testFix()
	{
		LibJ2PCSCGNULinuxFix.fixNativeLibrary(new TestLogger());
	}
	
	public static void main(String[] args)
	{
		LibJ2PCSCGNULinuxFixTest fixtest=new LibJ2PCSCGNULinuxFixTest();
		fixtest._testFix();

	}
	
	@Test
	public void testFix() throws Exception
	{
		_testFix();
	}
}
