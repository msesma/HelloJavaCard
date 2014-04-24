package eu.sesma.hellojc;

import com.licel.jcardsim.io.CAD;
import com.licel.jcardsim.io.JavaxSmartCardInterface;

import junit.framework.Assert;

import org.bouncycastle.util.Arrays;
import org.junit.Before;
import org.junit.Test;

import javacard.framework.AID;
import javacard.framework.ISO7816;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

@SuppressWarnings("deprecation")
public class HelloJctest {

	HelloJc applet;

	private static final byte[] APPLET_AID = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x00, 0x00 };
	AID appletAID;
	JavaxSmartCardInterface simulator;

	@Before
	public void setup() {
		// For testing utility methods
		applet = new HelloJc();

		appletAID = new AID(APPLET_AID, (short) 0, (byte) APPLET_AID.length);

		// 2 - Local Mode with ResponseAPDU transmitCommand(CommandAPDU) method
		System.setProperty("com.licel.jcardsim.terminal.type", "2");
		CAD cad = new CAD(System.getProperties());

		simulator = (JavaxSmartCardInterface) cad.getCardInterface();
		simulator.installApplet(appletAID, HelloJc.class);

	}

	@Test
	public void testSelectAppletShouldReturnOk() {
		Assert.assertTrue(simulator.selectApplet(appletAID));
	}

	@Test
	public void testProcessIncorrectClaShouldReturnSW_CLA_NOT_SUPPORTED() {
		simulator.selectApplet(appletAID);
		ResponseAPDU response = simulator.transmitCommand(new CommandAPDU(0x80, 0xbc, 0x00, 0x00));
		Assert.assertEquals(ISO7816.SW_CLA_NOT_SUPPORTED, (short) response.getSW());
	}

	@Test
	public void testProcessCorrectClaNonZeroChannelShouldReturnSW_INS_NOT_SUPPORTED() {
		simulator.selectApplet(appletAID);
		ResponseAPDU response = simulator.transmitCommand(new CommandAPDU(0x01, 0xbc, 0x00, 0x00));
		Assert.assertEquals(ISO7816.SW_INS_NOT_SUPPORTED, (short) response.getSW());
	}

	@Test
	public void testProcessUnsupportedApduShouldReturnSW_INS_NOT_SUPPORTED() {
		simulator.selectApplet(appletAID);
		ResponseAPDU response = simulator.transmitCommand(new CommandAPDU(0x00, 0xbc, 0x00, 0x00));
		Assert.assertEquals(ISO7816.SW_INS_NOT_SUPPORTED, (short) response.getSW());
	}

	@Test
	public void testProcessHelloApduCase2ShouldReturnSW_WRONG_DATA() {
		simulator.selectApplet(appletAID);
		ResponseAPDU response = simulator.transmitCommand(new CommandAPDU(0x00, 0xbb, 0x00, 0x00, 0x7));
		Assert.assertEquals(ISO7816.SW_WRONG_DATA, (short) response.getSW());
	}

	@Test
	public void testProcessHelloApduWrongDataLenghtShouldReturnSW_WRONG_LENGTH() {
		simulator.selectApplet(appletAID);
		byte[] data = { 0x01, 0x02, 0x03, 0x04 };
		ResponseAPDU response = simulator.transmitCommand(new CommandAPDU(0x00, 0xbb, 0x00, 0x00, data));
		Assert.assertEquals(ISO7816.SW_WRONG_LENGTH, (short) response.getSW());
	}

	@Test
	public void testProcessHelloApduWrongDataShouldReturnSW_WRONG_DATA() {
		simulator.selectApplet(appletAID);
		byte[] data = { 'H', 'e', 'l', 'l', 'a', };
		ResponseAPDU response = simulator.transmitCommand(new CommandAPDU(0x00, 0xbb, 0x00, 0x00, data));
		Assert.assertEquals(ISO7816.SW_WRONG_DATA, (short) response.getSW());
	}

	@Test
	public void testProcessHelloApduGoodDataWrongLeShouldReturnSW_WRONG_LENGTH() {
		simulator.selectApplet(appletAID);
		byte[] data = { 'H', 'e', 'l', 'l', 'o', };
		ResponseAPDU response = simulator.transmitCommand(new CommandAPDU(0x00, 0xbb, 0x00, 0x00, data, 0x06));
		Assert.assertEquals(ISO7816.SW_WRONG_LENGTH, (short) response.getSW());
	}

	@Test
	public void testProcessHelloApduGoodDataShouldReturnGoodbyeAndOK() {
		simulator.selectApplet(appletAID);
		byte[] data = { 'H', 'e', 'l', 'l', 'o', };
		byte[] responseData = { 'G', 'o', 'o', 'd', 'b', 'y', 'e' };

		ResponseAPDU response = simulator.transmitCommand(new CommandAPDU(0x00, 0xbb, 0x00, 0x00, data));
		Assert.assertTrue(Arrays.areEqual(responseData, response.getData()));
		Assert.assertEquals(ISO7816.SW_NO_ERROR, (short) response.getSW());
	}

}
