package tr.kontas;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Main {

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("application.properties file not found in classpath");
            }
            props.load(input);
        }
        return props;
    }

    private static class HostnameVerifierImpl implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
            return "localhost".equals(hostname) || "127.0.0.1".equals(hostname) || "::1".equals(hostname);
        }
    }

    public static void main(String[] args) throws Exception {
        // Properties dosyasını yükle
        Properties props = loadProperties();

        String keyStorePath = props.getProperty("keystore.path");
        String keyStorePassword = props.getProperty("keystore.password");
        String socketIoHost = props.getProperty("socketio.host");
        int socketIoPort = Integer.parseInt(props.getProperty("socketio.port"));
        boolean useSsl = Boolean.parseBoolean(props.getProperty("socketio.ssl.enable", "true"));

        URI socketIoUri = URI.create((useSsl ? "https" : "http") + "://" + socketIoHost + ":" + socketIoPort);

        if (keyStorePath == null || keyStorePassword == null || socketIoHost == null) {
            throw new IllegalArgumentException("Required properties not found in application.properties");
        }

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            trustStore.load(fis, keyStorePassword.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        HostnameVerifier hostnameVerifier = new HostnameVerifierImpl();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                .hostnameVerifier(hostnameVerifier)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Map<String, List<String>> extraHeaders = new HashMap<>();
        extraHeaders.put("User-Agent", List.of("Java-SocketIO-Client/1.0"));
        extraHeaders.put("X-Custom-Header", List.of("custom-value"));
        extraHeaders.put("Accept", List.of("application/json"));

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("client", "java");
        queryParams.put("version", "1.0");
        queryParams.put("timestamp", String.valueOf(System.currentTimeMillis()));

        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        opts.reconnection = true;
        opts.transports = new String[]{"websocket"};
        opts.callFactory = okHttpClient;
        opts.webSocketFactory = okHttpClient;
        opts.hostname = socketIoHost;
        opts.port = socketIoPort;
        opts.secure = true;
        opts.timestampParam = "t";
        opts.timestampRequests = true;
        opts.extraHeaders = extraHeaders;
        opts.query = String.join("&", queryParams.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList());

        Socket socket = IO.socket(socketIoUri, opts);

        socket.emit("ping", "hello", (Ack) ackArgs -> {
            if (ackArgs != null && ackArgs.length > 0) {
                System.out.println("✅ Server Ack Response:");
                System.out.println("   Number of arguments: " + ackArgs.length);
                System.out.println("   ----------------------------");

                for (int i = 0; i < ackArgs.length; i++) {
                    Object arg = ackArgs[i];
                    String type = (arg != null) ? arg.getClass().getSimpleName() : "null";
                    System.out.println("   [" + i + "] " + type + ": " + arg);
                }
                System.out.println("   ----------------------------");
            } else if (ackArgs != null) {
                System.out.println("ℹ️  Server Ack Response: Empty array (0 arguments)");
            } else {
                System.out.println("❌ Server Ack Response: null");
            }
        });

        socket.on("pong", args1 -> System.out.println("pong: " + args1[0]));
        socket.on("welcome", args1 -> System.out.println("welcome: " + args1[1]));
        socket.on(Socket.EVENT_CONNECT_ERROR, args1 -> System.err.println("connect_error: " + args1[0]));
        socket.on(Socket.EVENT_DISCONNECT, args1 -> System.out.println("disconnected: " + args1[0]));

        socket.connect();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            socket.disconnect();
            socket.close();
        }));
    }
}