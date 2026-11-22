Welcome to **18xx Rails Evolution**. If you love the deep strategy of 18xx board games but find the physical upkeep, mental math, and sheer length of the games exhausting, you are in the right place.

Originally branching from the dormant Rails-18xx repository, this project represents a massive architectural rewrite designed to bring 18xx into the modern digital era. Whether you are a veteran preparing for the next World Championship or a tabletop player looking to get more games to the table, Rails Evolution provides a rock-steady, zero-jitter environment that lets you play a full, highly competitive game in two hours instead of three.

---

## 🚀 The Digital Experience: Tabletop Strategy, Engine Precision

Transitioning from the physical board to Rails Evolution fundamentally changes how you experience the game. We have removed the mechanical friction of 18xx so you can focus entirely on outmaneuvering your opponents.



* **Absolute Information Symmetry:** The physical ambiguity of obscured cash piles, mental math, and overlapping share certificates is eliminated. The Game Status matrix displays exact player capital, certificate limits, company treasuries, and train distributions in a single, continuously updated view.
* **Algorithmic Automation:** The engine acts as the perfect banker and rules enforcer. It autonomously calculates optimal maximum route revenues, executes exact fractional dividend payouts, and strictly enforces legal tile orientations and phase obsolescence.
* **Curated Spatial Layout:** Critical game states are never hidden behind nested menus. The map, stock market index, corporate status matrices, and available tile pools are positioned simultaneously on-screen, allowing you to instantly cross-reference market data with geographical positioning.
* **Accelerated Pacing:** By bypassing the physical distribution of paper money, making change, and swapping out scrapped trains, your mental energy is reserved purely for strategy.

📺 **See the Engine in Action!** Want to see how it feels? Check out our [Rails Evolution YouTube Channel](https://www.youtube.com/@Rails-Evolution-18xx) to watch full playthroughs, including a [high-level 1835 Championship-style match](https://www.youtube.com/watch?v=lYlzwSVrk54).

---

## 🚂 The Game Catalog

Rails Evolution acts as the definitive engine to support and preserve the legacy catalog of 18xx titles.

### Championship Ready (Stable)

These titles have been thoroughly stress-tested and optimized for competitive tournament play—perfect for preparing for online events like the 1835 World Championship.

* **1835** (Includes Standard, Clemens, and Snake variants)
* **1817** (Native implementation with short selling, liquidations, and mergers)
* **1870** * **1830:** The original, Railways & Robber Barons
* **1837** (Austria)
* **18Chesapeake** ### Beta Phase (Playable, requires stress testing)
Fully integrated using their original logic, but require community playtesting to guarantee 100% stability.
* 1889: History of Shikoku Railways
* 1825, 1826, 1851, 1862, 1880
* 18AL, 18EU, 18GA, 18Kaas, 18Lummer, 18NL, 18Scan, 18TN
* Steam over Holland

### Alpha / Under Construction

* 1856: Railroading in Upper Canada
* 18VA

---

## ⚙️ Under the Hood: Technical Architecture

For developers and power users, the "Rails 2.0" engine represents over 100,000 lines of rewritten architecture aimed at stability, synchronous play, and AI integration.

### The UI, UX, and Cognitive Load Revolution

* **Railcards & Animation:** The tactile board game experience is preserved. Certificates are physical-looking beige "Railcards." When transferred, they physically animate across the screen to ensure all players can track the flow of equity.
* **The Linear OR Panel:** The old, cluttered OR panel is replaced by a strict, top-to-bottom linear sequence (Build > Marker > Revenue > Buy > Special). Inactive steps are grayed out.
* **Synchronous Play Tools:** Integrated chess clocks display active thinking time. A massive gray "Game Paused" overlay ensures tournament integrity.
* **Bulletproof Corrections:** Standardized `Cmd+Z` / `Cmd+Y` handles seamless undo/redo, supported by completely rebuilt chronological event logs and correction managers.

### The Native AI Engine

The decision logic has been completely decoupled to build an autonomous "brain." This hybrid engine is designed for human interaction, AI building assistance, and headless dry-run simulations.

* **Data-Driven "Opening Books":** The AI loads external JSON strategy files to guide draft picks and opening moves based on historical performance.
* **Dry-Run Simulations:** Utilizes the `RevenueAdapter` to spin up parallel network graphs, allowing the AI to simulate and score potential tile lays before committing.
* **AI Building Assistant:** In-game, the AI can be invoked to instantly calculate and build the tile that maximizes current train income.

---

## 🤝 Contributing

Rails Evolution is a community-driven, open-source project. We are actively seeking Java developers to help migrate the Alpha catalog into the Beta phase, and veteran players to stress-test the engine and refine our competitive implementations. Download the latest release, fire up a game, and join the evolution.
