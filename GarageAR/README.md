# Garage AR

Garagem virtual automotiva com realidade aumentada (Kotlin, XML, ViewBinding, Navigation Component, Room, SceneView/ARCore).

## O que foi feito sobre o projeto-base
- **Navegação inferior:** Realidade Aumentada · Modelos · Minha Garagem (Navigation Component).
- **Minha Garagem:** grade responsiva de cartões, filtro de favoritos, cadastro por câmera ou galeria.
- **Cadastro/edição/detalhes/exclusão** com marca, modelo, ano, motorização, cv, torque, tração, raridade, local, observações e favorito.
- **Persistência:** Room (`garage_ar.db`) + fotos em `filesDir/vehicle_photos` (armazenamento interno).
- **AR:** mantida a implementação com `ArModelNode`; agora com mover/girar/redimensionar, botão Posicionar/Reposicionar e nome do modelo.
- **Modelos:** lista os `.glb` de `assets/` e permite importar/remover `.glb` do aparelho.
- **Tema:** escuro, laranja papaya (#FF8700), cartões arredondados.

## Correções do projeto-base
- `nav_graph.xml` apontava para `ProfileFragment`/`fragment_profile`, que não existem.
- `CameraFragment`: `lateinit modelNode` causava crash ao tocar em "Place" antes de carregar; a permissão de câmera
  era pedida via `requireActivity()` e o resultado nunca chegava ao fragment; `super.onCreate` era chamado em `onCreateView`.

## Modelos 3D
`esportivo.glb`, `suv.glb` e `picape.glb` são **placeholders low-poly** gerados por script. Para modelos reais, coloque
arquivos `.glb` em `app/src/main/assets/` (ou importe pelo app).
