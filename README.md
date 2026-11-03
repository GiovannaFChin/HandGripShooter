# HandGrip Shooter

## Sobre o projeto

O **HandGrip Shooter** é um projeto extensionista que une tecnologia, jogos digitais e mobilidade da mão por meio de um jogo de tiro 2D desenvolvido em **Java com o framework libGDX**.

O jogo utiliza um hand grip como controle físico, permitindo que o jogador dispare projéteis ao apertar o dispositivo. A pressão é captada por um sensor FSR402 e interpretada por um ESP32, que envia os dados ao computador para controlar as ações do jogo.

## Objetivo extensionista

O projeto busca explorar o potencial dos jogos digitais como ferramentas de incentivo à realização de movimentos com as mãos. Por meio da gamificação, a proposta transforma o movimento de apertar e soltar o hand grip em ações dentro do jogo, tornando a experiência mais interativa.

A iniciativa também busca demonstrar como a programação e os componentes eletrônicos podem ser utilizados em soluções criativas com potencial de apoio a atividades de mobilidade e controle motor da mão. O jogo não substitui tratamentos ou exercícios orientados por profissionais de saúde.

## Tecnologias e componentes

- **Java:** linguagem utilizada no desenvolvimento do jogo.
- **libGDX:** framework para criação do jogo 2D.
- **ESP32:** microcontrolador responsável pela leitura do sensor e pelo envio dos dados ao computador.
- **FSR402:** sensor de pressão que detecta a intensidade relativa do aperto.
- **Hand grip:** dispositivo físico utilizado como controle.
- **Resistor de 10 kΩ e conexões:** componentes utilizados no circuito de leitura do sensor.

## Como funciona

1. O jogador aperta o hand grip.
2. O sensor FSR402 detecta a variação de pressão.
3. O ESP32 lê o sinal e envia os dados ao computador por comunicação serial USB.
4. O jogo interpreta a intensidade do aperto e executa a ação correspondente.

### Mecânica dos disparos

| Intensidade do aperto | Ação no jogo                            |
| --------------------- | --------------------------------------- |
| Sem pressão           | Personagem aguarda                      |
| Aperto leve           | Disparo simples                         |
| Aperto moderado       | Disparo com dois projéteis              |
| Aperto forte          | Disparo com três projéteis              |

## Fases do jogo

O jogo possui três fases com mudanças de cenário e dificuldade progressiva:

Fase 1 — Dia: o jogo começa em um ambiente diurno, introduzindo a mecânica de combate.

Fase 2 — Tarde: o cenário passa gradualmente para o entardecer, criando uma mudança visual no ambiente.

Fase 3 — Noite: o cenário fica noturno e os monstros se tornam mais fortes, aumentando o desafio para o jogador.

A progressão das fases aumenta o desafio e incentiva o jogador a continuar interagindo com o dispositivo para avançar no jogo.

## Resultados esperados

Espera-se desenvolver um protótipo funcional que integre hardware e software, utilizando os movimentos realizados com o hand grip como controle do jogo. A proposta também busca explorar a gamificação como forma de incentivar a prática de movimentos manuais e possibilitar futuras melhorias na avaliação da interação e do desempenho do jogador.

