// ... (restante do código igual)
// SUBSTITUA APENAS a função iaAreaMode pelo código abaixo

function iaAreaMode(self, timeArr, adversArr, bola, side) {
  // IA sempre vai atrás da bola, mas ajusta estratégia para ataque/defesa
  let genes = self.genome;
  let mate = timeArr.find(r => r !== self);

  // Sempre persegue a bola
  let targetX = bola.x;
  let targetY = bola.y;

  // Se estiver em modo defesa, se posiciona mais entre bola e próprio gol
  if (inDefArea(self)) {
    // Linha do gol
    let myGoalX = (side === "azul") ? FIELD.x + FIELD.goalD / 2 : FIELD.x + FIELD.w - FIELD.goalD / 2;
    let middle = FIELD.centerY;
    // Fica entre bola e gol, mas levemente mais perto do gol
    targetX = myGoalX + (bola.x - myGoalX) * 0.65;
    targetY = middle + (bola.y - middle) * 0.75;
  }
  // Se estiver em modo ataque, mira na bola, mas tenta alinhar com o gol adversário
  else if (inAttackArea(self)) {
    let goalX = (side === "azul") ? FIELD.x + FIELD.w + 35 : FIELD.x - 35;
    let goalY = FIELD.centerY;
    // Anda para a bola, mas já mirando para o gol
    targetX = bola.x + (goalX - bola.x) * .2;
    targetY = bola.y + (goalY - bola.y) * .2;
  }

  // Move-se para o alvo calculado
  let dx = targetX - self.x, dy = targetY - self.y;
  let force = 1 + genes.aggro * 0.4;
  self.move(dx, dy, force);
}

// ... (restante do código igual)