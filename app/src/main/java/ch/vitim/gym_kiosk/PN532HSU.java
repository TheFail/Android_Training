package ch.vitim.gym_kiosk;

import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class PN532HSU  {

    boolean DEBUG = true;

    final static byte PN532_PREAMBLE = (byte) 0x00;
    final static byte PN532_STARTCODE1 = (byte) 0x00;
    final static byte PN532_STARTCODE2 = (byte) 0xFF;
    final static byte PN532_POSTAMBLE = (byte) 0x00;
    final static byte PN532_HOSTTOPN532 = (byte) 0xD4;
    final static byte PN532_PN532TOHOST = (byte) 0xD5;

    final static long PN532_ACK_WAIT_TIME = 1000;
    final static int PN532_INVALID_ACK = -1;
    final static int PN532_TIMEOUT = -2;
    final static int PN532_INVALID_FRAME = -3;
    final static int PN532_NO_SPACE = -4;

    //# PN532_ Commands;
    final static byte PN532_COMMAND_GETFIRMWAREVERSION = (byte) 0x02;
    final static byte PN532_COMMAND_SAMCONFIGURATION = (byte) 0x14;
    final static byte PN532_COMMAND_INLISTPASSIVETARGET = (byte) 0x4A;
    final static byte PN532_COMMAND_INDATAEXCHANGE = (byte) 0x40;
    final static byte PN532_MIFARE_ISO14443A = (byte) 0x00;

    //# Mifare Commands;
    final static byte MIFARE_CMD_AUTH_A = (byte) 0x60;
    final static byte MIFARE_CMD_AUTH_B = (byte) 0x61;
    final static byte MIFARE_CMD_READ = (byte) 0x30;
    final static byte MIFARE_CMD_WRITE = (byte) 0xA0;
    final static byte MIFARE_CMD_WRITE_ULTRALIGHT = (byte) 0xA2;

    final static byte[] KEYB = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) (byte) 0xFF, (byte) 0xFF};

    final private static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    final private static int MAX_DATA_SIZE_CLASSIC = 3 * 16;        // 3  blocks x 16 bytes
    final private static int MAX_DATA_SIZE_ULTRALIGHT = 12 * 4;     // 12 blocks x 4  bytes


    private byte[] pn532_packetbuffer = new byte[64];

    private byte[] _uid = new byte[7];  // ISO14443A uid
    private byte _uidLen;  // uid len
    private byte[] _key = new byte[6];  // Mifare Classic key
    private byte inListedTag; // Tg number of inlisted tag.

    private byte command;
    private final UsbSerialPort serialPort;
    private final UsbDeviceConnection connection;

    private final int serialPortTimeout = 10;
    private long avaibleDataTimeout = 10000;

    private byte[] buff = new byte[512];
    private int buffReadIndex = 0;
    private int buffSize = 0;


    String LOGTAG = "TEST_UART";

//    private SerialInputOutputManager ioManager;


    public PN532HSU(UsbSerialPort serialPort, UsbDeviceConnection connection) {
        this.serialPort = serialPort;
        this.connection = connection;
        command = 0;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Log.d(LOGTAG, "Sleep error");
        }
    }

    boolean SAMConfig() {
        pn532_packetbuffer[0] = PN532_COMMAND_SAMCONFIGURATION;
        pn532_packetbuffer[1] = (byte) 0x01; // normal mode;
        pn532_packetbuffer[2] = (byte) 0x14; // timeout 50ms * 20 = 1 second
        pn532_packetbuffer[3] = (byte) 0x01; // use IRQ pin!

        if (DEBUG) {
            Log.d(LOGTAG, "SAMConfig");
        }

        if (writeCommand(pn532_packetbuffer, 4) < 0) {
            if (DEBUG) {
                Log.d(LOGTAG, "SAMConfig writeCommand < 0");
            }
            return false;
        }

        boolean result = (0 <= readResponse(pn532_packetbuffer, pn532_packetbuffer.length));
        cleanBuff();
        return result;
    }

    private int writeCommand(byte[] header, int hlen) {
        return writeCommand(header, hlen, new byte[]{0}, 0);
    }

    private int readResponse(byte buf[], int len) {
        return readResponse(buf, len, 1000);
    }

    private int readResponse(byte buf[], int len, long timeout) {

        if (DEBUG) {
            Log.d(LOGTAG, "ReadResponse - readAllBytes(), read count:" + buffReadIndex + ", buffer data hex:" + bytesToHex(buff));
        }

        byte[] tmp = new byte[3];

        if (DEBUG) {
            Log.d(LOGTAG, "\nRead:  ");
        }

        /**
         * Frame Preamble and Start Code // 3
         */
        if (receive(tmp, 3, timeout) <= 0) {            // 3
            return PN532_TIMEOUT;
        }

        if ((byte) 0x00 != tmp[0] || (byte) 0x00 != tmp[1] || (byte) 0xFF != tmp[2]) {
            if (DEBUG) {
                Log.d(LOGTAG, "Preamble error");
            }
            return PN532_INVALID_FRAME;
        }

        /**
         * receive length and check  2
         */
        byte[] length = new byte[2];
        if (receive(length, 2, timeout) <= 0) {         // 2
            return PN532_TIMEOUT;
        }
        if (0 != (byte) (length[0] + length[1])) {
            if (DEBUG) {
                Log.d(LOGTAG, "Length error");
            }
            return PN532_INVALID_FRAME;
        }
        length[0] -= (byte) 2;
        if ((length[0] & 0xFF) > len) {
            return PN532_NO_SPACE;
        }

        /**
         * receive command byte  2
         */
        byte cmd = (byte) (command + 1);               // response command
        if (receive(tmp, 2, timeout) <= 0) {        // 2
            return PN532_TIMEOUT;
        }
        if (PN532_PN532TOHOST != tmp[0] || cmd != tmp[1]) {
            if (DEBUG) {
                Log.d(LOGTAG, "Command error");
            }
            return PN532_INVALID_FRAME;
        }

        if (receive(buf, length[0], timeout) != length[0]) {        // data size //0 // 4
            return PN532_TIMEOUT;
        }
        byte sum = (byte) (PN532_PN532TOHOST + cmd);
        for (int i = 0; i < length[0]; i++) {
            sum += buf[i];
        }

        /**
         * checksum and postamble // 2
         */
        if (receive(tmp, 2, timeout) <= 0) {                // 2
            return PN532_TIMEOUT;
        }
        if (0 != (byte) (sum + tmp[0]) || 0 != tmp[1]) {
            Log.d(LOGTAG, "Checksum error");
            return PN532_INVALID_FRAME;
        }

        return length[0] & 0xFF;
    }

    private int readAckFrame() {
        byte[] PN532_ACK = new byte[]{(byte) 0x0, (byte) 0x0, (byte) 0xFF, (byte) 0x0, (byte) 0xFF, (byte) 0x0};
        byte[] ackBuf = new byte[PN532_ACK.length];

        if (DEBUG) {
            Log.d(LOGTAG, "\nAck: ");
        }

//        waiteReceiveBytes(1000);
        readData(1000);


        if (receive(ackBuf, PN532_ACK.length, PN532_ACK_WAIT_TIME) <= 0) {
            if (DEBUG) {
                Log.d(LOGTAG, "ReadAckFrame: Timeout");
            }
            return PN532_TIMEOUT;
        }

        for (int i = 0; i < PN532_ACK.length; i++) {
            if (ackBuf[i] != PN532_ACK[i]) {
                if (DEBUG) {
                    Log.d(LOGTAG, "readAckFrame: Invalid");
                }
                return PN532_INVALID_ACK;
            }
        }

        if (DEBUG) {
            Log.d(LOGTAG, "ReadAckFrame true");
        }

        return 0;
    }

    public String getFirmwareVersion() {

        pn532_packetbuffer[0] = PN532_COMMAND_GETFIRMWAREVERSION;

        if (writeCommand(pn532_packetbuffer, 1) < 0) {
            return null;
        }
        // read data packet
        int status = readResponse(pn532_packetbuffer, pn532_packetbuffer.length);

        if (0 > status) {
            return null;
        }

        String responseHex = bytesToHex(pn532_packetbuffer);
        cleanBuff();
        return "Found chip PN5" + responseHex.substring(0, 2) + ", firmware version: " + responseHex.substring(2, 4) + "," + responseHex.substring(4, 6);
    }

    boolean inListPassiveTarget() {

        pn532_packetbuffer[0] = PN532_COMMAND_INLISTPASSIVETARGET;
        pn532_packetbuffer[1] = (byte) 1;
        pn532_packetbuffer[2] = (byte) 0;

        if (DEBUG) {
            Log.d(LOGTAG, "inList passive target");
        }

        if (writeCommand(pn532_packetbuffer, 3) < 0) {
            return false;
        }

        long time = 0;
        while (availableData() <= 0 && time < avaibleDataTimeout) {
//            Log.d(LOGTAG, "No Avaible data");
            time += 500;
            sleep(500);
        }

        int status = readResponse(pn532_packetbuffer, pn532_packetbuffer.length, 3000);

        if (0 > status) {
            return false;
        }

        if (pn532_packetbuffer[0] != (byte) 1) {
            return false;
        }

        inListedTag = pn532_packetbuffer[1];
        cleanBuff();
        return true;
    }

    public boolean sendAPDU(byte cla, byte ins, byte p1, byte p2, String aid, byte le, byte[] response, int[] resp_len) {
        byte[] cmdbuf = new byte[255];
        cmdbuf[0] = cla;
        cmdbuf[1] = ins;
        cmdbuf[2] = p1;
        cmdbuf[3] = p2;
        cmdbuf[4] = (byte) aid.length();
        int i;
        for (i = 0; i < aid.length(); i++) {
            cmdbuf[5 + i] = (byte) aid.charAt(i);
        }
        cmdbuf[6 + i] = le;

        Log.d(LOGTAG, "Command array:");
        for (int j = 0; j < 5 + aid.length(); j++) {
            System.out.print(bytesToHex(new byte[]{cmdbuf[j]}));
        }
        Log.d(LOGTAG, "\nCommand array done");

        return inDataExchange(cmdbuf, 5 + aid.length(), response, resp_len);
    }

    public boolean inDataExchange(byte[] send, int sendLength, byte[] response, int[] responseLength) {

        pn532_packetbuffer[0] = PN532_COMMAND_INDATAEXCHANGE;//0x40
        pn532_packetbuffer[1] = inListedTag;

        if (writeCommand(pn532_packetbuffer, 2, send, sendLength) < 0) {
            return false;
        }

        int status = readResponse(response, responseLength[0], 1000);

        if (status < 0) {
            return false;
        }

        if ((response[0] & (byte) 0x3f) != 0) {
            if (DEBUG) {
                Log.d(LOGTAG, "Status code indicates an error");
            }
            return false;
        }

        int length = status;
        length -= 1;

        if (length > responseLength[0]) {
            length = responseLength[0]; // silent truncation...
        }

        for (int i = 0; i < length; i++) {
            response[i] = response[i + 1];
        }
        responseLength[0] = length;

        return true;
    }

    boolean readPassiveTargetID(byte cardbaudrate, byte[] uid, int[] uidLength, long timeout) {
        pn532_packetbuffer[0] = PN532_COMMAND_INLISTPASSIVETARGET;
        pn532_packetbuffer[1] = 1;  // max 1 cards at once (we can set this to 2 later)
        pn532_packetbuffer[2] = cardbaudrate;

        if (writeCommand(pn532_packetbuffer, 3) < 0) {
            return false;  // command failed
        }

        // read data packet
        if (readResponse(pn532_packetbuffer, pn532_packetbuffer.length, timeout) < 0) {
            return false;
        }

        // check some basic stuff
        /* ISO14443A card response should be in the following format:

      byte            Description
      -------------   ------------------------------------------
      b0              Tags Found
      b1              Tag Number (only one used in this example)
      b2..3           SENS_RES
      b4              SEL_RES
      b5              NFCID Length
      b6..NFCIDLen    NFCID
         */
        if (pn532_packetbuffer[0] != (byte) 1) {
            return false;
        }

        int sens_res = pn532_packetbuffer[2] & 0xFF;
        sens_res <<= 8;
        sens_res |= pn532_packetbuffer[3];


        if (DEBUG) {
            Log.d(LOGTAG, bytesToHex(new byte[]{pn532_packetbuffer[4]}));
        }


        /* Card appears to be Mifare Classic */
        uidLength[0] = pn532_packetbuffer[5] & 0xFF;

        for (int i = 0; i < pn532_packetbuffer[5]; i++) {
            uid[i] = pn532_packetbuffer[6 + i];
        }

        cleanBuff();
        return true;
    }

    boolean mifareclassic_AuthenticateBlock(byte[] uid, int blockNumber, boolean useKeyB, byte[] keyData) {
        int i;

        _key = keyData;
        _uid = uid;
        _uidLen = (byte) uid.length;

        // Prepare the authentication command //
        pn532_packetbuffer[0] = PN532_COMMAND_INDATAEXCHANGE;
        /* Data Exchange Header */
        pn532_packetbuffer[1] = 1;
        /* Max card numbers */
        pn532_packetbuffer[2] = (useKeyB) ? MIFARE_CMD_AUTH_B : MIFARE_CMD_AUTH_A;
        pn532_packetbuffer[3] = (byte) blockNumber;
        /* Block Number (1K = 0..63, 4K = 0..255 */

        for (int j = 0; j < 6; j++) {
            pn532_packetbuffer[4 + j] = _key[j];
        }

        for (i = 0; i < _uidLen; i++) {
            pn532_packetbuffer[10 + i] = _uid[i];
            /* 4 bytes card ID */
        }

        if (writeCommand(pn532_packetbuffer, (10 + (int) _uidLen)) < 0) {
            return false;
        }

        // Read the response packet
        readResponse(pn532_packetbuffer, pn532_packetbuffer.length);

        // Check if the response is valid and we are authenticated???
        // for an auth success it should be bytes 5-7: 0xD5 0x41 0x00
        // Mifare auth error is technically byte 7: 0x14 but anything other and 0x00 is not good
        if (pn532_packetbuffer[0] != 0x00) {
            if (DEBUG) {
                Log.d(LOGTAG, "Authentification failed");
            }
            return false;
        }

        return true;
    }

    private boolean mifareclassic_ReadDataBlock(int blockNumber, byte[] data) {

        if (DEBUG) {
            Log.d(LOGTAG, "Trying to read 16 bytes from block ");
        }
//    DMSG_INT(blockNumber);

        /* Prepare the command */
        pn532_packetbuffer[0] = PN532_COMMAND_INDATAEXCHANGE;
        pn532_packetbuffer[1] = 1;
        /* Card number */
        pn532_packetbuffer[2] = MIFARE_CMD_READ;
        /* Mifare Read command = 0x30 */
        pn532_packetbuffer[3] = (byte) blockNumber;
        /* Block Number (0..63 for 1K, 0..255 for 4K) */

        /* Send the command */
        if (writeCommand(pn532_packetbuffer, 4) < 0) {
            return false;
        }

        /* Read the response packet */
        readResponse(pn532_packetbuffer, pn532_packetbuffer.length);

        /* If byte 8 isn't 0x00 we probably have an error */
        if (pn532_packetbuffer[0] != 0x00) {
            return false;
        }

        /* Copy the 16 data bytes to the output buffer        */
        /* Block content starts at byte 9 of a valid response */
//        memcpy(data, pn532_packetbuffer + 1, 16);
        for (int i = 0; i < 16; i++) {
            data[i] = pn532_packetbuffer[i + 1];
        }

        return true;
    }

    private boolean mifareclassic_WriteDataBlock(int blockNumber, byte[] data) {
        /* Prepare the first command */
        pn532_packetbuffer[0] = PN532_COMMAND_INDATAEXCHANGE;
        pn532_packetbuffer[1] = 1;
        /* Card number */
        pn532_packetbuffer[2] = MIFARE_CMD_WRITE;
        /* Mifare Write command = 0xA0 */
        pn532_packetbuffer[3] = (byte) blockNumber;
        /* Block Number (0..63 for 1K, 0..255 for 4K) */

        /* Data Payload */
        System.arraycopy(data, 0, pn532_packetbuffer, 4, 16);

        /* Send the command */
        if (writeCommand(pn532_packetbuffer, 20) > 0) {
            return false;
        }

        /* Read the response packet */
        return (readResponse(pn532_packetbuffer, pn532_packetbuffer.length) > 0);
    }


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void begin() {
        try {
            serialPort.open(connection);
            serialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);


//            ioManager = new SerialInputOutputManager(serialPort, this);
//            Executors.newSingleThreadExecutor().submit(ioManager);

//            bb = ByteBuffer.allocate(1024);


            sleep(500);
//            cleanBuff();
            wakeup();
        } catch (IOException ex) {
            Log.d(LOGTAG, "SerialPort init error:" + ex.getMessage());
        }
    }

    public void stop() {
        try {
            serialPort.close();
            connection.close();
        } catch (IOException ex) {
            Log.d(LOGTAG, "SerialPort stop error:" + ex.getMessage());
        }
    }

    public void readData(long timeout) {

        try {
            long time = 0;
            int readCount = 0;
            byte[] tmp = new byte[512];
            while (timeout - time > 0) {

                readCount = serialPort.read(tmp, (timeout == 0 ? 0 : 50));

                if (readCount == 0) {
                    break;
                }

                System.arraycopy(tmp, 0, buff, buffSize, readCount);
                buffSize += readCount;

//                Log.d(LOGTAG, "Read time:" + time + ", size: " + readCount + " data:" + bytesToHex(tmp));

                sleep(10);
                time += 10;
            }
        } catch (IOException e) {
            Log.d(LOGTAG, "Read error:", e);
        }
//        Log.d(LOGTAG, "BUFF  size: " + buffSize + " data:" + bytesToHex(buff));
    }


    public void wakeup() {

        try {
            byte[] bb = new byte[]{0x55, 0x55, 0x0, 0x0, 0x0};
            serialPort.write(bb, serialPortTimeout);

//            readData();
        } catch (IOException ex) {
            Log.d(LOGTAG, "Wakeup error:" + ex);
        }
    }

    private int writeCommand(byte[] header, int hlen, byte[] body, int blen) {

        try {
            // dump data
//            readData();

            command = header[0];
            ByteBuffer bb = ByteBuffer.allocate(8 + hlen);

            bb.put(PN532_PREAMBLE);
            bb.put(PN532_STARTCODE1);
            bb.put(PN532_STARTCODE2);

            byte length = (byte) (hlen + blen + 1);  // length of data field: TFI + DATA
            bb.put(length);
            bb.put((byte) ((~length) + 1));          // checksum of length

            bb.put(PN532_HOSTTOPN532);
            byte sum = PN532_HOSTTOPN532;            // sum of TFI + DATA

            if (DEBUG) {
                Log.d(LOGTAG, "\nWrite: ");
            }

            for (int i = 0; i < hlen; i++) {
                bb.put(header[i]);
                sum += header[i];
//                DMSG(header[i]);
            }

            for (int i = 0; i < blen; i++) {
                bb.put(body[i]);
                sum += body[i];
//                DMSG(body[i]);
            }

            byte checksum = (byte) (~sum + 1);      // checksum of TFI + DATA
            bb.put(checksum);
            bb.put(PN532_POSTAMBLE);

            serialPort.write(bb.array(), serialPortTimeout);

        } catch (IOException ex) {
            Log.d(LOGTAG, "writeCommand error:" + ex);
        }

        return readAckFrame();
//        return 0;
    }


//    private int receive(byte[] buf, int len, long timeout) {
//
//        if (len == 0) {
//            if (DEBUG) {
//                Log.d(LOGTAG, "Receive: index: " + buffSize + ", Data size: " + len + ", Data: [00]");
//            }
//            return 0;
//        }
//
//        Arrays.fill(buf, 0, buf.length, (byte) 0);
//
//        if (buff.length >= len) {
//            System.arraycopy(buff, buffSize, buf, 0, len);
//            buffSize += len;
//        } else {
//            Log.d(LOGTAG, "Receive: return 0");
//            return 0;
//        }
//
//        if (DEBUG) {
//            Log.d(LOGTAG, "Receive: index: " + buffSize + ", Data size: " + len + ", Data:" + bytesToHex(buf));
//        }
//        return len;
//
//    }

    private int receive(byte[] buf, int len, long timeout) {

        if (len == 0) {
            if (DEBUG) {
                Log.d(LOGTAG, "Receive: index: " + buffSize + ", Data size: " + len + ", Data: [00]");
            }
            return 0;
        }

        Arrays.fill(buf, 0, buf.length, (byte) 0);

        if ((buffSize - buffReadIndex) >= len) {
            System.arraycopy(buff, buffReadIndex, buf, 0, len);
            buffReadIndex += len;
//            Log.d(LOGTAG, "Receive - Size: " + len + ", DATA:" + bytesToHex(buf));
        } else {
            Log.d(LOGTAG, "Receive: return 0");
            return 0;
        }

        if (DEBUG) {
            Log.d(LOGTAG, "Receive: index: " + buffSize + ", Data size: " + len + ", Data:" + bytesToHex(buf));
        }

        return len;
    }

    private void waiteReceiveBytes(long timeout) {
        while (buffSize <= 0) {
            sleep(100);
        }
    }

    public void cleanBuff() {
        Arrays.fill(buff, 0, buff.length, (byte) 0);
        buffSize = 0;
        buffReadIndex = 0;
    }

    public int availableData() {
        readData(500);
        return buffSize - buffReadIndex;
    }

//    @Override
//    public void onNewData(byte[] data) {
//        int dataL = data.length;
//        System.arraycopy(data, 0, buff, buffSize, dataL);
//        buffSize += dataL;
//        Log.d(LOGTAG, "onNewData - Size: " + dataL + ", DATA:" + bytesToHex(data));
//    }
//
//    @Override
//    public void onRunError(Exception e) {
//
//    }


    public String mifareClassicReadData(byte[] uid, int startBlock, boolean useKeyB, byte[] key) {

        byte[] data;
        byte[] blockData = new byte[16];
        int dataIndex = 0;
        int dataSize;
        int blocks;
        int checksum = 0;

        if (mifareclassic_AuthenticateBlock(uid, startBlock, useKeyB, key)) {

            if (mifareclassic_ReadDataBlock(startBlock, blockData)) {

                dataSize = (int) (blockData[0] & 0xFF);
                blocks = (dataSize % 16) == 0 ? (dataSize / 16) : (dataSize / 16) + 1;
                data = new byte[16 * blocks];
                System.arraycopy(blockData, 1, data, dataIndex, 15);
                dataIndex += 15;

                for (int block = startBlock + 1; block < blocks + startBlock; block++) {

                    if (mifareclassic_AuthenticateBlock(uid, startBlock, useKeyB, key)) {

                        if (mifareclassic_ReadDataBlock(block, blockData)) {
                            System.arraycopy(blockData, 0, data, dataIndex, 16);
                            dataIndex += 16;
                        }
                    }
                }

                if (DEBUG) {
                    System.out.println("Data size:" + dataSize + ", Data blocks: " + blocks + ", All Data: " + bytesToHex(data));
                    System.out.println("All Data string: " + new String(data));
                }

                for (int i = 0; i < dataSize - 1; i++) {
                    checksum += data[i];
                }

                if (0 != (byte) (checksum + data[dataSize - 1])) {
                    System.out.println("MifareClassicReadData: CHECK SUMM FALSE");
                    return null;
                }

                String result = new String(data).trim();
                return result.substring(0, result.length() - 1);
            }
        }
        return null;
    }

    public boolean mifareClassicWriteData(byte[] uid, int startBlock, boolean useKeyB, byte[] key, String data) {

        if (data == null || data.equals("")) {
            return false;
        }

        if ((data.length() - 2) > MAX_DATA_SIZE_CLASSIC) {
            Log.d(LOGTAG, "MifareClassic: data size out of range");
            return false;
        }
        boolean success = false;

        byte[] blockData = new byte[16];
        byte[] dataBytes = data.getBytes(Charset.forName("UTF-8"));
        int dataSize = dataBytes.length;
        int blocks = ((dataSize + 2) % 16) == 0 ? ((dataSize + 2) / 16) : ((dataSize + 2) / 16) + 1;
        int allDataIndex = 0;

        byte[] allData = new byte[blocks * 16];
        allData[0] = (byte) (dataSize + 1);
        System.arraycopy(dataBytes, 0, allData, 1, dataSize);

        byte checksum = 0;
        for (int i = 0; i < dataSize; i++) {
            checksum += dataBytes[i];
        }
        allData[dataSize + 1] = (byte) (~checksum + 1);

        for (int block = startBlock; block < blocks + startBlock; block++) {
            if (mifareclassic_AuthenticateBlock(uid, block, useKeyB, key)) {
                System.arraycopy(allData, allDataIndex, blockData, 0, 16);
                success = mifareclassic_WriteDataBlock(block, blockData);
                allDataIndex += 16;
                if (DEBUG) {
                    System.out.println("Write block: " + block + ", data: " + bytesToHex(blockData));
                }
            }
        }
        return success;
    }

    private boolean mifareultralight_ReadPage(int page, byte[] buffer) {
        if (page >= 64) {
            Log.d(LOGTAG, "Page value out of range\n");
            return false;
        }
        if (buffer.length > 4) {
            Log.d(LOGTAG, "Buffer size out of range\n");
            return false;
        }

        /* Prepare the command */
        pn532_packetbuffer[0] = PN532_COMMAND_INDATAEXCHANGE;
        pn532_packetbuffer[1] = 1;
        /* Card number */
        pn532_packetbuffer[2] = MIFARE_CMD_READ;
        /* Mifare Read command = 0x30 */
        pn532_packetbuffer[3] = (byte) page;
        /* Page Number (0..63 in most cases) */

        /* Send the command */
        if (writeCommand(pn532_packetbuffer, 4) > 0) {
            return false;
        }

        /* Read the response packet */
        readResponse(pn532_packetbuffer, pn532_packetbuffer.length);

        /* If byte 8 isn't 0x00 we probably have an error */
        if (pn532_packetbuffer[0] == 0x00) {
            /* Copy the 4 data bytes to the output buffer         */
            /* Block content starts at byte 9 of a valid response */
            /* Note that the command actually reads 16 bytes or 4  */
            /* pages at a time ... we simply discard the last 12  */
            /* bytes                                              */
            System.arraycopy(pn532_packetbuffer, 1, buffer, 0, buffer.length);
//        memcpy (buffer, pn532_packetbuffer + 1, 4);
        } else {
            return false;
        }

        // Return OK signal
        return true;
    }

    private boolean mifareultralight_WritePage(int page, byte[] buffer) {
        /* Prepare the first command */
        pn532_packetbuffer[0] = PN532_COMMAND_INDATAEXCHANGE;
        pn532_packetbuffer[1] = 1;
        /* Card number */
        pn532_packetbuffer[2] = (byte) MIFARE_CMD_WRITE_ULTRALIGHT;
        /* Mifare UL Write cmd = 0xA2 */
        pn532_packetbuffer[3] = (byte) page;
        /* page Number (0..63) */

//        memcpy(pn532_packetbuffer + 4, buffer, 4);
        System.arraycopy(buffer, 0, pn532_packetbuffer, 4, buffer.length);
        /* Data Payload */

        /* Send the command */
        if (writeCommand(pn532_packetbuffer, 8) > 0) {
            return false;
        }

        /* Read the response packet */
        return (0 < readResponse(pn532_packetbuffer, pn532_packetbuffer.length));
    }

    public boolean mifareUltralightWriteData(int startBlock, String data) {

        if (data == null || data.equals("")) {
            return false;
        }

        if ((data.length() - 2) > MAX_DATA_SIZE_ULTRALIGHT) {
            Log.d(LOGTAG, "MifareUltralight: data size out of range");
            return false;
        }

        boolean success = false;
        byte[] dataBytes = data.getBytes(Charset.forName("UTF-8"));

        int dataSize = dataBytes.length;
        int allDataSize = dataSize + 2;
        int allDataIndex = 0;
        int blocks = ((dataSize + 2) % 4) == 0 ? ((dataSize + 2) / 4) : ((dataSize + 2) / 4) + 1;

        byte[] blockData = new byte[4];
        byte[] allData = new byte[blocks * 4];

        allData[0] = (byte) (dataSize + 1);
        System.arraycopy(dataBytes, 0, allData, 1, dataSize);

        byte checksum = 0;
        for (int i = 1; i < allDataSize - 1; i++) {
            checksum += allData[i];
        }
        allData[allDataSize - 1] = (byte) (~checksum + 1);

        if (DEBUG) {
            Log.d(LOGTAG, "Mifare Ultralight data size:" + dataSize + ", str:" + data + ", buffer size:" + allDataSize + ", buffer hex: " + bytesToHex(allData) + ", blocks:" + blocks);
        }

        for (int block = startBlock; block < blocks + startBlock; block++) {
            System.arraycopy(allData, allDataIndex, blockData, 0, blockData.length);
            success = mifareultralight_WritePage(block, blockData);
            allDataIndex += 4;
            if (DEBUG) {
                System.out.println("Write block: " + block + ", data: " + bytesToHex(blockData));
            }
        }

        return success;
    }

    public String mifareUltralightReadData(int startBlock) {

        byte[] data;
        byte[] blockData = new byte[4];
        int dataIndex = 0;
        int dataSize;
        int blocks;
        int checksum = 0;

        if (mifareultralight_ReadPage(startBlock, blockData)) {

            dataSize = (int) (blockData[0] & 0xFF);
            blocks = (dataSize % 4) == 0 ? (dataSize / 4) : (dataSize / 4) + 1;
            data = new byte[4 * blocks];
            System.arraycopy(blockData, 1, data, dataIndex, 3);
            dataIndex += 3;

            for (int block = startBlock + 1; block < blocks + startBlock; block++) {
                if (mifareultralight_ReadPage(block, blockData)) {
                    System.arraycopy(blockData, 0, data, dataIndex, 4);
                    dataIndex += 4;
                }
            }

            for (int i = 0; i < dataSize - 1; i++) {
                checksum += data[i];
            }

            if (0 != (byte) (checksum + data[dataSize - 1])) {
                Log.d(LOGTAG, "MifareUltralightReadData: CHECK SUMM FALSE");
                return null;
            }

            String result = new String(data).trim();
            return result.substring(0, result.length() - 1);
        }

        return null;
    }

}
