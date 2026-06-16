<h1 align="center">
  <br>
  <a href="https://www.youtube.com/@Rails-Evolution-18xx">
<img src="./src/main/resources/assets/logo.jpeg" alt="Rails Evolution Logo" width="300">
  </a>
  <br>
  Rails Evolution - Digital 18xx Engine
  <br>
</h1>

<p align="center">
  <i>Shifting the focus from bookkeeping to strategic thinking.</i>
</p>

Welcome to Rails Evolution. Originally branching from the dormant Rails-18xx repository, this project represents a massive 6-month, 100,000+ line architectural rewrite.

The core objective is a "Dual Mandate":
1. Upgrade the engine from a static, manual ledger into a dynamic, living ecosystem capable of supporting synchronous, championship-level competitive play.
2. Act as the definitive, modernized "Rails 2.0" engine to support and preserve the entire legacy catalog of 18xx titles.

By drastically reducing cognitive load and visual strain, a full championship game can now be played in two hours instead of three—with absolute stability and zero UI jitter.

📺 **See the Engine, UI, and AI in action on YouTube:** [Rails Evolution 18xx](https://www.youtube.com/@Rails-Evolution-18xx)

---

## 🚂 The Game Catalog

Rails Evolution supports a wide array of 18xx titles, categorized by their current stability and testing status in the new engine.

### Championship Ready (Stable)
These titles have been thoroughly stress-tested and optimized for competitive tournament play.
* **1835** (Includes Standard, Clemens, and Snake variants)
* **1817** (Native implementation with short selling, liquidations, and mergers)

### Beta Phase (Playable, requires stress testing)
These legacy titles are fully integrated into the new architecture using their original logic, but require community playtesting to guarantee 100% stability.
* 1830: Railways & Robber Barons
* 1837 (Austria)
* 18Chesapeake
* 1889: History of Shikoku Railways
* 1825, 1826, 1851, 1862, 1880
* 18AL, 18EU, 18GA, 18Kaas, 18Lummer, 18NL, 18Scan, 18TN, 18VA
* Steam over Holland

### Alpha / Under Construction
Titles currently being ported or awaiting UI/UX modernization.
* 1870
* 1856: Railroading in Upper Canada

---

## 🖥️ The UI, UX, and Cognitive Load Revolution

The interface has been completely overhauled to create a rock-steady environment. There is now a single source of truth for all information, eliminating duplicated and cluttered text.

* **Railcards & Animation:** The tactile board game experience is back. Certificates are physical-looking beige "Railcards." When transferred, they physically animate across the screen to ensure all players can track the flow of equity on a shared screen.
* **The Linear OR Panel:** The old, cluttered OR panel is replaced by a strict, top-to-bottom linear sequence (Build > Marker > Revenue > Buy > Special). Inactive steps are grayed out.
* **Synchronous Play Tools:** Integrated chess clocks display active thinking time. A massive gray "Game Paused" overlay ensures tournament integrity.
* **Bulletproof Corrections:** Standardized `Cmd+Z` / `Cmd+Y` handles seamless undo/redo, supported by completely rebuilt correction managers.

---

## 🧠 The Native AI Engine

The decision logic has been completely decoupled to build an autonomous "brain." This is a hybrid engine designed for human interaction, AI building assistance, and headless dry-run simulations.

* **Data-Driven "Opening Books":** The AI loads external JSON strategy files to guide draft picks and opening moves based on historical performance.
* **Dry-Run Simulations:** Utilizes the `RevenueAdapter` to spin up parallel network graphs, allowing the AI to simulate and score potential tile lays before committing.
* **AI Building Assistant:** In-game, the AI can be invoked to instantly calculate and build the tile that maximizes current train income.

---

## 🤝 Contributing

Rails Evolution is an open-source project. We are actively seeking Java developers to help migrate the Alpha catalog into the Beta phase and veterans to stress-test the engine.
