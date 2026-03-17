//Challenge 2: ChaCha20-Poly1305
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

class hjStreamServer {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Error, use: mySend <movie> <ip-multicast-address> <port>");
            System.out.println("or: mySend <movie> <ip-unicast-address> <port>");
            System.exit(-1);
        }

        int frameSize;
        int totalBytesSent = 0;
        int frameCount = 0;
        long frameTimestamp;

        DataInputStream movieInput = new DataInputStream(new FileInputStream(args[0]));
        byte[] frameBuffer = new byte[4096];
        //envia e recebe Sockets
        DatagramSocket udpSocket = new DatagramSocket();
        InetSocketAddress destinationAddress = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
        //chave fixa para ChaCha20-Poly1305
        byte[] keyBytes = "0123456789abcdef0123456789abcdef".getBytes();
        SecretKeySpec chachaKey = new SecretKeySpec(keyBytes, "ChaCha20");

        //packet UDP
        DatagramPacket udpPacket = new DatagramPacket(frameBuffer, frameBuffer.length, destinationAddress);

        long startNanoTime = System.nanoTime();
        long streamStartTimestamp = 0;

        SecureRandom secureRandom = new SecureRandom();

        //enquanto existir dados vai enviar frame a frame
        while (movieInput.available() > 0) {
            frameSize = movieInput.readShort();
            totalBytesSent = totalBytesSent + frameSize;
            frameTimestamp = movieInput.readLong();
            if (frameCount == 0) streamStartTimestamp = frameTimestamp;
            frameCount += 1;

            //le os bytes do frame
            movieInput.readFully(frameBuffer, 0, frameSize);

            //nonce por frame
            byte[] nonce = new byte[12];
            secureRandom.nextBytes(nonce);

            //encripta o frame
            Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
            IvParameterSpec ivSpec = new IvParameterSpec(nonce);
            cipher.init(Cipher.ENCRYPT_MODE, chachaKey, ivSpec);

            //ciphertext + tag
            byte[] cipherText = cipher.doFinal(frameBuffer, 0, frameSize);

            //formato= nonce(12) + ciphertext
            byte[] packetData = new byte[nonce.length + cipherText.length];
            System.arraycopy(nonce, 0, packetData, 0, nonce.length);
            System.arraycopy(cipherText, 0, packetData, nonce.length, cipherText.length);

            //atualiza o packet
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
}

