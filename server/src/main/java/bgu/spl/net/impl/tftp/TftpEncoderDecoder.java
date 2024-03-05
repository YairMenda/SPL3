package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.util.LinkedList;
import java.util.List;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder
    private List<Byte> Bytes;

    public TftpEncoderDecoder()
    {
        this.Bytes = new LinkedList<Byte>();
    }

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // TODO: implement this
        //The first byte is zero
        if (Bytes.isEmpty()) {
            Bytes.add(nextByte);
            return null;
        }

        else
            if (nextByte == '0' | (Bytes.size() == 1 & (nextByte == 'a' | nextByte == '6'))){
                Bytes.add(nextByte);
                return BytesArray();
            }
            else {Bytes.add(nextByte);}

        return null;
    }

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
        return result;
    }
}