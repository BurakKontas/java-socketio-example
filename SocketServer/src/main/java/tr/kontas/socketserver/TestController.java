package tr.kontas.socketserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/health")
    public String health() {
        return "Server is running!";
    }

    @GetMapping("/test-ping")
    public String testPing() {
        return "Use Socket.IO client to test ping/pong";
    }
}