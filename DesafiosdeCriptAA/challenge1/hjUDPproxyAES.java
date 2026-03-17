//Challenge 1: proxy AES-GCM
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class hjUDPproxyAES {
    public static void main(String[] args) throws Exception {
        InputStream configInput = openConfig();

        Properties config = new Properties();
        config.load(configInput);

        //AES key
        byte[] keyBytes = "0123456789abcdef".getBytes();
        SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");

        //endpoint
        String remoteEndpoint = config.getProperty("remote");
        String localDeliveryEndpoints = config.getProperty("localdelivery");

        //converte o IP em endereco
        SocketAddress listenAddress = parseSocketAddress(remoteEndpoint);
        Set<SocketAddress> forwardAddressSet = Arrays.stream(localDeliveryEndpoints.split(","))
                .map(s -> parseSocketAddress(s))
                .collect(Collectors.toSet());

        //recebe e envia sockets
        DatagramSocket listenSocket = new DatagramSocket(listenAddress);
        DatagramSocket forwardSocket = new DatagramSocket();

        byte[] packetBuffer = new byte[4 * 1024];

        while (true) {
            //recebe o datagrama encriptado
            DatagramPacket receivedPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
            listenSocket.receive(receivedPacket);

            System.out.print(".");

            for (SocketAddress forwardAddress : forwardAddressSet) {
                //copia apenas os bytes recebidos
                byte[] receivedData = Arrays.copyOf(receivedPacket.getData(), receivedPacket.getLength());
                //formato= iv(12) + ciphertext+tag
                byte[] iv = Arrays.copyOfRange(receivedData, 0, 12);
                byte[] cipherText = Arrays.copyOfRange(receivedData, 12, receivedData.length);
                //desencripta com AES-GCM
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
                cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

                byte[] plainText = cipher.doFinal(cipherText);
                //reencaminha para cada destino final
                DatagramPacket forwardPacket = new DatagramPacket(plainText, plainText.length, forwardAddress);
                forwardSocket.send(forwardPacket);
            }
        }
    }
    //converte IP em InetSocketAddress
    private static InetSocketAddress parseSocketAddress(String endpoint) {
        String[] split = endpoint.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
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

        throw new FileNotFoundException("config.properties (not found )");
    }
}

