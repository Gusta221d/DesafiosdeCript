//Challenge 3: proxy DPRG e XOR
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

class hjUDPproxyDPRG {
    //secret key partilhada para DPRG
    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes();

    public static void main(String[] args) throws Exception {
        InputStream configInput = openConfig();

        Properties config = new Properties();
        config.load(configInput);

        String remoteEndpoint = config.getProperty("remote");
        String localDeliveryEndpoints = config.getProperty("localdelivery");

        SocketAddress listenAddress = parseSocketAddress(remoteEndpoint);
        Set<SocketAddress> forwardAddressSet = Arrays.stream(localDeliveryEndpoints.split(","))
                .map(s -> parseSocketAddress(s))
                .collect(Collectors.toSet());

        //envia e recebe Sockets
        DatagramSocket listenSocket = new DatagramSocket(listenAddress);
        DatagramSocket forwardSocket = new DatagramSocket();

        //buffer de rececao
        byte[] packetBuffer = new byte[4 * 1024];

        while (true) {
            //recebe o datagrama encriptado
            DatagramPacket receivedPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
            listenSocket.receive(receivedPacket);

            System.out.print(".");
            //copia apenas os bytes recebidos
            byte[] receivedData = Arrays.copyOf(receivedPacket.getData(), receivedPacket.getLength());
            //le o frameCounter e o cipherFrame
            int frameCounter = readIntBigEndian(receivedData, 0);
            byte[] cipherFrame = Arrays.copyOfRange(receivedData, 4, receivedData.length);
            //desencripta com XOR 
            byte[] plainFrame = xorWithKeyStream(cipherFrame, frameCounter);
            //reencaminha para cada destino final
            for (SocketAddress forwardAddress : forwardAddressSet) {
                DatagramPacket forwardPacket = new DatagramPacket(plainFrame, plainFrame.length, forwardAddress);
                forwardSocket.send(forwardPacket);
            }
        }
    }
    //XOR com keystream gerado por SHA-256 = (key||frameCounter||blockIndex)
    private static byte[] xorWithKeyStream(byte[] cipherFrame, int frameCounter) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] out = new byte[cipherFrame.length];
        int outPos = 0;
        int blockIndex = 0;

        while (outPos < cipherFrame.length) {
            digest.reset();
            digest.update(KEY);
            digest.update(intToBytes(frameCounter));
            digest.update(intToBytes(blockIndex));
            byte[] block = digest.digest();

            int blockLen = Math.min(block.length, cipherFrame.length - outPos);
            for (int i = 0; i < blockLen; i++) {
                out[outPos + i] = (byte) (cipherFrame[outPos + i] ^ block[i]);
            }
            outPos += blockLen;
            blockIndex += 1;
        }

        return out;
    }
    private static int readIntBigEndian(byte[] src, int offset) {
        int b0 = (src[offset] & 0xFF) << 24;
        int b1 = (src[offset + 1] & 0xFF) << 16;
        int b2 = (src[offset + 2] & 0xFF) << 8;
        int b3 = (src[offset + 3] & 0xFF);
        return b0 | b1 | b2 | b3;
    }
    //converte IP em InetSocketAddress
    private static InetSocketAddress parseSocketAddress(String endpoint) {
        String[] split = endpoint.split(":");
        String host = split[0]; 
        int port = Integer.parseInt(split[1]); 
        return new InetSocketAddress(host, port); 
    }
    private static byte[] intToBytes(int value) {
        byte[] b = new byte[4];
        b[0] = (byte) ((value >> 24) & 0xFF);
        b[1] = (byte) ((value >> 16) & 0xFF);
        b[2] = (byte) ((value >> 8) & 0xFF);
        b[3] = (byte) (value & 0xFF);
        return b;
    }
    //abre config.properties a partir de diferentes caminhos
    private static InputStream openConfig() throws FileNotFoundException {
        String[] candidates = new String[] {
                "config.properties",
                "..\\config.properties",
                "..\\hjUDPproxy\\config.properties",
                "..\\..\\hjUDPproxy\\config.properties"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                System.out.println("Using config: " + f.getPath());
                return new FileInputStream(f);
            }
        }
        throw new FileNotFoundException("config.properties (not found)");
    }
}

