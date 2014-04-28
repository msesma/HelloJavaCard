
package eu.sesma.hellojc;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class HelloJc extends Applet {

    private static final byte[] INPUT = {
            'H', 'e', 'l', 'l', 'o',
    };
    private static final byte INPUT_LENGHT = 5;
    private static final byte[] OUTPUT = {
            'G', 'o', 'o', 'd', 'b', 'y', 'e'
    };
    private static final byte OUTPUT_LENGHT = 7;
    private static byte[] buffer;

    protected HelloJc() {
    }

    public static void install(final byte bArray[], final short bOffset, final byte bLength) throws ISOException {
        new HelloJc().register();
    }

    // The JCRE dispatches incoming APDUs to the process method.The APDU object
    // is owned and maintained by the JCRE. It
    // encapsulates details of the underlying transmission protocol (T0 or T1 as
    // specified in ISO 7816-3) by providing a
    // common interface.
    public void process(final APDU apdu) throws ISOException {
        if (selectingApplet()) {
            return;
        }

        buffer = apdu.getBuffer();

        // CLA MASK allows for different channels
        if ((buffer[ISO7816.OFFSET_CLA] & 0xFC) != 0x00) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        switch (buffer[ISO7816.OFFSET_INS]) {
            case (byte) 0xBB: // HELLO APDU
                getHelloWorld(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void getHelloWorld(final APDU apdu) throws ISOException {
        // assume this command has incoming data Lc tells us the incoming apdu
        // command length
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
        if (lc < INPUT_LENGHT) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Read the data
        short readCount = apdu.setIncomingAndReceive();
        while (lc > 0) {
            // process bytes in buffer[5] to buffer[readCount+4];
            lc -= readCount;
            readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
        }

        if (Util.arrayCompare(buffer, (ISO7816.OFFSET_CDATA), INPUT, (short) 0, (short) INPUT.length) == 0) {
            // inform system that the applet has finished processing the command
            // and the system should now prepare to
            // construct a response APDU which contains data field
            short le = apdu.setOutgoing();

            // setOutgoing always returns 256 for Le but Java specifies that le should not be read from buffer
            // so this will never happen, but it is convenient for security reasons
            if (le < OUTPUT_LENGHT) {
                ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
            }

            // informs the CAD the actual number of bytes returned
            apdu.setOutgoingLength(OUTPUT_LENGHT);

            // Set the response data
            Util.arrayCopyNonAtomic(OUTPUT, (short) 0, buffer, (short) 0, OUTPUT_LENGHT);

            apdu.sendBytes((short) 0, OUTPUT_LENGHT);
        } else {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
    }

    // UNUSED

    // This method is called by the JCRE to indicate that this applet has been
    // selected. It performs necessary
    // initialization, which is required to process the subsequent APDU
    // messages.
    // public boolean select() {
    // return true;
    // }

    // This method is called by the JCRE to inform the applet that it should
    // perform any clean-up and bookkeeping tasks
    // before the applet is deselected.
    // public void deselect() {
    // }

}
