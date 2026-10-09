"""Gera a textura da borda escura dos sentidos (assets/sussurros/textures/misc/vinheta.png).

O jogo desenha a textura com a mistura "destino x (1 - origem)": onde ela é branca, a tela escurece;
onde é preta, nada muda. A vinheta do próprio jogo escurece no máximo uns 25% nos cantos, o que é pouco
para o que o mod precisa, então esta tem a queda mais larga e chega ao branco nos cantos.
"""
import os
import numpy as np
from PIL import Image

LADO = 256
DESTINO = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'sussurros',
                       'textures', 'misc', 'vinheta.png')

y, x = np.mgrid[0:LADO, 0:LADO]
u = (x + 0.5) / LADO * 2 - 1
v = (y + 0.5) / LADO * 2 - 1
# Distância ao centro: 0 no meio, 1 no meio de cada borda, ~1,41 nos cantos.
r = np.sqrt(u * u + v * v)
t = np.clip((r - 0.55) / (1.35 - 0.55), 0.0, 1.0)
valor = (t * t * (3 - 2 * t)) ** 1.15

# Um pouco de ruído quebra as faixas que um degradê liso mostra em céu claro.
rng = np.random.default_rng(7)
valor = np.clip(valor + (rng.random((LADO, LADO)) - 0.5) / 255.0 * 1.5, 0.0, 1.0)

img = (valor * 255).round().astype(np.uint8)
Image.fromarray(np.dstack([img, img, img]), 'RGB').save(DESTINO)
print('gravado', os.path.normpath(DESTINO), 'centro', img[LADO // 2, LADO // 2], 'borda', img[LADO // 2, 0], 'canto', img[0, 0])
