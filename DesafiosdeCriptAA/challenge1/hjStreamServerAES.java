//Challenge 1:AES-GCM
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

class hjStreamServerAES {
    public static void main(String[] args) throws Exception {
        //valida argumentos
        if (args.length != 3) {
            System.out.println("Error, use: mySend <movie> <ip-multicast-address> <port>");
            System.out.println("ou: mySend <movie> <ip-unicast-address> <port>");
            System.exit(-1);
        }

        int frameSize;         
        int totalBytesSent = 0;
        int frameCount = 0;
        long frameTimestamp;

        //abre o ficheiro do filme
        DataInputStream movieInput = new DataInputStream(new FileInputStream(args[0]));
        byte[] frameBuffer = new byte[4096];

        //socket UDP e o destino
        DatagramSocket udpSocket = new DatagramSocket();
        InetSocketAddress destinationAddress = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

        //AES key
        byte[] keyBytes = "0123456789abcdef".getBytes();
        SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");

        //pacote UDP
        DatagramPacket udpPacket = new DatagramPacket(frameBuffer, frameBuffer.length, destinationAddress);

        long startNanoTime = System.nanoTime();
        long streamStartTimestamp = 0;

        SecureRandom secureRandom = new SecureRandom();

        //enviar frame a frame
        while (movieInput.available() > 0) {
            frameSize = movieInput.readShort();
            totalBytesSent = totalBytesSent + frameSize;
            frameTimestamp = movieInput.readLong();
            if (frameCount == 0) streamStartTimestamp = frameTimestamp;
            frameCount += 1;
            //le os bytes do frame
            movieInput.readFully(frameBuffer, 0, frameSize);
            //IV por frame
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            //encriptar o frame com AES-GCM
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); //tag 128 bits
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
            //ciphertext + tag
            byte[] cipherText = cipher.doFinal(frameBuffer, 0, frameSize);
            //formato final= iv(12) + (ciphertext+tag)
            byte[] packetData = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, packetData, 0, iv.length);
            System.arraycopy(cipherText, 0, packetData, iv.length, cipherText.length);
            //atualiza o pacote
            udpPacket.setData(packetData, 0, packetData.length);
            udpPacket.setSocketAddress(destinationAddress);

            long nowNanoTime = System.nanoTime();
            Thread.sleep(Math.max(0, ((frameTimestamp - streamStartTimestamp) - (nowNanoTime - startNanoTime)) / 1000000));
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

