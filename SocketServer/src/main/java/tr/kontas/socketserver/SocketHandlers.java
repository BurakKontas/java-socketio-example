package tr.kontas.socketserver;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import io.netty.handler.codec.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SocketHandlers {

    @OnConnect
    public void onConnect(SocketIOClient client) {
        System.out.println("Client connected: " + client.getSessionId());

        Map<String, List<String>> queryParams = client.getHandshakeData().getUrlParams();
        HttpHeaders headers = client.getHandshakeData().getHttpHeaders();

        System.out.println("📋 Query Parameters: " + queryParams);
        System.out.println("📋 Headers: " + headers);

        client.sendEvent("welcome", "connected to server");
    }

    @OnDisconnect
    private void onDisconnect(SocketIOClient client) {
        System.out.println("Client disconnected: " + client.getSessionId());
    }

    @OnEvent("ping")
    public void onPing(SocketIOClient client, String msg, AckRequest ackRequest) {
        System.out.println("Ping: " + msg);

        Map<String, List<String>> urlParams = client.getHandshakeData().getUrlParams();
        HttpHeaders headers = client.getHandshakeData().getHttpHeaders();

        System.out.println("📋 Query Parameters: " + urlParams);
        System.out.println("📋 Headers: " + headers);

        if (ackRequest.isAckRequested()) {
            ackRequest.sendAckData("received:" + msg,
                    Map.of("headers", headers.toString(), "params", urlParams));
        }
    }
}