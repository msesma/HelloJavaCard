
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
    private static final byte[] OUTPUT = {
            'G', 'o', 'o', 'd', 'b', 'y', 'e'
    };
    private static final byte CLA = (byte) 0x00;
    // CLA MASK allows for different channels
    private static final byte CLA_MASK = (byte) 0xFC;
    private static final byte HELLO_INS = (byte) 0xBB;

    protected HelloJc() {
    }

    public static void install(final byte bArray[], final short bOffset, final byte bLength) throws ISOException {
        new HelloJc().register();
    }

    // The JCRE dispatches incoming APDUs to the process method.The APDU object is owned and maintained by the JCRE. It
    // encapsulates details of the underlying transmission protocol (T0 or T1 as specified in ISO 7816-3) by providing a
    // common interface.
    public void process(final APDU apdu) throws ISOException {
        if (selectingApplet()) {
            return;
        }

        byte[] buffer = apdu.getBuffer();
        if ((buffer[ISO7816.OFFSET_CLA] & CLA_MASK) != CLA) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        byte ins = buffer[ISO7816.OFFSET_INS];
        switch (ins) {
            case HELLO_INS:
                getHelloWorld(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void getHelloWorld(final APDU apdu) throws ISOException {
        byte[] buffer = apdu.getBuffer();
        // assume this command has incoming data Lc tells us the incoming apdu command length
        short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
        if (bytesLeft < (short) INPUT.length) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short readCount = apdu.setIncomingAndReceive();
        while (bytesLeft > 0) {
            // process bytes in buffer[5] to buffer[readCount+4];
            bytesLeft -= readCount;
            readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
        }

        if (contains(buffer, INPUT) == ISO7816.OFFSET_LC + 1) {
            short length = (short) OUTPUT.length;
            // inform system that the applet has finished processing the command and the system should now prepare to
            // construct a response APDU which contains data field
            short le = apdu.setOutgoing();
            // Unfortunately setOutgoing always returns 256 for Le so lets retrieve it in other way
            // This works fine on unit test but is not detected on yubikey. More testing necessary
            le = buffer[(short) (ISO7816.OFFSET_LC + buffer[ISO7816.OFFSET_LC] + 1)];
            if (le != 0 && le < length) {
                ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
            }

            // informs the CAD the actual number of bytes returned
            apdu.setOutgoingLength((byte) length);

            // Set the response data
            Util.arrayCopyNonAtomic(OUTPUT, (short) 0, buffer, (short) 0, length);

            apdu.sendBytes((short) 0, length);
        } else {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
    }

    public short contains(final byte[] data, final byte[] pattern) {
        if (data == null || pattern == null || data.length < pattern.length) {
            return -1;
        }

        byte j;
        for (short i = 0; i < (short) (data.length - pattern.length + 1); i++) {
            j = 0;
            while (j < pattern.length && data[(short) (i + j)] == pattern[j]) {
                j++;
            }
            if (j == pattern.length) {
                return i;
            }
        }
        return -1;
    }

    // UNUSED

    // This method is called by the JCRE to indicate that this applet has been selected. It performs necessary
    // initialization, which is required to process the subsequent APDU messages.
    public boolean select() {
        return true;
    }

    // This method is called by the JCRE to inform the applet that it should perform any clean-up and bookkeeping tasks
    // before the applet is deselected.
    public void deselect() {
    }

}
