# 🎮 Enhanced Pong Game — Java Swing

This project is an **advanced Pong game** built using **Java Swing**, featuring:

✅ Smart AI opponent  
✅ Multiple Power-ups  
✅ Visual effects & timer UI  
✅ Ability cooldown system  
✅ Game states: Start, Pause, Game Over  
✅ Cleaner UI + Smooth gameplay  

This version goes beyond classic Pong by adding modern arcade mechanics and strategy-based gameplay.

---

## 🚀 Features

### 🧠 AI System
- Tracks ball movement  
- **CONFUSE AI** power-up disables AI logic temporarily  

### ⚡ Player Abilities
Press `SPACE` to trigger a special power (cooldown-based)

### 🎁 Power-Ups

| Power-Up | Symbol | Effect |
|---------|--------|--------|
| Paddle Size Boost | `P` | Makes paddle larger |
| Slow Ball | `S` | Reduces ball speed |
| Multi-Ball | `M` | Spawns extra balls |
| Speed Boost | `B` | Ball accelerates |
| Magnet | `G` | Player paddle pulls ball |
| Confuse AI | `C` | Disrupts AI movement |

Timers appear in the bottom-right corner.

---

## 🎨 Controls

| Key | Action |
|----|--------|
| W | Move Up |
| S | Move Down |
| Space | Activate Ability |
| P | Pause Game |
| R | Restart Game |
| Any key (on start) | Begin |

---

## 🖥️ How to Run

### ✅ Requirements
- Java JDK **8+**
- Any Java IDE (IntelliJ / Eclipse / NetBeans) *or* terminal

### ▶️ Run via Terminal

```bash
javac EnhancedPongGame.java
java EnhancedPongGame
