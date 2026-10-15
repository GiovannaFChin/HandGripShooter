# HandGrip Shooter 

## Sobre o projeto

O **HandGrip Shooter** é um projeto extensionista que propõe unir tecnologia, jogos digitais e mobilidade da mão por meio de um jogo de tiro 2D desenvolvido em **Java com o framework libGDX**.

A proposta é utilizar um hand grip como controle físico do jogo, permitindo que o jogador dispare projéteis ao apertar o dispositivo. A intensidade do aperto será captada por um sensor de pressão FSR402 e interpretada por um ESP32, possibilitando diferentes tipos de disparos de acordo com a força aplicada.

## Objetivo extensionista

O projeto busca explorar o potencial dos jogos digitais como ferramentas de interação e incentivo à realização de movimentos com as mãos. A ideia é oferecer uma experiência lúdica que associe o movimento de apertar e soltar o hand grip a desafios dentro do jogo, tornando a atividade mais interativa e envolvente.

A proposta também busca aproximar a tecnologia da comunidade, demonstrando como a programação e os componentes eletrônicos podem ser utilizados no desenvolvimento de soluções acessíveis e criativas, com potencial de apoio a atividades de mobilidade e controle motor da mão.

O jogo não tem como objetivo substituir tratamentos ou exercícios orientados por profissionais de saúde, mas explorar uma possibilidade de gamificação dessas atividades.

## Tecnologias e componentes

- **Java:** linguagem de programação utilizada no desenvolvimento do jogo.
- **libGDX:** framework para criação do jogo 2D.
- **ESP32:** microcontrolador responsável por processar as leituras do sensor e enviá-las ao computador.
- **FSR402:** sensor de pressão que detecta a intensidade relativa do aperto.
- **Hand grip:** dispositivo físico utilizado como controle.
- **Resistor de 10 kΩ e conexões:** componentes utilizados no circuito de leitura do sensor.

## Como funciona

1. O jogador aperta o hand grip.
2. O sensor FSR402 detecta a variação de pressão.
3. O ESP32 lê o sinal e envia os dados ao computador por comunicação serial USB.
4. O jogo interpreta a intensidade do aperto e executa a ação correspondente.

### Mecânica dos disparos

| Intensidade do aperto | Ação no jogo |
|---|---|
| Sem pressão | Personagem aguarda |
| Aperto leve | Disparo simples |
| Aperto moderado | Disparo reforçado |
| Aperto forte | Disparo especial ou múltiplos projéteis |

## Resultados esperados

Espera-se desenvolver um protótipo funcional que integre hardware e software, explore diferentes intensidades de movimento e ofereça uma experiência de jogo interativa. O projeto também pretende demonstrar como a gamificação pode ser explorada em atividades relacionadas à mobilidade e ao controle motor, abrindo possibilidades para futuras melhorias e avaliações com o público-alvo.

## Status do projeto

🚧 Em desenvolvimento.
