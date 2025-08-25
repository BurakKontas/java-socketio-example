package tr.kontas.socketserver;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@org.springframework.context.annotation.Configuration
public class SocketConfig {

    @Value("${socketio.host:0.0.0.0}")
    private String host;

    @Value("${socketio.port:7083}")
    private int port;

    @Value("${keystore.path}")
    private String keyStorePath;

    @Value("${keystore.password}")
    private String keyStorePassword;

    @Bean(destroyMethod = "stop")
    public SocketIOServer socketIOServer() throws FileNotFoundException {
        Configuration cfg = new Configuration();
        cfg.setHostname(host);
        cfg.setPort(port);

        cfg.setKeyStorePassword(keyStorePassword);
        cfg.setKeyStore(new FileInputStream(keyStorePath));
        cfg.setKeyStoreFormat("PKCS12");

        cfg.setOrigin("*");

        cfg.setBossThreads(1);
        cfg.setWorkerThreads(1);

        SocketIOServer server = new SocketIOServer(cfg);
        server.start();

        System.out.println("Socket.IO server started on " + host + ":" + port);
        System.out.println("SSL enabled: " + (keyStorePath != null));

        return server;
    }
}