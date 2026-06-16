#!/bin/bash

# Create output directory if it doesn't exist
mkdir -p logs/headless_runs

# Run 100 games
for i in {1..200}; do
    echo "----------------------------------------------------------------"
    echo ">>> STARTING BATCH GAME #$i <<<"
    echo "----------------------------------------------------------------"
    
    # FIX: 
    # 1. Use the ACTUAL Gradle command (not ./run_headless.sh)
    # 2. Use '-q' (quiet) to suppress Gradle headers so you see the dots clearly
    # 3. Use '| tee' so output goes to the screen AND the log file
    
    ./gradlew -q run -PmainClass=net.sf.rails.game.ai.playground.HeadlessRunner | tee "logs/headless_runs/game_${i}.log"
    
    # Check if the CSV grew
    if [ -f training_data.csv ]; then
        ROW_COUNT=$(wc -l < training_data.csv)
        echo ">>> Game $i Finished. Total Data Rows: $ROW_COUNT"
    else
        echo ">>> Game $i Finished. (No CSV found yet)"
    fi
    
    # Small cool-down
    sleep 1
done