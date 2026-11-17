# Garage AR

Garagem virtual automotiva com realidade aumentada (Kotlin, XML, ViewBinding, Navigation Component, Room, SceneView/ARCore).

## Funcionalidades
- **Minha Garagem:** cadastro por câmera/galeria, edição, exclusão e favoritos (Room + fotos no armazenamento interno).
- **Modelos:** lista os `.glb` de `assets/` e permite importar/remover `.glb`.
- **Realidade Aumentada:** ARCore em celular compatível; modo de visualização 3D no emulador (veja `EMULADOR.md`).
- **Localização (branch `feature-location`):** endereço atual e pontos turísticos de Ponta Grossa.

## Mapa dos carros
- Ao cadastrar um carro, o app guarda a posição (permissão de localização) e sugere o endereço no campo "Local".
  O botão **Usar minha localização** refaz a leitura (também na edição).
- Garagem → chip **Mapa**: marcadores de todos os carros; tocar em um mostra o cartão com **Detalhes**.
  Nos detalhes do carro, **Ver no mapa** centraliza nele. Mapa OpenStreetMap (osmdroid), sem chave de API; precisa de internet.
- Carros cadastrados antes dessa versão continuam na garagem, mas sem marcador (banco migrado da v1 para a v2).

## Jogo (branch `feature-location-score`)
- Começa com **100 pontos**.
- Chegar a até 100 m de um ponto turístico revela um modelo **ainda não encontrado** e soma os pontos do local
  (`game/TouristSpots.kt`: Catedral Sant'Ana, Igreja dos Polacos, Estação Ferroviária, Buraco do Padre, Parque de Vila Velha).
- Cadastrar um carro **repetido** (mesma marca, modelo e ano) cancela o cadastro, tira **50 pontos** e abre a tela de **DERROTA**.
- Pontuação **0 = GAME OVER**; "Novo jogo" reinicia a pontuação e os modelos encontrados (a garagem é mantida).
- A tela de AR mostra o **endereço atual** e a **pontuação**.
- Na tela de AR, o botão de lixeira **remove o objeto** da cena ("Mostrar objeto" o traz de volta) e o troféu abre o **Placar**.
- **Placar:** tabela com cada modelo encontrado (+) e cada carro repetido (−), total, modelos encontrados e repetidos.
  **Encerrar contagem** congela a pontuação final; **Reiniciar tudo** volta a 100 pontos e apaga o histórico.

## Observações
- `esportivo.glb`, `suv.glb` e `picape.glb` são placeholders low-poly gerados por script.
- Para trocar os locais, edite apenas `game/TouristSpots.kt`.
