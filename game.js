// FUTBOT Neon Evolution - IA pode chutar para qualquer lado, alterna ataque/defesa baseado na área, sempre visa o gol

const CANVAS = document.getElementById('game-canvas');
const CTX = CANVAS.getContext('2d');
const W = CANVAS.width;
const H = CANVAS.height;

const FIELD = {
  x: 60, y: 35, w: 1080, h: 530,
  centerX: W / 2, centerY: H / 2,
  goalW: 170, goalD: 36, areaW: 230, areaH: 190, lineW: 5
};
const ROBOT = {
  radius: 32,
  speed: 5.0
};
const BALL = {
  radius: 20,
  maxSpeed: 13,
  minSpeed: 0.07,
  minGhost: 0.19,
  friction: 0.98,
  color: '#fff', shadow: '#aee'
};
const GAME_TIME = 120;
const POP_SIZE = 10;
const TEAM_SIZE = 2;
let FINAL_SCORE = 2;

let gameState = "menu";
let timer = GAME_TIME;
let timerInterval = null;
let animationFrame = null;

let score = { azul: 0, laranja: 0 };
let gen = 1, bestScore = 0;
let phase = "evolution";
let population = [];
let currentPair = 0;
let ball, azulTeam, laranjaTeam, bestPair = null;

const startEvolBtn = document.getElementById('start-evol-btn');
const finalBtn = document.getElementById('final-btn');
const restartBtn = document.getElementById('restart-btn');
const scoreAzulEl = document.getElementById('score-azul');
const scoreLaranjaEl = document.getElementById('score-laranja');
const timerEl = document.getElementById('game-timer');
const goalMsg = document.getElementById('goal-msg');
const endgameScreen = document.getElementById('endgame-screen');
const endgameTitle = document.getElementById('endgame-title');
const endgameStats = document.getElementById('endgame-stats');
const endgameRestartBtn = document.getElementById('endgame-restart-btn');
const genCounter = document.getElementById('gen-counter');
const bestScoreEl = document.getElementById('best-score');

class Genome {
  constructor() {
    this.aggro = Math.random();
    this.defense = Math.random();
    this.shootPower = 0.7 + Math.random() * 0.3;
    this.teamplay = Math.random();
    this.aim = Math.random();
    this.steal = Math.random();
  }
  mutate() {
    let g = new Genome();
    g.aggro = clamp(this.aggro + (Math.random() - 0.5) * 0.13, 0, 1);
    g.defense = clamp(this.defense + (Math.random() - 0.5) * 0.13, 0, 1);
    g.shootPower = clamp(this.shootPower + (Math.random() - 0.5) * 0.12, 0.6, 1);
    g.teamplay = clamp(this.teamplay + (Math.random() - 0.5) * 0.13, 0, 1);
    g.aim = clamp(this.aim + (Math.random() - 0.5) * 0.13, 0, 1);
    g.steal = clamp(this.steal + (Math.random() - 0.5) * 0.13, 0, 1);
    return g;
  }
  crossover(other) {
    let g = new Genome();
    g.aggro = Math.random() < 0.5 ? this.aggro : other.aggro;
    g.defense = Math.random() < 0.5 ? this.defense : other.defense;
    g.shootPower = Math.random() < 0.5 ? this.shootPower : other.shootPower;
    g.teamplay = Math.random() < 0.5 ? this.teamplay : other.teamplay;
    g.aim = Math.random() < 0.5 ? this.aim : other.aim;
    g.steal = Math.random() < 0.5 ? this.steal : other.steal;
    return g;
  }
}

class Robot {
  constructor(x, y, color, id, genome, side, teamRef) {
    this.x = x;
    this.y = y;
    this.radius = ROBOT.radius;
    this.vx = 0;
    this.vy = 0;
    this.dir = 0;
    this.color = color;
    this.id = id;
    this.genome = genome;
    this.side = side;
    this.teamRef = teamRef;
    this.lastKick = -1;
  }
  move(dx, dy, force = 1) {
    let mag = Math.hypot(dx, dy);
    let speed = ROBOT.speed * force;
    if (mag > 3) {
      dx /= mag; dy /= mag;
      this.vx = dx * speed;
      this.vy = dy * speed;
      this.x += this.vx;
      this.y += this.vy;
    } else {
      this.vx = 0; this.vy = 0;
    }
    if (dx !== 0 || dy !== 0)
      this.dir = Math.atan2(dy, dx);
    this.keepInField();
  }
  keepInField() {
    let minX = FIELD.x + this.radius + FIELD.lineW / 2;
    let maxX = FIELD.x + FIELD.w - this.radius - FIELD.lineW / 2;
    let minY = FIELD.y + this.radius + FIELD.lineW / 2;
    let maxY = FIELD.y + FIELD.h - this.radius - FIELD.lineW / 2;
    if (this.x < FIELD.x + FIELD.goalD + this.radius &&
      (this.y < FIELD.centerY - FIELD.goalW / 2 || this.y > FIELD.centerY + FIELD.goalW / 2))
      this.x = FIELD.x + FIELD.goalD + this.radius;
    if (this.x > FIELD.x + FIELD.w - FIELD.goalD - this.radius &&
      (this.y < FIELD.centerY - FIELD.goalW / 2 || this.y > FIELD.centerY + FIELD.goalW / 2))
      this.x = FIELD.x + FIELD.w - FIELD.goalD - this.radius;
    this.x = Math.max(minX, Math.min(maxX, this.x));
    this.y = Math.max(minY, Math.min(maxY, this.y));
  }
  draw(ctx) {
    ctx.save();
    ctx.translate(this.x, this.y);
    ctx.rotate(this.dir);
    let grad = ctx.createRadialGradient(0, 0, this.radius * 0.2, 0, 0, this.radius);
    grad.addColorStop(0, "#fff7");
    grad.addColorStop(0.5, this.color);
    grad.addColorStop(1, "#232c5b");
    ctx.beginPath();
    ctx.arc(0, 0, this.radius, 0, 2 * Math.PI);
    ctx.fillStyle = grad;
    ctx.shadowColor = this.color;
    ctx.shadowBlur = 20;
    ctx.fill();
    ctx.shadowBlur = 0;
    ctx.beginPath();
    ctx.arc(0, 0, this.radius + 3, 0, 2 * Math.PI);
    ctx.strokeStyle = this.color + "99";
    ctx.lineWidth = 5.5;
    ctx.shadowColor = this.color + 'bb';
    ctx.shadowBlur = 10;
    ctx.stroke();
    ctx.shadowBlur = 0;
    ctx.beginPath();
    ctx.arc(0, 0, this.radius * 0.61, 0, Math.PI, false);
    ctx.fillStyle = "#fff";
    ctx.globalAlpha = 0.67;
    ctx.fill();
    ctx.globalAlpha = 1;
    ctx.beginPath();
    ctx.arc(-this.radius * 0.22, -this.radius * 0.22, 5, 0, 2 * Math.PI);
    ctx.arc(this.radius * 0.22, -this.radius * 0.22, 5, 0, 2 * Math.PI);
    ctx.fillStyle = "#222";
    ctx.fill();
    ctx.beginPath();
    ctx.arc(0, this.radius * 0.22, 8, 0, Math.PI, false);
    ctx.lineWidth = 2;
    ctx.strokeStyle = "#222";
    ctx.stroke();
    ctx.font = "bold 18px Segoe UI";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillStyle = "#fff";
    ctx.strokeStyle = "#232c5b";
    ctx.lineWidth = 2.7;
    ctx.strokeText(this.id, 0, 0);
    ctx.fillText(this.id, 0, 0);
    ctx.font = "bold 10px Segoe UI";
    ctx.fillStyle = "#ffe646";
    ctx.fillText("Gen", 0, this.radius * 0.73);
    ctx.restore();
  }
}

class Ball {
  constructor(x, y) {
    this.x = x; this.y = y;
    this.radius = BALL.radius;
    this.vx = 0; this.vy = 0;
    this.lastTouch = null;
    this.lastKickBy = null;
    this.lastKickTime = 0;
    this.stuckTime = 0;
  }
  move() {
    this.x += this.vx; this.y += this.vy;
    let stuck =
      (this.x - this.radius <= FIELD.x + FIELD.lineW / 2 + 2) ||
      (this.x + this.radius >= FIELD.x + FIELD.w - FIELD.lineW / 2 - 2) ||
      (this.y - this.radius <= FIELD.y + FIELD.lineW / 2 + 2) ||
      (this.y + this.radius >= FIELD.y + FIELD.h - FIELD.lineW / 2 - 2);

    if (stuck) {
      this.stuckTime += 1;
      if (this.stuckTime > 120) {
        this.x = FIELD.centerX;
        this.y = FIELD.centerY;
        let angle = Math.random() * Math.PI * 2;
        let speed = 9.5 + Math.random() * 2.0;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.stuckTime = 0;
      }
    } else {
      this.stuckTime = 0;
    }

    if (this.x - this.radius <= FIELD.x + FIELD.lineW / 2) {
      this.x = FIELD.x + FIELD.lineW / 2 + this.radius;
      this.vx = Math.abs(this.vx) * 1.0;
      this.vx *= -1;
    }
    if (this.x + this.radius >= FIELD.x + FIELD.w - FIELD.lineW / 2) {
      this.x = FIELD.x + FIELD.w - FIELD.lineW / 2 - this.radius;
      this.vx = -Math.abs(this.vx) * 1.0;
      this.vx *= -1;
    }
    if (this.y - this.radius <= FIELD.y + FIELD.lineW / 2) {
      this.y = FIELD.y + FIELD.lineW / 2 + this.radius;
      this.vy = Math.abs(this.vy) * 1.0;
      this.vy *= -1;
    }
    if (this.y + this.radius >= FIELD.y + FIELD.h - FIELD.lineW / 2) {
      this.y = FIELD.y + FIELD.h - FIELD.lineW / 2 - this.radius;
      this.vy = -Math.abs(this.vy) * 1.0;
      this.vy *= -1;
    }
    this.vx *= BALL.friction;
    this.vy *= BALL.friction;

    let v = Math.hypot(this.vx, this.vy);
    if (v < BALL.minGhost) {
      let angle = Math.random() * Math.PI * 2;
      this.vx = Math.cos(angle) * BALL.minGhost;
      this.vy = Math.sin(angle) * BALL.minGhost;
    }
  }
  draw(ctx) {
    ctx.save();
    ctx.beginPath();
    ctx.arc(this.x, this.y, this.radius + 9, 0, 2 * Math.PI);
    ctx.fillStyle = "#41e0ff33";
    ctx.shadowColor = "#41e0ff99";
    ctx.shadowBlur = 20;
    ctx.fill();
    ctx.shadowBlur = 0;
    ctx.beginPath();
    ctx.arc(this.x, this.y, this.radius, 0, 2 * Math.PI);
    ctx.fillStyle = BALL.color;
    ctx.shadowColor = "#fff";
    ctx.shadowBlur = 10;
    ctx.fill();
    ctx.shadowBlur = 0;
    ctx.lineWidth = 2.3;
    ctx.strokeStyle = "#232c5b";
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(this.x - 8, this.y - 12, 7, 0, 2 * Math.PI);
    ctx.fillStyle = "#e0feff";
    ctx.globalAlpha = 0.19;
    ctx.fill();
    ctx.globalAlpha = 1;
    ctx.restore();
  }
}

function inDefArea(robot) {
  // IA está dentro da área de defesa? (área do time)
  if (robot.side === "azul") {
    return robot.x < FIELD.x + FIELD.areaW + 10;
  } else {
    return robot.x > FIELD.x + FIELD.w - FIELD.areaW - 10;
  }
}

function inAttackArea(robot) {
  // IA está dentro da área de ataque? (área do time adversário)
  if (robot.side === "azul") {
    return robot.x > FIELD.x + FIELD.w - FIELD.areaW - 10;
  } else {
    return robot.x < FIELD.x + FIELD.areaW + 10;
  }
}

function iaAreaMode(self, timeArr, adversArr, bola, side) {
  let genes = self.genome;
  let mate = timeArr.find(r => r !== self);
  let ballToMe = Math.hypot(bola.x - self.x, bola.y - self.y);
  let mateDist = mate ? Math.hypot(bola.x - mate.x, bola.y - mate.y) : 9999;
  let closer = (ballToMe < mateDist);

  // Decide modo: defesa, ataque ou neutro
  let modo;
  if (inDefArea(self)) modo = "defesa";
  else if (inAttackArea(self)) modo = "ataque";
  else modo = "neutro";

  let targetX, targetY;

  if (modo === "defesa") {
    // Protege o gol e tenta afastar a bola dele
    let myGoalX = (side === "azul") ? FIELD.x + FIELD.goalD / 2 : FIELD.x + FIELD.w - FIELD.goalD / 2;
    targetX = myGoalX;
    targetY = clamp(bola.y, FIELD.centerY - FIELD.goalW / 2 + 30, FIELD.centerY + FIELD.goalW / 2 - 30);
    // Se a bola estiver próxima, vai nela para afastar
    if (Math.hypot(bola.x - self.x, bola.y - self.y) < 140)
      targetX = bola.x, targetY = bola.y;
  } else if (modo === "ataque") {
    // Vai para o gol adversário (tenta ângulo aleatório)
    let goalX = (side === "azul") ? FIELD.x + FIELD.w + 35 : FIELD.x - 35;
    let goalY = FIELD.centerY + (Math.random() - 0.5) * FIELD.goalW * 0.9;
    // Tenta bola se estiver mais perto dela
    if (closer)
      targetX = bola.x, targetY = bola.y;
    else
      targetX = goalX, targetY = goalY;
  } else {
    // Neutro: tenta interceptar a bola entre áreas
    let attackLine = (side === "azul") ? FIELD.x + FIELD.w * 0.7 : FIELD.x + FIELD.w * 0.3;
    let defenseLine = (side === "azul") ? FIELD.x + FIELD.w * 0.3 : FIELD.x + FIELD.w * 0.7;
    if (bola.x > attackLine || bola.x < defenseLine)
      targetX = bola.x, targetY = bola.y;
    else
      targetX = (side === "azul") ? bola.x + 40 : bola.x - 40, targetY = bola.y;
  }

  let dx = targetX - self.x, dy = targetY - self.y;
  let force = 1 + genes.aggro * 0.4;
  self.move(dx, dy, force);
}

function robotKickFreeAnyDir(robot, ball) {
  let dx = ball.x - robot.x, dy = ball.y - robot.y;
  let dist = Math.hypot(dx, dy), minDist = robot.radius + ball.radius;
  if (dist < minDist && dist > 0) {
    let now = Date.now();
    if (now - robot.lastKick > 250) {
      // Chuta para qualquer direção: ataque mira no gol, defesa pode afastar para o lado, neutro pode variar
      let modo;
      if (inDefArea(robot)) modo = "defesa";
      else if (inAttackArea(robot)) modo = "ataque";
      else modo = "neutro";
      let chuteDir;
      if (modo === "ataque") {
        // Mira para o gol adversário
        let targetX = (robot.side === "azul") ? FIELD.x + FIELD.w + 35 : FIELD.x - 35;
        let targetY = FIELD.centerY + (Math.random() - 0.5) * FIELD.goalW * 0.6;
        chuteDir = Math.atan2(targetY - ball.y, targetX - ball.x);
      } else if (modo === "defesa") {
        // Afastar para o lado do campo
        chuteDir = Math.atan2(Math.random() - 0.5, robot.side === "azul" ? 1 : -1);
      } else {
        // Neutro: chuta aleatório para frente ou para onde está indo
        if (Math.abs(robot.vx) + Math.abs(robot.vy) > 1)
          chuteDir = Math.atan2(robot.vy, robot.vx);
        else
          chuteDir = Math.atan2(ball.y - robot.y, ball.x - robot.x) + (Math.random() - 0.5) * 0.7;
      }
      let chutePower = BALL.maxSpeed * (0.85 + Math.random() * 0.35);
      ball.vx = Math.cos(chuteDir) * chutePower;
      ball.vy = Math.sin(chuteDir) * chutePower;
      ball.lastTouch = robot;
      robot.lastKick = now;
    }
  }
}

function initPopulation() {
  population = [];
  for (let i = 0; i < POP_SIZE; i++) {
    let azulGenomes = [new Genome(), new Genome()];
    let laranjaGenomes = [new Genome(), new Genome()];
    population.push({
      azul: [azulGenomes[0], azulGenomes[1]],
      laranja: [laranjaGenomes[0], laranjaGenomes[1]],
      score: 0
    });
  }
  gen = 1;
  bestScore = 0;
  phase = "evolution";
  bestPair = null;
  updateGenUI();
  finalBtn.disabled = true;
}

function startEvolution() {
  gameState = "playing";
  phase = "evolution";
  currentPair = 0;
  playNextPair();
}

function playNextPair() {
  score = { azul: 0, laranja: 0 };
  timer = GAME_TIME;
  azulTeam = [
    new Robot(FIELD.x + FIELD.w * 0.20, FIELD.centerY - 70, "#41e0ff", "A1", population[currentPair].azul[0], "azul", null),
    new Robot(FIELD.x + FIELD.w * 0.20, FIELD.centerY + 70, "#41e0ff", "A2", population[currentPair].azul[1], "azul", null)
  ];
  azulTeam[0].teamRef = azulTeam;
  azulTeam[1].teamRef = azulTeam;
  laranjaTeam = [
    new Robot(FIELD.x + FIELD.w * 0.80, FIELD.centerY - 70, "#ffb347", "L1", population[currentPair].laranja[0], "laranja", null),
    new Robot(FIELD.x + FIELD.w * 0.80, FIELD.centerY + 70, "#ffb347", "L2", population[currentPair].laranja[1], "laranja", null)
  ];
  laranjaTeam[0].teamRef = laranjaTeam;
  laranjaTeam[1].teamRef = laranjaTeam;
  ball = new Ball(FIELD.centerX, FIELD.centerY);
  ballRandomInitialImpulse();
  updateScoreboard();
  updateTimer();
  if (animationFrame) cancelAnimationFrame(animationFrame);
  animationFrame = requestAnimationFrame(gameLoop);
  if (timerInterval) clearInterval(timerInterval);
  timerInterval = setInterval(timerTick, 1000);
}

function ballRandomInitialImpulse() {
  let angle = Math.random() * Math.PI * 2;
  let speed = 9.5 + Math.random() * 2.0;
  ball.vx = Math.cos(angle) * speed;
  ball.vy = Math.sin(angle) * speed;
}

function evolveGeneration() {
  population.sort((a, b) => b.score - a.score);
  let top = population.slice(0, POP_SIZE / 2);
  bestScore = population[0].score;
  bestPair = { azul: [top[0].azul[0], top[0].azul[1]], laranja: [top[0].laranja[0], top[0].laranja[1]] };
  updateGenUI();
  finalBtn.disabled = false;
  let newPop = [];
  for (let i = 0; i < POP_SIZE / 2; i++) {
    for (let j = 0; j < 2; j++) {
      let parentA = top[Math.floor(Math.random() * top.length)];
      let parentB = top[Math.floor(Math.random() * top.length)];
      let childAzul1 = parentA.azul[0].crossover(parentB.azul[0]).mutate();
      let childAzul2 = parentA.azul[1].crossover(parentB.azul[1]).mutate();
      let childLaran1 = parentA.laranja[0].crossover(parentB.laranja[0]).mutate();
      let childLaran2 = parentA.laranja[1].crossover(parentB.laranja[1]).mutate();
      newPop.push({ azul: [childAzul1, childAzul2], laranja: [childLaran1, childLaran2], score: 0 });
    }
  }
  population = newPop;
  gen++;
  setTimeout(() => {
    currentPair = 0;
    if (gameState === "playing") playNextPair();
  }, 900);
}

function startFinal() {
  gameState = "playing";
  phase = "final";
  score = { azul: 0, laranja: 0 };
  timer = GAME_TIME;
  azulTeam = [
    new Robot(FIELD.x + FIELD.w * 0.20, FIELD.centerY - 70, "#41e0ff", "A1", bestPair.azul[0], "azul", null),
    new Robot(FIELD.x + FIELD.w * 0.20, FIELD.centerY + 70, "#41e0ff", "A2", bestPair.azul[1], "azul", null)
  ];
  azulTeam[0].teamRef = azulTeam;
  azulTeam[1].teamRef = azulTeam;
  laranjaTeam = [
    new Robot(FIELD.x + FIELD.w * 0.80, FIELD.centerY - 70, "#ffb347", "L1", bestPair.laranja[0], "laranja", null),
    new Robot(FIELD.x + FIELD.w * 0.80, FIELD.centerY + 70, "#ffb347", "L2", bestPair.laranja[1], "laranja", null)
  ];
  laranjaTeam[0].teamRef = laranjaTeam;
  laranjaTeam[1].teamRef = laranjaTeam;
  ball = new Ball(FIELD.centerX, FIELD.centerY);
  ballRandomInitialImpulse();
  updateScoreboard();
  updateTimer();
  if (animationFrame) cancelAnimationFrame(animationFrame);
  animationFrame = requestAnimationFrame(gameLoop);
  if (timerInterval) clearInterval(timerInterval);
  timerInterval = setInterval(timerTick, 1000);
}

function gameLoop() {
  update();
  draw();
  if (gameState === "playing") animationFrame = requestAnimationFrame(gameLoop);
}

function update() {
  azulTeam.forEach(rb => iaAreaMode(rb, azulTeam, laranjaTeam, ball, "azul"));
  laranjaTeam.forEach(rb => iaAreaMode(rb, laranjaTeam, azulTeam, ball, "laranja"));
  resolveAllRobotsCollision([...azulTeam, ...laranjaTeam]);
  for (let rb of [...azulTeam, ...laranjaTeam]) robotKickFreeAnyDir(rb, ball);
  ball.move();
  let goal = detectGoal();
  if (goal) {
    if (goal === "azul") { score.azul++; } else { score.laranja++; }
    updateScoreboard();
    showGoalMsg();
    clearInterval(timerInterval);
    setTimeout(() => {
      centerAfterGoal();
      hideGoalMsg();
      timerInterval = setInterval(timerTick, 1000);
    }, 900);
  }
}

function resolveAllRobotsCollision(all) {
  for (let i = 0; i < all.length; i++)
    for (let j = i + 1; j < all.length; j++)
      resolveRobotsCollision(all[i], all[j]);
}
function resolveRobotsCollision(r1, r2) {
  let dx = r1.x - r2.x, dy = r1.y - r2.y;
  let dist = Math.hypot(dx, dy), minDist = r1.radius + r2.radius;
  if (dist < minDist && dist > 0) {
    let overlap = (minDist - dist) / 2;
    let ox = dx / dist * overlap, oy = dy / dist * overlap;
    r1.x += ox; r1.y += oy; r2.x -= ox; r2.y -= oy;
    r1.keepInField(); r2.keepInField();
    if (Math.random() < 0.36) {
      r1.vx += ox * 0.09; r1.vy += oy * 0.09;
      r2.vx -= ox * 0.09; r2.vy -= oy * 0.09;
    }
  }
}

function detectGoal() {
  let goalLeft = (ball.x - ball.radius <= FIELD.x + FIELD.goalD) &&
    (ball.y > FIELD.centerY - FIELD.goalW / 2 && ball.y < FIELD.centerY + FIELD.goalW / 2);
  let goalRight = (ball.x + ball.radius >= FIELD.x + FIELD.w - FIELD.goalD) &&
    (ball.y > FIELD.centerY - FIELD.goalW / 2 && ball.y < FIELD.centerY + FIELD.goalW / 2);
  if (goalLeft) return "laranja";
  if (goalRight) return "azul";
  return null;
}

function centerAfterGoal() {
  azulTeam[0].x = FIELD.x + FIELD.w * 0.20; azulTeam[0].y = FIELD.centerY - 70;
  azulTeam[1].x = FIELD.x + FIELD.w * 0.20; azulTeam[1].y = FIELD.centerY + 70;
  laranjaTeam[0].x = FIELD.x + FIELD.w * 0.80; laranjaTeam[0].y = FIELD.centerY - 70;
  laranjaTeam[1].x = FIELD.x + FIELD.w * 0.80; laranjaTeam[1].y = FIELD.centerY + 70;
  for (let rb of [...azulTeam, ...laranjaTeam]) rb.vx = rb.vy = 0;
  ball.x = FIELD.centerX; ball.y = FIELD.centerY;
  ball.vx = ball.vy = 0;
  ballRandomInitialImpulse();
}

function timerTick() {
  if (gameState !== "playing") return;
  timer--;
  updateTimer();
  if (timer <= 0 || (phase === "final" && score.azul === FINAL_SCORE && score.laranja === FINAL_SCORE)) {
    clearInterval(timerInterval);
    endMatch();
  }
}

function draw() {
  CTX.clearRect(0, 0, W, H);
  drawField();
  ball.draw(CTX);
  for (let rb of azulTeam) rb.draw(CTX);
  for (let rb of laranjaTeam) rb.draw(CTX);
  CTX.save();
  CTX.font = "bold 1.2em Segoe UI";
  CTX.fillStyle = "#ffe646";
  CTX.textAlign = "center";
  CTX.shadowColor = "#41e0ff";
  CTX.shadowBlur = 9;
  if (phase === "final")
    CTX.fillText(`FINAL - Azul x Laranja`, FIELD.centerX, FIELD.y + 28);
  else
    CTX.fillText(`Geração ${gen}  |  Par ${currentPair + 1}/${POP_SIZE}`, FIELD.centerX, FIELD.y + 28);
  CTX.shadowBlur = 0;
  CTX.restore();
}

function drawField() {
  CTX.save();
  let grad = CTX.createLinearGradient(FIELD.x, FIELD.y, FIELD.x + FIELD.w, FIELD.y + FIELD.h);
  grad.addColorStop(0, "#1e80ed");
  grad.addColorStop(0.5, "#2af598");
  grad.addColorStop(1, "#0e1547");
  CTX.fillStyle = grad;
  CTX.fillRect(FIELD.x, FIELD.y, FIELD.w, FIELD.h);
  for (let i = 0; i < 10; i++) {
    if (i % 2 === 0) {
      CTX.globalAlpha = 0.13;
      CTX.fillStyle = "#fff";
      CTX.fillRect(FIELD.x, FIELD.y + (FIELD.h / 10) * i, FIELD.w, FIELD.h / 10);
      CTX.globalAlpha = 1;
    }
  }
  CTX.strokeStyle = "#fff";
  CTX.lineWidth = FIELD.lineW;
  CTX.strokeRect(FIELD.x, FIELD.y, FIELD.w, FIELD.h);
  CTX.globalAlpha = 0.7;
  CTX.fillStyle = "#232c5bcc";
  CTX.fillRect(FIELD.x, FIELD.centerY - FIELD.areaH / 2, FIELD.areaW, FIELD.areaH);
  CTX.fillRect(FIELD.x + FIELD.w - FIELD.areaW, FIELD.centerY - FIELD.areaH / 2, FIELD.areaW, FIELD.areaH);
  CTX.save();
  CTX.globalAlpha = 0.9;
  CTX.shadowColor = "#41e0ff";
  CTX.shadowBlur = 30;
  CTX.fillStyle = "#41e0ff";
  CTX.fillRect(FIELD.x - FIELD.goalD, FIELD.centerY - FIELD.goalW / 2, FIELD.goalD, FIELD.goalW);
  CTX.restore();
  CTX.save();
  CTX.globalAlpha = 0.9;
  CTX.shadowColor = "#ffb347";
  CTX.shadowBlur = 30;
  CTX.fillStyle = "#ffb347";
  CTX.fillRect(FIELD.x + FIELD.w, FIELD.centerY - FIELD.goalW / 2, FIELD.goalD, FIELD.goalW);
  CTX.restore();
  CTX.globalAlpha = 1;
  CTX.strokeStyle = "#ffe646";
  CTX.lineWidth = 5;
  CTX.strokeRect(FIELD.x - FIELD.goalD, FIELD.centerY - FIELD.goalW / 2, FIELD.goalD, FIELD.goalW);
  CTX.strokeRect(FIELD.x + FIELD.w, FIELD.centerY - FIELD.goalW / 2, FIELD.goalD, FIELD.goalW);
  CTX.beginPath();
  CTX.arc(FIELD.centerX, FIELD.centerY, 92, 0, 2 * Math.PI);
  CTX.lineWidth = 4.5;
  CTX.strokeStyle = "#fff";
  CTX.stroke();
  CTX.beginPath();
  CTX.moveTo(FIELD.centerX, FIELD.y);
  CTX.lineTo(FIELD.centerX, FIELD.y + FIELD.h);
  CTX.lineWidth = 3.2;
  CTX.stroke();
  CTX.fillStyle = "#fff";
  CTX.beginPath();
  CTX.arc(FIELD.x + 90, FIELD.centerY, 6, 0, 2 * Math.PI);
  CTX.arc(FIELD.x + FIELD.w - 90, FIELD.centerY, 6, 0, 2 * Math.PI);
  CTX.fill();
  CTX.restore();
}

function showGoalMsg() {
  goalMsg.style.display = "block";
  goalMsg.style.animation = "goalPop 1s cubic-bezier(.5,2,.5,1)";
}
function hideGoalMsg() {
  goalMsg.style.display = "none";
  goalMsg.style.animation = "";
}
function endMatch() {
  gameState = "end";
  if (timerInterval) clearInterval(timerInterval);
  if (animationFrame) cancelAnimationFrame(animationFrame);
  let msg = "";
  if (score.azul > score.laranja) msg = "Time Azul venceu!";
  else if (score.azul < score.laranja) msg = "Time Laranja venceu!";
  else msg = "Empate!";
  let statsMsg = `
    <b>Placar final:</b> ${score.azul} x ${score.laranja}<br>
    ${score.azul === score.laranja ? "<br><i>Empate! Fim da Final.</i>" : ""}
  `;
  endgameTitle.innerHTML = msg;
  endgameStats.innerHTML = statsMsg;
  endgameScreen.style.display = "block";
  restartBtn.disabled = false;
  finalBtn.disabled = false;
  if (phase === "evolution" && currentPair < population.length - 1) {
    currentPair++;
    setTimeout(() => {
      if (gameState === "end") {
        playNextPair();
        endgameScreen.style.display = "none";
      }
    }, 400);
  } else if (phase === "evolution" && currentPair >= population.length - 1) {
    setTimeout(() => {
      if (gameState === "end") {
        evolveGeneration();
        endgameScreen.style.display = "none";
      }
    }, 400);
  }
}
function updateScoreboard() {
  scoreAzulEl.textContent = score.azul;
  scoreLaranjaEl.textContent = score.laranja;
}
function updateTimer() {
  let min = Math.floor(timer / 60);
  let sec = timer % 60;
  timerEl.textContent = `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
}
function updateGenUI() {
  genCounter.textContent = gen;
  bestScoreEl.textContent = bestScore;
}
function clamp(val, min, max) { return Math.max(min, Math.min(max, val)); }

startEvolBtn.onclick = () => {
  if (gameState !== "playing") {
    initPopulation();
    updateScoreboard();
    updateTimer();
    startEvolBtn.disabled = true;
    finalBtn.disabled = true;
    restartBtn.disabled = false;
    if (animationFrame) cancelAnimationFrame(animationFrame);
    if (timerInterval) clearInterval(timerInterval);
    startEvolution();
  }
};
finalBtn.onclick = () => {
  if (phase !== "final" && bestPair) {
    endgameScreen.style.display = "none";
    startFinal();
    finalBtn.disabled = true;
  }
};
restartBtn.onclick = () => {
  startEvolBtn.disabled = false;
  restartBtn.disabled = true;
  finalBtn.disabled = true;
  if (animationFrame) cancelAnimationFrame(animationFrame);
  if (timerInterval) clearInterval(timerInterval);
  initPopulation();
  updateScoreboard();
  updateTimer();
  endgameScreen.style.display = "none";
};
endgameRestartBtn.onclick = restartBtn.onclick;

window.addEventListener('resize', resizeCanvas, false);
function resizeCanvas() {
  let maxW = Math.min(window.innerWidth * 0.99, 1200);
  let scale = maxW / 1200;
  CANVAS.style.width = `${maxW}px`;
  CANVAS.style.height = `${600 * scale}px`;
}
resizeCanvas();

initPopulation();
draw();