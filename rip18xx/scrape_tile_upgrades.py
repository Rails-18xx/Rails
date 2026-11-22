#!/usr/bin/env python3
"""
Filename: rip18xx/scrape_tile_upgrades.py
Description: Crawls a directory of working Rails XML tile sets to build a 
             master tile upgrade relationship JSON map.
"""

import os
import xml.etree.ElementTree as ET
import json
import sys

def scrape_reference_upgrades(data_dir):
    master_map = {}

    if not os.path.exists(data_dir):
        print(f"Error: Directory '{data_dir}' does not exist.", file=sys.stderr)
        return master_map

    # Scan for all XML files containing Tile definitions
    for root_dir, _, files in os.walk(data_dir):
        for file in files:
            if not file.endswith(".xml"):
                continue
            
            file_path = os.path.join(root_dir, file)
            try:
                tree = ET.parse(file_path)
                root = tree.getroot()
                
                if root.tag != "TileManager":
                    continue
                
                for tile_node in root.findall("Tile"):
                    tile_id = tile_node.get("id")
                    if not tile_id:
                        continue
                        
                    upgrade_node = tile_node.find("Upgrade")
                    if upgrade_node is not None:
                        upgrade_ids = upgrade_node.get("id", "").strip()
                        if upgrade_ids:
                            # Split, sanitize, and merge tracking lists
                            new_upgrades = [uid.strip() for uid in upgrade_ids.split(",") if uid.strip()]
                            
                            if tile_id not in master_map:
                                master_map[tile_id] = set()
                            master_map[tile_id].update(new_upgrades)
                            
            except (ET.ParseError, IOError) as e:
                # Skip invalid or non-tile xml configs gracefully
                continue

    # Convert sets back to sorted comma-separated strings for serialization
    final_serialized_map = {tid: ",".join(sorted(list(upgrades))) for tid, upgrades in master_map.items()}
    return final_serialized_map

if __name__ == "__main__":
    # Point this to the directory containing your working XML datasets (e.g., src/main/resources/data/)
    TARGET_DATA_DIR = "src/main/resources/data"
    OUTPUT_JSON = "rip18xx/master_tile_upgrades.json"
    
    print(f"Scraping valid tile upgrade relationships from: {TARGET_DATA_DIR}")
    upgrades_matrix = scrape_reference_upgrades(TARGET_DATA_DIR)
    
    os.makedirs(os.path.dirname(OUTPUT_JSON), exist_ok=True)
    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(upgrades_matrix, f, indent=4, sort_keys=True)
        
    print(f"Successfully compiled {len(upgrades_matrix)} tile mappings into {OUTPUT_JSON}")