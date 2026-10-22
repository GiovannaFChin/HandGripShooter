// ===== Hand Grip com FSR402 + ESP32 =====
// Circuito: 3V3 -> FSR402 -> (ponto de leitura GPIO34) -> resistor 10k -> GND

const int PIN_FSR = 34;

// Limites (0 a 4095). AJUSTE depois de calibrar com o Monitor Serial.
const int LIMITE_ATIVACAO = 400;   // acima disso = apertando
const int LIMITE_LEVE     = 400;
const int LIMITE_MODERADO = 1200;
const int LIMITE_FORTE    = 2500;
const int HISTERESE       = 100;   // evita "tremer" perto do limite

const int NUM_AMOSTRAS = 8;        // média para suavizar o ruído

bool apertando = false;
int picoDoAperto = 0;

int lerFiltrado() {
  long soma = 0;
  for (int i = 0; i < NUM_AMOSTRAS; i++) {
    soma += analogRead(PIN_FSR);
  }
  return soma / NUM_AMOSTRAS;
}

// 0 = nenhum, 1 = leve, 2 = moderado, 3 = forte
int classificar(int valor) {
  if (valor >= LIMITE_FORTE)    return 3;
  if (valor >= LIMITE_MODERADO) return 2;
  if (valor >= LIMITE_LEVE)     return 1;
  return 0;
}

void setup() {
  Serial.begin(115200);                        // precisa bater com o Java
  analogReadResolution(12);                    // 0..4095
  analogSetPinAttenuation(PIN_FSR, ADC_11db);  // faixa até ~3,3 V
}

void loop() {
  int valor = lerFiltrado();

  if (!apertando && valor > LIMITE_ATIVACAO) {
    // Começou um aperto
    apertando = true;
    picoDoAperto = valor;
  }
  else if (apertando) {
    if (valor > picoDoAperto) picoDoAperto = valor;

    // Soltou: dispara com base no pico atingido
    if (valor < LIMITE_ATIVACAO - HISTERESE) {
      apertando = false;
      int nivel = classificar(picoDoAperto);
      Serial.print("TIRO:");
      Serial.println(nivel);
      picoDoAperto = 0;
    }
  }

  // Telemetria contínua (barra de força e calibração)
  Serial.print("FORCA:");
  Serial.println(valor);

  delay(10);   // ~100 leituras por segundo
}
