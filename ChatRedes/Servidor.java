import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    private static final int PORTA = 5000;
    // Lista thread-safe para guardar os canais de saída de todos os clientes TCP conectados
    private static final List<PrintWriter> clientesTCP = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try {
            String ipLocal = InetAddress.getLocalHost().getHostAddress();
            System.out.println("=== SERVIDOR INICIADO ===");
            System.out.println("IPSERV: " + ipLocal + " | PORT: " + PORTA);
            System.out.println("Aguardando conexoes TCP e pacotes UDP...\n");
        } catch (UnknownHostException e) {
            System.out.println("Servidor iniciado na porta " + PORTA);
        }

        // Thread 1: Escuta pacotes UDP
        new Thread(Servidor::iniciarServidorUDP).start();

        // Thread Principal: Escuta conexões TCP
        iniciarServidorTCP();
    }

    private static void iniciarServidorTCP() {
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                System.out.println("[TCP] Novo cliente conectado: " + clienteSocket.getInetAddress().getHostAddress());
                
                // Cada cliente TCP ganha sua própria thread
                new Thread(() -> atenderClienteTCP(clienteSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("[ERRO TCP] " + e.getMessage());
        }
    }

    private static void atenderClienteTCP(Socket socket) {
        PrintWriter out = null;
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);
            clientesTCP.add(out);

            String mensagem;
            while ((mensagem = in.readLine()) != null) {
                System.out.println("[TCP RECEBIDO] " + mensagem);
                // Reenvia a mensagem para todos os clientes conectados
                for (PrintWriter cliente : clientesTCP) {
                    cliente.println(mensagem);
                }
            }
        } catch (IOException e) {
            System.out.println("[TCP] Cliente desconectado.");
        } finally {
            if (out != null) clientesTCP.remove(out);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static void iniciarServidorUDP() {
        try (DatagramSocket udpSocket = new DatagramSocket(PORTA)) {
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket pacoteRecebido = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(pacoteRecebido);

                String msg = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());
                InetAddress ipOrigem = pacoteRecebido.getAddress();
                int portaOrigem = pacoteRecebido.getPort();

                System.out.println("[UDP RECEBIDO de " + ipOrigem.getHostAddress() + ":" + portaOrigem + "] " + msg);

                // Responde imediatamente ao remetente (funciona através de NAT)
                String resposta = "PONG! Servidor recebeu seu alerta UDP: '" + msg + "'";
                byte[] dadosResposta = resposta.getBytes();
                DatagramPacket pacoteResposta = new DatagramPacket(
                        dadosResposta, dadosResposta.length, ipOrigem, portaOrigem
                );
                udpSocket.send(pacoteResposta);
            }
        } catch (IOException e) {
            System.err.println("[ERRO UDP] " + e.getMessage());
        }
    }
}