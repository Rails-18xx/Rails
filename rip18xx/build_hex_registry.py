#!/usr/bin/env python3
"""
Filename: build_hex_registry.py
Description: Crawls the Rails-18xx Java repository to extract preprinted map hex IDs.
             Generates a universal JSON registry to map game-specific coordinates to Java's negative tile IDs.
"""

import os
import glob
import json
import xml.etree.ElementTree as ET

def build_registry(base_dir="src/main/resources/data", output_file="universal_hex_registry.json"):
    registry = {}
    
    # Locate all Map.xml files in the game data subdirectories
    search_pattern = os.path.join(base_dir, "*", "Map.xml")
    map_files = glob.glob(search_pattern)
    
    if not map_files:
        print(f"Error: No Map.xml files found in {base_dir}/*/")
        return

    for map_file in map_files:
        # Extract the game ID from the directory name (e.g., '1830' from 'data/1830/Map.xml')
        game_id = os.path.basename(os.path.dirname(map_file))
        
        try:
            tree = ET.parse(map_file)
            root = tree.getroot()
            
            game_hexes = {}
            # Use XPath to find all Hex elements, even those nested inside <IfOption> variants
            for hex_node in root.findall(".//Hex"):
                hex_name = hex_node.get("name")
                tile_id = hex_node.get("tile")
                
                # We only want to catalog explicitly preprinted infrastructure (negative IDs)
                # We skip "0" (empty hexes) and positive IDs (standard track)
                if hex_name and tile_id and tile_id.startswith("-"):
                    game_hexes[hex_name] = tile_id
                    
            if game_hexes:
                registry[game_id] = game_hexes
                print(f"Processed {game_id}: Found {len(game_hexes)} preprinted hexes.")
                
        except ET.ParseError as e:
            print(f"XML Parsing Error in {map_file}: {e}")
        except Exception as e:
            print(f"Unexpected error processing {map_file}: {e}")

    # Write the compiled dictionary to the JSON payload
    try:
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(registry, f, indent=4, sort_keys=True)
        print(f"\nSuccess: Registry compiled and saved to {output_file}")
    except IOError as e:
        print(f"File Write Error: {e}")

if __name__ == "__main__":
    # Assumes the script is run from the root of the Rails repository.
    # Adjust base_dir if the script is placed inside a subdirectory like rip18xx/
    build_registry(base_dir="src/main/resources/data", output_file="rip18xx/universal_hex_registry.json")