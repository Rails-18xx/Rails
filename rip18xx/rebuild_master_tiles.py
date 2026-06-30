#!/usr/bin/env python3
"""
Filename: rip18xx/rebuild_master_tiles.py
Description: Scrapes all game-specific XML tile manifests to generate a clean,
             unified, error-audited global master Tiles.xml file. 
             Provides clear side-by-side structural diff prints for conflicting pairs.
"""

import os
import xml.etree.ElementTree as ET
import sys
from xml.dom import minidom

def prettify_node(elem):
    """Returns a clean, pretty-printed string of an XML element block for visual comparison."""
    rough_string = ET.tostring(elem, "utf-8")
    reparsed = minidom.parseString(rough_string)
    # Strip the XML declaration line <?xml ... ?> for a cleaner console display
    lines = reparsed.toprettyxml(indent="  ").split('\n')
    return '\n'.join([line for line in lines if not line.startswith('<?xml')])

def normalize_element(elem):
    """Generates a canonical string representation of an element to check functional identity."""
    tag = elem.tag
    attrs = " ".join(f'{k}="{v}"' for k, v in sorted(elem.attrib.items()) if k != "name")
    children = "".join(normalize_element(child) for child in sorted(elem, key=lambda c: (c.tag, str(c.attrib))))
    return f"<{tag} {attrs}>{children}</{tag}>"

def rebuild_and_audit_tiles(data_dir, output_master_path):
    print(f"[*] Beginning workspace master tile scrape across: {data_dir}")
    
    # Structure: { tile_id: [(normalized_signature, raw_element_node, source_file_path)] }
    tile_database = {}
    duplicates_count = 0
    errors_count = 0

    if not os.path.exists(data_dir):
        raise FileNotFoundError(f"Directory '{data_dir}' does not exist.")

    # 1. Crawl all directories for game configuration manifests
    for root_dir, _, files in os.walk(data_dir):
        for file in files:
            if file.lower() == "tiles.xml":
                file_path = os.path.abspath(os.path.join(root_dir, file))
                try:
                    tree = ET.parse(file_path)
                    root = tree.getroot()
                    
                    if root.tag != "Tiles":
                        continue
                        
                    for tile_node in root.findall("Tile"):
                        tile_id = tile_node.get("id")
                        if not tile_id:
                            continue
                            
                        sig = normalize_element(tile_node)
                        if tile_id not in tile_database:
                            tile_database[tile_id] = []
                        tile_database[tile_id].append((sig, tile_node, file_path))
                        
                except ET.ParseError as e:
                    print(f"[!] XML Parsing Error in file {file_path}: {e}", file=sys.stderr)

    # 2. Compile unique nodes and flag structural layout conflicts visibly
    master_root = ET.Element("Tiles")
    conflict_reports = []
    
    for tile_id in sorted(tile_database.keys(), key=lambda x: int(x) if x.replace('-', '').isdigit() else 9999):
        entries = tile_database[tile_id]
        first_sig, first_node, first_path = entries[0]
        
        # Check if subsequent instances found are identical to the baseline instance
        for next_sig, next_node, next_path in entries[1:]:
            duplicates_count += 1
            if next_sig != first_sig:
                errors_count += 1
                conflict_reports.append((tile_id, first_path, first_node, next_path, next_node))
        
        # Append the first parsed definition as the baseline reference in the master pool
        master_root.append(first_node)

    # 3. Print the visually isolated pair reports
    if conflict_reports:
        print("\n" + "="*80)
        print(f"CRITICAL STRUCTURAL MISMATCHES DETECTED ({errors_count} CONFLICTING PAIRS)")
        print("="*80)
        for idx, (tid, path_a, node_a, path_b, node_b) in enumerate(conflict_reports, 1):
            print(f"\n[CONFLICT PAIR #{idx}] Tile ID: {tid}")
            print(f"--> DEFINITION A PATH: {path_a}")
            print(f"--> DEFINITION B PATH: {path_b}")
            print("\n--- VISUAL XML DIFF PAIR ---")
            print("--- [DEFINITION A XML] ---")
            print(prettify_node(node_a).strip())
            print("\n--- [DEFINITION B XML] ---")
            print(prettify_node(node_b).strip())
            print("-"*80)
    else:
        print("\n[+] Success: All duplicated tile references across the workspace are functionally identical.")

    print("\n=== WORKSPACE AUDIT SUMMARY ===")
    print(f"[*] Processed {len(tile_database)} unique Tile profiles.")
    print(f"[*] Audited {duplicates_count} total duplicate occurrences.")
    print(f"[!] Total Non-Identical Error Mismatches: {errors_count}")
    print("===============================\n")

    # 4. Write out the verified clean master array baseline template
    os.makedirs(os.path.dirname(output_master_path), exist_ok=True)
    rough_string = ET.tostring(master_root, "utf-8")
    pretty_payload = minidom.parseString(rough_string).toprettyxml(indent="\t")
    
    with open(output_master_path, "w", encoding="utf-8") as f:
        f.write(pretty_payload)
    print(f"[+] Consolidated master layout written to: {output_master_path}\n")

if __name__ == "__main__":
    WORKSPACE_DATA_DIR = "src/main/resources/data"
    GLOBAL_MASTER_PATH = "src/main/resources/tiles/Tiles.xml"
    rebuild_and_audit_tiles(WORKSPACE_DATA_DIR, GLOBAL_MASTER_PATH)