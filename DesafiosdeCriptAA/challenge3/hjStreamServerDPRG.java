//Challenge 3: DPRG e XOR 
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.security.MessageDigest;

class hjStreamServerDPRG {
    //secret key partilhada para DPRG
    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes();

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Error, use: mySend <movie> <ip-multicast-address> <port>");
            System.out.println("ou: mySend <movie> <ip-unicast-address> <port>");
            System.exit(-1);
        }

        int frameSize;
        int totalBytesSent = 0;
        int frameCount = 0;
        long frameTimestamp;

        DataInputStream movieInput = new DataInputStream(new FileInputStream(args[0]));
        byte[] frameBuffer = new byte[4096];

        DatagramSocket udpSocket = new DatagramSocket();
        InetSocketAddress destinationAddress = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

        DatagramPacket udpPacket = new DatagramPacket(frameBuffer, frameBuffer.length, destinationAddress);

        long startNanoTime = System.nanoTime();
        long streamStartTimestamp = 0;

        int frameCounter = 0;

        //enquanto existir dados vai enviar frame a frame
        while (movieInput.available() > 0) {
            frameSize = movieInput.readShort();
            totalBytesSent = totalBytesSent + frameSize;
            frameTimestamp = movieInput.readLong();
            if (frameCount == 0) streamStartTimestamp = frameTimestamp;
            frameCount += 1;
            //le os bytes do frame
            movieInput.readFully(frameBuffer, 0, frameSize);
            //faz a incrementação do contador de frame
            frameCounter++;
            //encripta com XOR
            byte[] cipherFrame = xorWithKeyStream(frameBuffer, frameSize, frameCounter);

            //formato do packet (frameCounter(4 bytes)) + cipherFrame
            byte[] packetData = new byte[4 + cipherFrame.length];
            writeIntBigEndian(packetData, 0, frameCounter);
            System.arraycopy(cipherFrame, 0, packetData, 4, cipherFrame.length);

            //atualiza o pacote
            udpPacket.setData(packetData, 0, packetData.length);
            udpPacket.setSocketAddress(destinationAddress);

            long nowNanoTime = System.nanoTime();
            Thread.sleep(Math.max(0, ((frameTimestamp - streamStartTimestamp) - (nowNanoTime - startNanoTime)) / 1000000));
            //envia por UDP
            udpSocket.send(udpPacket);

            System.out.print(".");
        }

        long endNanoTime = System.nanoTime();
        System.out.println();
        System.out.println("DONE! all frames sent: " + frameCount);

        long duration = (endNanoTime - startNanoTime) / 1000000000;
        System.out.println("Movie duration " + duration + " s");
        System.out.println("Throughput " + frameCount / duration + " fps");
        System.out.println("Throughput " + (8 * (totalBytesSent) / duration) / 1000 + " Kbps");
    }

    //XOR com keystream gerado por SHA-256 = (key||frameCounter||blockIndex)
    private static byte[] xorWithKeyStream(byte[] plain, int length, int frameCounter) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] out = new byte[length];
        int outPos = 0;
        int blockIndex = 0;

        while (outPos < length) {
            digest.reset();
            digest.update(KEY);
            digest.update(intToBytes(frameCounter));
            digest.update(intToBytes(blockIndex));
            byte[] block = digest.digest();

            int blockLen = Math.min(block.length, length - outPos);
            for (int i = 0; i < blockLen; i++) {
                out[outPos + i] = (byte) (plain[outPos + i] ^ block[i]);
            }
            outPos += blockLen;
            blockIndex += 1;
        }
        return out;
    }
    //escreve int em endian num array
    private static void writeIntBigEndian(byte[] dst, int offset, int value) {
        dst[offset] = (byte) ((value >> 24) & 0xFF);
        dst[offset + 1] = (byte) ((value >> 16) & 0xFF);
        dst[offset + 2] = (byte) ((value >> 8) & 0xFF);
        dst[offset + 3] = (byte) (value & 0xFF);
    }
    private static byte[] intToBytes(int value) {
        byte[] b = new byte[4];
        writeIntBigEndian(b, 0, value);
        return b;
    }
}

