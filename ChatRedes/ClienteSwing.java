import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ClienteSwing extends JFrame {
    private JTextField txtIp, txtPorta, txtNome, txtMensagem;
    private JButton btnConectar, btnEnviarTCP, btnPingUDP;
    private JTextArea areaChat;

    private Socket socketTCP;
    private PrintWriter outTCP;
    private boolean conectado = false;

    public ClienteSwing() {
        setTitle("Cliente Chat Redes (TCP + UDP)");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- PAINEL SUPERIOR (Conexão) ---
        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtIp = new JTextField("127.0.0.1", 10);
        txtPorta = new JTextField("5000", 5);
        txtNome = new JTextField("Usuario1", 8);
        btnConectar = new JButton("Conectar TCP");

        painelTopo.add(new JLabel("IP:"));
        painelTopo.add(txtIp);
        painelTopo.add(new JLabel("Porta:"));
        painelTopo.add(txtPorta);
        painelTopo.add(new JLabel("Nome:"));
        painelTopo.add(txtNome);
        painelTopo.add(btnConectar);
        add(painelTopo, BorderLayout.NORTH);

        // --- PAINEL CENTRAL (Área de Mensagens) ---
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setFont(new Font("Monospaced", Font.PLAIN, 13));
        add(new JScrollPane(areaChat), BorderLayout.CENTER);

        // --- PAINEL INFERIOR (Envio TCP e UDP) ---
        JPanel painelBaixo = new JPanel(new BorderLayout(5, 5));
        txtMensagem = new JTextField();
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnEnviarTCP = new JButton("Enviar (TCP)");
        btnPingUDP = new JButton("Alerta/Ping (UDP)");

        btnEnviarTCP.setEnabled(false);

        painelBotoes.add(btnEnviarTCP);
        painelBotoes.add(btnPingUDP);
        painelBaixo.add(txtMensagem, BorderLayout.CENTER);
        painelBaixo.add(painelBotoes, BorderLayout.EAST);
        add(painelBaixo, BorderLayout.SOUTH);

        // --- AÇÕES DOS BOTÕES ---
        btnConectar.addActionListener(e -> conectarTCP());
        btnEnviarTCP.addActionListener(e -> enviarMensagemTCP());
        txtMensagem.addActionListener(e -> enviarMensagemTCP()); // Enviar com Enter
        btnPingUDP.addActionListener(e -> enviarPingUDP());
    }

    private void conectarTCP() {
        if (conectado) return;
        String ip = txtIp.getText().trim();
        int porta = Integer.parseInt(txtPorta.getText().trim());

        // Conecta em uma thread separada para não travar a tela Swing
        new Thread(() -> {
            try {
                socketTCP = new Socket(ip, porta);
                outTCP = new PrintWriter(socketTCP.getOutputStream(), true);
                BufferedReader inTCP = new BufferedReader(new InputStreamReader(socketTCP.getInputStream()));

                conectado = true;
                SwingUtilities.invokeLater(() -> {
                    adicionarLog("[SISTEMA] Conectado via TCP em " + ip + ":" + porta);
                    btnConectar.setEnabled(false);
                    btnEnviarTCP.setEnabled(true);
                });

                // Loop de leitura contínua de mensagens TCP
                String msgRecebida;
                while ((msgRecebida = inTCP.readLine()) != null) {
                    String finalMsg = msgRecebida;
                    SwingUtilities.invokeLater(() -> adicionarLog("[TCP] " + finalMsg));
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> 
                    adicionarLog("[ERRO TCP] Nao foi possivel conectar: " + ex.getMessage())
                );
            }
        }).start();
    }

    private void enviarMensagemTCP() {
        if (!conectado) return;
        String texto = txtMensagem.getText().trim();
        if (!texto.isEmpty()) {
            outTCP.println(txtNome.getText().trim() + ": " + texto);
            txtMensagem.setText("");
        }
    }

    private void enviarPingUDP() {
        String ip = txtIp.getText().trim();
        int porta = Integer.parseInt(txtPorta.getText().trim());
        String nome = txtNome.getText().trim();

        // Envio e espera de resposta UDP em uma thread separada
        new Thread(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket()) {
                udpSocket.setSoTimeout(3000); // Timeout de 3 segundos caso o pacote se perca

                long inicio = System.currentTimeMillis();
                String msg = "Alerta rapido de " + nome;
                byte[] dados = msg.getBytes();

                InetAddress endereco = InetAddress.getByName(ip);
                DatagramPacket pacoteEnvio = new DatagramPacket(dados, dados.length, endereco, porta);
                udpSocket.send(pacoteEnvio);

                byte[] bufferResposta = new byte[1024];
                DatagramPacket pacoteResposta = new DatagramPacket(bufferResposta, bufferResposta.length);
                udpSocket.receive(pacoteResposta);

                long tempoMs = System.currentTimeMillis() - inicio;
                String resposta = new String(pacoteResposta.getData(), 0, pacoteResposta.getLength());

                SwingUtilities.invokeLater(() -> 
                    adicionarLog("[UDP - " + tempoMs + "ms] " + resposta)
                );
            } catch (SocketTimeoutException ex) {
                SwingUtilities.invokeLater(() -> 
                    adicionarLog("[UDP] Sem resposta do servidor (Timeout - pacote perdido ou porta bloqueada).")
                );
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> 
                    adicionarLog("[ERRO UDP] " + ex.getMessage())
                );
            }
        }).start();
    }

    private void adicionarLog(String texto) {
        areaChat.append(texto + "\n");
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClienteSwing().setVisible(true));
    }
}