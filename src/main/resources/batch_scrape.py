import sys
import os
import scrape

def run_batch_extraction():
    # Ensure the base directory exists
    base_dir = os.path.join(".", "src", "main", "resources", "gamespecifics")
    os.makedirs(base_dir, exist_ok=True)
    
    for game in scrape.SINGLE_WORD_GAMES:
        if game in ["All others", "Rest"]:
            continue
            
        print(f"--- Starting extraction for: {game} ---")
        try:
            # The run_extractor function in scrape.py will handle 
            # its own specific sub-directory creation
            scrape.run_extractor(game)
        except Exception as e:
            print(f"Failed to extract {game}: {e}")

if __name__ == "__main__":
    run_batch_extraction()