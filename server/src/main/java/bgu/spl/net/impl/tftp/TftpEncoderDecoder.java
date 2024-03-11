package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.sql.SQLOutput;
import java.util.LinkedList;
import java.util.List;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder
    private List<Byte> Bytes;
    private short opcode;

    public TftpEncoderDecoder() {
        this.Bytes = new LinkedList<Byte>();
    }

    @Override
    //Decodes next byte into an array
    public byte[] decodeNextByte(byte nextByte) {
        // TODO: implement this
        //The first byte is zero
        if (Bytes.isEmpty()) {
            Bytes.add(nextByte);
            return null;
        } else
            Bytes.add(nextByte);

        if (Bytes.size() == 2) {
            byte[] b = new byte[]{Bytes.get(0), Bytes.get(1)};
            this.opcode = (short) (((short) b[0] & 0xFF) << 8 | (short) (b[1] & 0xFF));
        }
        switch (opcode) {
            case (3):
                if (Bytes.size() > 3) {
                    byte[] s = new byte[]{Bytes.get(2), Bytes.get(3)};
                    int packetsize = (short) (((short) s[0] & 0xFF) << 8 | (short) (s[1] & 0xFF));
                    if (Bytes.size() == packetsize + 6)
                        return BytesArray();
                    else
                        return null;
                }

            case (4):
                if (Bytes.size() == 4)
                    return BytesArray();
                else
                    return null;

            case (10):
                if (Bytes.size() == 2)
                    return BytesArray();
                else
                    return null;
            case (6):
                if (Bytes.size() == 2)
                    return BytesArray();
                else
                    return null;
        }

        if (nextByte == 0)
            return BytesArray();

        return null;
    }

    //Encodes msg
    @Override
    public byte[] encode(byte[] message) {
        //TODO: implement this
        return message;
    }

    public byte[] BytesArray()
    {
        byte[] result = new byte[this.Bytes.size()];
        int i = 0;
        for (Byte b : Bytes)
        {
            result[i] = b;
            i++;
        }

        this.Bytes.clear();
        this.opcode = -1;
        return result;
    }
}