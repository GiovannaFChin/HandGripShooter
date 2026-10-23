package br.mackenzie;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ArduinoReaderThread extends Thread {

    private final SerialPort port;
    private volatile int forcaAtual = 0;
    private volatile boolean rodando = true;
    private final ConcurrentLinkedQueue<Integer> tiros = new ConcurrentLinkedQueue<>();

    public ArduinoReaderThread(String portName) {
        this.port = SerialPort.getCommPort(portName);
        setDaemon(true);
    }

    @Override
    public void run() {
        port.setBaudRate(115200);
        if (!port.openPort()) {
            System.err.println("Erro: não foi possível abrir a porta serial.");
            return;
        }

        StringBuilder linha = new StringBuilder();
        try (InputStream in = port.getInputStream()) {
            while (rodando) {
                if (in.available() > 0) {
                    char c = (char) in.read();
                    if (c == '\n') {
                        processar(linha.toString().trim());
                        linha.setLength(0);
                    } else {
                        linha.append(c);
                    }
                } else {
                    Thread.sleep(1);
                }
            }
        } catch (Exception e) {
            if (rodando) System.err.println("Erro na leitura serial: " + e.getMessage());
        } finally {
            if (port.isOpen()) {
                port.closePort();
                System.out.println("Porta serial fechada.");
            }
        }
    }

    private void processar(String msg) {
        try {
            if (msg.startsWith("FORCA:")) {
                forcaAtual = Integer.parseInt(msg.substring(6));
            } else if (msg.startsWith("TIRO:")) {
                tiros.add(Integer.parseInt(msg.substring(5)));
            }
        } catch (NumberFormatException ignored) {
            // linha corrompida (comum no início da conexão): ignora
        }
    }

    /** Força atual (0..4095), para barra de carga. */
    public int getForca() { return forcaAtual; }

    /** Retorna o próximo tiro (1..3) ou null se não houver. */
    public Integer pollTiro() { return tiros.poll(); }

    public void stopReading() { rodando = false; }
}