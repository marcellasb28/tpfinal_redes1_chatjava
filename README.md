# Chat em Rede: TCP + UDP

Projeto desenvolvido em Java para atender ao Trabalho Prático de Redes de
Computadores I da PUC Minas Virtual. A aplicação combina um servidor de rede
com um cliente gráfico em Java Swing e demonstra os comportamentos diferentes
dos protocolos TCP e UDP.

O enunciado original está disponível em
[`docs/unidade01-trab-pratico.pdf`](docs/unidade01-trab-pratico.pdf).
Os slides da apresentação estão em
[`TP Redes — Cliente-Servidor TCP & UDP.pdf`](TP%20Redes%20%E2%80%94%20Cliente-Servidor%20TCP%20%26%20UDP.pdf).

## Objetivo

Criar uma aplicação distribuída que:

- usa Java para servidor e cliente;
- apresenta ao usuário uma interface gráfica Java Swing;
- mantém um canal confiável de mensagens com TCP;
- envia uma solicitação breve e recebe uma confirmação com UDP;
- usa threads para não bloquear a interface e atender vários clientes.

O projeto foi pensado para executar o servidor em um computador (PC2) e o
cliente em outro computador (PC1), com a comunicação atravessando três redes
roteadas.

## Estrutura do projeto

```text
ChatRedes/
  ClienteSwing.java
  ClienteSwing.class
  Servidor.java
  Servidor.class
TP Redes — Cliente-Servidor TCP & UDP.pdf
docs/
  unidade01-trab-pratico.pdf
README.md
```

- `Servidor.java`: recebe conexões TCP e datagramas UDP na porta `5000`.
- `ClienteSwing.java`: cria a janela Swing, conecta pelo TCP e envia o teste UDP.
- `TP Redes — Cliente-Servidor TCP & UDP.pdf`: slides da apresentação do projeto.
- `Servidor.class` e `ClienteSwing.class`: arquivos compilados já presentes no
  repositório; podem ser removidos/regerados ao compilar novamente.
- `docs/unidade01-trab-pratico.pdf`: cópia do enunciado fornecido.

## Como a aplicação funciona

### Servidor

O servidor define a porta `5000` e abre dois tipos de sockets:

1. **TCP**: `ServerSocket` aceita conexões em `5000`. Cada cliente aceito é
   atendido por uma nova thread.
2. **UDP**: `DatagramSocket` recebe datagramas na mesma porta `5000`.

A porta pode ser compartilhada por TCP e UDP porque os protocolos são
identificados separadamente pela camada de transporte.

Ao iniciar, o servidor cria uma thread para o método `iniciarServidorUDP()`.
A thread principal permanece responsável pelo loop de aceitação das conexões
TCP em `iniciarServidorTCP()`.

### Chat via TCP

O método `atenderClienteTCP` cria um `BufferedReader` para receber textos e um
`PrintWriter` para enviar respostas ao cliente conectado.

Cada `PrintWriter` é armazenado em uma `CopyOnWriteArrayList`. Essa lista é
thread-safe e permite que o servidor retransmita uma mensagem recebida de um
cliente para todos os clientes atualmente conectados.

Esse caminho demonstra as propriedades do TCP:

- a conexão é estabelecida antes da troca de dados;
- o fluxo preserva a ordem das mensagens;
- o servidor identifica desconexões pelo fim da leitura;
- o canal permite comunicação bidirecional e persistente.

### Alerta/ping via UDP

O botão **Alerta/Ping (UDP)** cria um datagrama, envia uma mensagem curta ao
endereço e à porta informados e aguarda uma resposta por até três segundos.

O servidor UDP retorna uma mensagem `PONG` para o endereço e a porta de origem.
Esse retorno funciona mesmo atrás de NAT porque a resposta é enviada diretamente
ao IP e à porta usados pelo cliente no envio.

Esse caminho demonstra as características do UDP:

- não existe uma conexão estabelecida antes do envio;
- o servidor responde ao remetente do datagrama;
- não há confirmação ou retransmissão automática;
- o timeout do cliente informa quando uma resposta não chegou.

## Interface Java Swing

`ClienteSwing` cria uma janela com:

- campos para IP do servidor, porta e nome do usuário;
- botão para conectar ao servidor TCP;
- área de conversa somente leitura;
- campo para digitar mensagens;
- botão para enviar pelo TCP;
- botão para enviar alerta pelo UDP.

A operação de conexão TCP é executada em uma thread separada. A recepção TCP
também funciona em uma thread própria dentro dessa execução. As atualizações da
área de texto são encaminhadas com `SwingUtilities.invokeLater()` para que
sejam realizadas pela Event Dispatch Thread, que é a thread responsável pela
interface Swing.

Essa separação evita que esperar por rede, receber dados ou aguardar o timeout
UDP congela a janela da aplicação.

## Compilação e execução

Requer JDK instalado e configurado no `PATH`.

### Compilar

Na pasta que contém a pasta `ChatRedes`, execute:

```powershell
javac ChatRedes\Servidor.java ChatRedes\ClienteSwing.java
```

### Executar no servidor

No PC2:

```powershell
java -cp . ChatRedes.Servidor
```

No início, o programa mostra o endereço local encontrado pelo Java e a porta
`5000`. Esse resultado é o par (IPSERV, PORT) que deve ser confirmado na máquina
servidora e usado pelo cliente. Em computadores com vários adaptadores de rede,
deve-se verificar qual endereço pertence à rede que chega ao servidor.

### Executar no cliente

Em outro terminal ou no PC1:

```powershell
java -cp . ChatRedes.ClienteSwing
```

Na janela:

1. informe o IP público/de encaminhamento configurado no roteador R1;
2. mantenha a porta `5000`;
3. informe um nome;
4. clique em **Conectar TCP**;
5. use o campo de mensagem e o botão **Enviar (TCP)**;
6. use **Alerta/Ping (UDP)** para verificar o datagrama.

Para que o acesso atravesse os roteadores, a porta deve ser encaminhada tanto
para TCP quanto para UDP no redirecionamento configurado. O projeto usa a mesma
porta nos dois protocolos.

## Topologia solicitada no trabalho

```text
PC1 ---- R1 ---- R2 ---- R3 ---- PC2
        LAN 1    LAN 1
        WAN      WAN
```

O PC1 permanece na rede do R1 e o PC2 na rede do R3:

| Equipamento | Rede solicitada | Faixa |
|---|---|---|
| R1 / PC1 | 192.168.0.0/16 | 192.168.0.0–192.168.255.255 |
| R2 | 172.16.0.0/12 | 172.16.0.0–172.31.255.255 |
| R3 / PC2 | 10.0.0.0/8 | 10.0.0.0–10.255.255.255 |

Os enlaces físicos são:

- cabo 1: LAN 1 do R1 para WAN do R2;
- cabo 2: LAN 1 do R2 para WAN do R3.

## Configuração de encaminhamento

A configuração deve seguir o sentido do cliente até o servidor:

1. no PC2, iniciar o servidor e anotar o endereço IPSERV e a porta `5000`;
2. em R3, encaminhar a porta `5000` para IPSERV;
3. em R2, encaminhar a porta `5000` para o endereço de R3;
4. em R1, encaminhar a porta `5000` para o endereço de R2;
5. no PC1, executar o cliente e preencher o endereço externo publicado pelo R1.

Os testes reais podem exigir os nomes específicos usados pelo simulador ou
roteador, como IP público, WAN, LAN, local IP e external port. Os mecanismos
devem ser configurados para ambos os protocolos no caso de uma interface que
separe TCP e UDP.

No Windows, o Firewall pode bloquear a entrada na porta `5000`. Antes do teste
em rede física, criar uma regra de entrada que permita TCP e UDP na porta
`5000` ou desativar temporariamente o firewall para o perfil usado no laboratório.

## Por que cada requisito foi implementado desta forma

| Requisito | Implementação | Motivo |
|---|---|---|
| Java | Servidor e cliente em arquivos `.java` | Permite executar a mesma aplicação em sistemas compatíveis com JDK |
| Java Swing | `JFrame`, `JTextArea`, `JButton` e painéis | Interface gráfica exigida pelo enunciado |
| TCP | `ServerSocket`, `Socket`, streams | Necessário para chat confiável e persistente |
| UDP | `DatagramSocket` e `DatagramPacket` | Necessário para demonstrar datagrama sem conexão |
| Multithreading | Thread UDP, thread por cliente TCP e threads de rede no cliente | Permite atender simultaneamente e não congelar Swing |

## Multithreading no projeto

Há três níveis de concorrência:

1. o servidor inicia uma thread dedicada ao recebedor UDP;
2. cada conexão TCP aceita gera uma thread para seu cliente;
3. o cliente usa thread separada para conexão/recepção TCP e outra para o UDP.

Esse desenho evita que um único cliente ou um timeout UDP bloqueie o restante
da aplicação. A `CopyOnWriteArrayList` protege a lista de escritores enquanto
clientes podem entrar ou sair.

## Verificação por Wireshark

Durante os testes, capture a interface de rede do PC1 ou do servidor e aplique
filtros por porta:

```text
tcp.port == 5000
udp.port == 5000
```

Filtros separados ajudam a comprovar:

- estabelecimento da conexão TCP por `SYN`, `SYN-ACK` e `ACK`;
- troca de mensagens confiáveis pelo TCP;
- ausência de handshake no envio UDP;
- resposta `PONG` no mesmo datagrama UDP;
- retransmissão de pacotes ou timeout caso o UDP seja perdido.

As imagens relevantes devem ser incluídas no relatório de desenvolvimento.

## Checklist de entrega

- [x] Aplicação Java com Swing;
- [x] Funcionalidade TCP;
- [x] Funcionalidade UDP;
- [x] Uso de multithreading;
- [x] Enunciado incluído em `docs/unidade01-trab-pratico.pdf`;
- [x] Projeto no Cisco Packet Tracer;
- [x] Capturas e análise no Wireshark;
- [x] PDF do relatório enviado pelo Canvas;
- [x] Apresentação presencial.
