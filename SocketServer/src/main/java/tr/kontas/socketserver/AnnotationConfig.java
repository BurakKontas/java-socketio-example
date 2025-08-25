package tr.kontas.socketserver;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnnotationConfig {

    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer server) {
        return new SpringAnnotationScanner(server);
    }
}