# Testando no PC (emulador do Android Studio), sem celular

O `CameraFragment` detecta o emulador e **não inicia o ARCore**: mostra o modelo 3D sobre a câmera virtual
(ou sobre um fundo cinza, se a câmera não responder em ~8 s). O botão passa a se chamar **Centralizar**.
Não há detecção de superfície no emulador; isso só funciona em um celular com ARCore.

## Criar o emulador
1. Android Studio → **Device Manager → Create Device** → Pixel (ex.: Pixel 6) → imagem **API 34 ou 35 com Google APIs**.
2. Antes de concluir, clique em **Show Advanced Settings** → **Camera → Back: VirtualScene** (cena 3D virtual)
   ou **Webcam0** (usa a webcam do PC).
3. Se o PC tiver pouca GPU/RAM: em **Graphics** escolha **Software** (mais lento, mas estável).

## Simular o GPS
- Emulador → **⋯ (Extended controls) → Location** → digite latitude/longitude → **Set Location**. Exemplos:

| Local | Latitude | Longitude | Pontos |
|---|---|---|---|
| Catedral Sant'Ana | -25.097922 | -50.159264 | 100 |
| Igreja dos Polacos | -25.094904 | -50.163075 | 100 |
| Estação Ferroviária | -25.099575 | -50.158396 | 150 |
| Buraco do Padre | -25.171830 | -49.968420 | 200 |
| Parque Estadual de Vila Velha | -25.246243 | -50.021782 | 250 |

- Depois de mudar o ponto, aguarde alguns segundos: o app só aceita leituras recentes e com precisão ≤ 50 m.
- Para o mesmo local render outro modelo, afaste-se mais de 125 m antes de voltar.

## Atalho sem GPS
Na tela de AR, em emulador ou build de depuração, aparece **Simular visita (teste)**. Cada toque percorre os
pontos turísticos em ordem e aplica as regras do jogo, sem precisar mexer no GPS.

## Testar o game over
1. Garagem → **Adicionar** → escolha uma foto → preencha marca/modelo/ano → **Salvar**.
2. Repita com **a mesma marca, modelo e ano** → tela de **DERROTA** (−50 pontos, cadastro cancelado).
3. Com a pontuação em 0 aparece **GAME OVER**; **Novo jogo** volta para 100 pontos.

## Placar
Na tela de AR, toque no troféu (ou na pontuação) para abrir o placar. Use **Encerrar contagem** para congelar
a pontuação final e **Reiniciar tudo** para começar de novo. O botão de lixeira remove o objeto 3D da cena.

## Mapa no emulador
1. Defina a posição em **Extended controls → Location** *antes* de cadastrar o carro.
2. Cadastre o carro (a tela do formulário pede a permissão e mostra "Localização registrada").
3. Garagem → **Mapa**. O emulador precisa de internet para baixar os tiles do mapa.
