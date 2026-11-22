#!/usr/bin/env python3
"""
Filename: rip18xx/generate_rails_xml.py
Description: Compiles 18xx.games Ruby structures into clean Java resource XML targets.
             Uses an external master upgrade JSON map for precise tile promotions.
"""

import os
import re
import xml.etree.ElementTree as ET
from xml.dom import minidom
import json




def prettify_xml(element):
    rough_string = ET.tostring(element, "utf-8")
    pretty_str = minidom.parseString(rough_string).toprettyxml(indent="\t")
    return "\n".join([line for line in pretty_str.splitlines() if line.strip()])

class RailsXMLConverter:
    def __init__(self, game_id, source_dir, output_dir, relative_resource_path):
        self.game_id = game_id
        self.source_dir = source_dir
        self.output_dir = output_dir
        self.relative_resource_path = relative_resource_path
        self.game_rb = ""
        self.map_rb = ""
        self.entities_rb = ""
        self.bank_cash = 10000
        self.cert_limits = {}
        self.starting_cash = {}
        self.trains = []
        self.phases = []
        self.companies = []
        self.corporations = []
        self.market_prices = []
        self.location_names = {}
        self.tiles_manifest = []
        self.active_coords = set()
        self.hex_details = {}

    def load_source_files(self):
        with open(os.path.join(self.source_dir, "game.rb"), "r", encoding="utf-8") as f:
            self.game_rb = f.read()
        with open(os.path.join(self.source_dir, "map.rb"), "r", encoding="utf-8") as f:
            self.map_rb = f.read()
        with open(os.path.join(self.source_dir, "entities.rb"), "r", encoding="utf-8") as f:
            self.entities_rb = f.read()

    def parse_all(self):
        self.load_source_files()
        bank_match = re.search(r"BANK_CASH\s*=\s*([\d_]+)", self.game_rb)
        if bank_match:
            self.bank_cash = int(bank_match.group(1).replace("_", ""))
        self.extract_cert_limits()
        self.extract_starting_cash()
        self.extract_market()
        self.extract_trains()
        self.extract_phases()
        self.extract_entities()
        self.extract_location_names()
        self.extract_tiles_manifest()
        self._populate_map_data()

    def extract_cert_limits(self):
        match = re.search(r"CERT_LIMIT\s*=\s*\{([^}]+)\}", self.game_rb)
        if match:
            for pair in re.findall(r"(\d+)\s*=>\s*(\d+)", match.group(1)):
                self.cert_limits[int(pair[0])] = int(pair[1])

    def extract_starting_cash(self):
        match = re.search(r"STARTING_CASH\s*=\s*\{([^}]+)\}", self.game_rb)
        if match:
            for pair in re.findall(r"(\d+)\s*=>\s*(\d+)", match.group(1)):
                self.starting_cash[int(pair[0])] = int(pair[1])

    def extract_market(self):
        match = re.search(r"MARKET\s*=\s*\[\s*%w\[([^\]]+)\]", self.game_rb)
        if match:
            self.market_prices = match.group(1).split()

    def extract_trains(self):
        train_blocks = re.findall(r"\{\s*name:\s*'([^']+)'.*?price:\s*(\d+).*?num:\s*([^,\s\}]+)", self.game_rb, re.DOTALL)
        for m in train_blocks:
            self.trains.append({"name": m[0], "cost": m[1], "quantity": m[2].replace("'", "")})

    def extract_phases(self):
        self.phases = []
        # Isolate the whole PHASES array content block
        phases_match = re.search(r"PHASES\s*=\s*\[(.*?)\]\.freeze", self.game_rb, re.DOTALL)
        if not phases_match:
            return

        # Find individual hash objects inside the array
        raw_blocks = re.findall(r"\{[^}]+\}", phases_match.group(1), re.DOTALL)
        for block in raw_blocks:
            name_match = re.search(r"name:\s*'([^']+)'", block)
            rounds_match = re.search(r"operating_rounds:\s*(\d+)", block)
            
            if name_match and rounds_match:
                name = name_match.group(1)
                rounds = rounds_m = rounds_match.group(1)
                
                # Capture colors from format: tiles: [:yellow] or tiles: %i[yellow green]
                tiles_match = re.search(r"tiles:\s*(?:\[:([\w\s,:]+)\]|%i\[([\w\s]+)\])", block)
                color_list = []
                if tiles_match:
                    if tiles_match.group(1):
                        color_list = [c.strip().replace(":", "") for c in tiles_match.group(1).split(",")]
                    elif tiles_match.group(2):
                        color_list = tiles_match.group(2).split()
                
                # Fallback default color if parsing missed it
                if not color_list:
                    color_list = ["yellow"]
                    
                self.phases.append({
                    "name": name,
                    "rounds": rounds,
                    "tiles": ",".join(color_list)
                })

    def extract_entities(self):
        comp_blocks = re.findall(r"name:\s*'([^']+)'.*?value:\s*(\d+).*?revenue:\s*(\d+).*?sym:\s*'([^']*)'", self.entities_rb, re.DOTALL)
        for m in comp_blocks:
            self.companies.append({"longname": m[0], "basePrice": m[1], "revenue": m[2], "name": m[3]})
        corp_blocks = re.findall(r"sym:\s*'([^']+)'.*?name:\s*'([^']+)'.*?tokens:\s*\[([^\]]+)\].*?coordinates:\s*'([^']+)'", self.entities_rb, re.DOTALL)
        for m in corp_blocks:
            token_count = len(m[2].split(","))
            self.corporations.append({"sym": m[0], "name": m[1], "tokens": token_count, "home": m[3]})

    def extract_location_names(self):
        match = re.search(r"LOCATION_NAMES\s*=\s*\{(.*?)\}\.freeze", self.map_rb, re.DOTALL)
        if match:
            for pair in re.findall(r"'([^']*)'\s*=>\s*'([^']*)'", match.group(1)):
                self.location_names[pair[0]] = pair[1]

        # 1. Load the reference master layouts to enrich simple manifest tiles
        master_lookup = {}
        master_tiles_path = "src/main/resources/tiles/Tiles.xml"
        if os.path.exists(master_tiles_path):
            try:
                tree = ET.parse(master_tiles_path)
                for tile_node in tree.getroot().findall("Tile"):
                    t_id = tile_node.get("id")
                    if t_id:
                        color = tile_node.get("colour", "standard")
                        stations = []
                        for st in tile_node.findall("Station"):
                            stations.append({
                                "type": st.get("type", "City"),
                                "slots": st.get("slots", "0")
                            })
                        paths = []
                        for tr in tile_node.findall("Track"):
                            paths.append((tr.get("from"), tr.get("to")))
                        master_lookup[t_id] = {"color": color, "stations": stations, "paths": paths}
            except Exception:
                pass

        # 2. Parse basic tiles from game.rb/map.rb
        simple_tiles = re.findall(r"'(\d+)'\s*=>\s*(\d+)", self.game_rb + self.map_rb)
        for tile_id, qty in simple_tiles:
            master_data = master_lookup.get(tile_id, {"color": "standard", "paths": [], "stations": []})
            self.tiles_manifest.append({
                "id": tile_id,
                "quantity": qty,
                "color": master_data["color"],
                "paths": master_data["paths"],
                "stations": master_data["stations"]
            })



    def build_game_xml(self):
        root = ET.Element("ComponentManager")
        gm = ET.SubElement(root, "Component", name="GameManager")
        gm.set("class", "net.sf.rails.game.GameManager")
        ET.SubElement(gm, "Game", name=self.game_id)
        params = ET.SubElement(gm, "GameParameters")
        ET.SubElement(params, "PlayerShareLimit", percentage="60")
        ET.SubElement(params, "BankPoolLimit", percentage="100")
        op_round = ET.SubElement(params, "OperatingRound")
        ET.SubElement(op_round, "EmergencyTrainBuying", mustBuyCheapestTrain="yes", mayBuyFromCompany="yes")
        end_game = ET.SubElement(gm, "EndOfGame")
        ET.SubElement(end_game, "Bankruptcy")
        ET.SubElement(end_game, "BankBreaks", limit="0", finish="setOfORs")

        pm = ET.SubElement(root, "Component", name="PlayerManager")
        pm.set("class", "net.sf.rails.game.PlayerManager")
        for p_count, cert in self.cert_limits.items():
            cash = self.starting_cash.get(p_count, 500)
            ET.SubElement(pm, "Players", number=str(p_count), cash=str(cash), certLimit=str(cert))

        bank = ET.SubElement(root, "Component", name="Bank")
        bank.set("class", "net.sf.rails.game.financial.Bank")
        ET.SubElement(bank, "Bank", amount=str(self.bank_cash))

        ET.SubElement(root, "Component", name="TileManager", file="TileSet.xml").set("class", "net.sf.rails.game.TileManager")
        ET.SubElement(root, "Component", name="Map", file="Map.xml").set("class", "net.sf.rails.game.MapManager")
        ET.SubElement(root, "Component", name="CompanyManager", file="CompanyManager.xml").set("class", "net.sf.rails.game.CompanyManager")
        ET.SubElement(root, "Component", name="StockMarket", file="StockMarket.xml").set("class", "net.sf.rails.game.financial.StockMarket")

        tm = ET.SubElement(root, "Component", name="TrainManager")
        tm.set("class", "net.sf.rails.game.TrainManager")
        defaults = ET.SubElement(tm, "Defaults")
        ET.SubElement(defaults, "Reach", base="stops", countTowns="yes")
        ET.SubElement(defaults, "Score", towns="yes")
        for t in self.trains:
            qty = "6" if t["quantity"] == "unlimited" else t["quantity"]
            major_stops = t["name"].split("+")[0]
            tt = ET.SubElement(tm, "TrainType", name=t["name"], majorStops=major_stops, cost=t["cost"], quantity=str(qty))
            if "+" in t["name"]:
                ET.SubElement(tt, "NewPhase", phaseName=major_stops)

        phm = ET.SubElement(root, "Component", name="PhaseManager")
        phm.set("class", "net.sf.rails.game.PhaseManager")
        for p in self.phases:
            ph = ET.SubElement(phm, "Phase", name=p["name"])
            ET.SubElement(ph, "Tiles", colour=p.get("tiles", "yellow"))
            ET.SubElement(ph, "OperatingRounds", number=p["rounds"])

        rev_m = ET.SubElement(root, "Component", name="RevenueManager")
        rev_m.set("class", "net.sf.rails.algorithms.RevenueManager")
        return prettify_xml(root)




    def build_company_manager_xml(self):
        root = ET.Element("CompanyManager")
        
        # 1. Private Company Type Setup
        private_type = ET.SubElement(root, "CompanyType", name="Private")
        private_type.set("class", "net.sf.rails.game.PrivateCompany")
        ET.SubElement(private_type, "Tradeable", toCompany="yes", lowerPriceFactor="0.5", upperPriceFactor="2.0")
        ET.SubElement(private_type, "Tradeable", toPlayer="yes")
        
        # 2. Public Major Company Type Setup (Injected with working lifecycle configs)
        major_type = ET.SubElement(root, "CompanyType", name="Major")
        major_type.set("class", "net.sf.rails.game.PublicCompany")
        ET.SubElement(major_type, "CanBuyPrivates")
        ET.SubElement(major_type, "PoolPaysOut")
        ET.SubElement(major_type, "Float", percentage="50") # 1860 floats at 50%
        
        # Set up default token placement timing and progressive pricing
        base_tokens = ET.SubElement(major_type, "BaseTokens")
        ET.SubElement(base_tokens, "HomeBase", lay="whenFloated")
        ET.SubElement(base_tokens, "LayCost", method="sequence", cost="0,40,100,100")
        
        # Global share layout template rules for Public Corporations
        shares_def = ET.SubElement(major_type, "Shares", unit="10")
        ET.SubElement(shares_def, "Certificate", type="President", shares="2")
        ET.SubElement(shares_def, "Certificate", shares="1", number="8")
        
        # 1860 Train Limit rules (4, 4, 3, 3, 2, 2, 2, 2 depending on phase progression)
        ET.SubElement(major_type, "Trains", limit="4,4,3,3,2,2,2,2")
        ET.SubElement(major_type, "StockPrice", par="yes")

        # 3. Append Private Companies
        for c in self.companies:
            ET.SubElement(root, "Company", 
                          name=c["name"], 
                          longname=c["longname"], 
                          type="Private", 
                          basePrice=c["basePrice"], 
                          revenue=c["revenue"])
            
        # 4. Append Public Corporations with correct tracking tokens and unique symbols
        for corp in self.corporations:
            co = ET.SubElement(root, "Company", 
                              name=corp["sym"], 
                              longname=corp["name"], 
                              type="Major")
            ET.SubElement(co, "Home", hex=corp["home"])
            
            # Explicitly embed individual share certificates inside each major company
            co_shares = ET.SubElement(co, "Shares", unit="10")
            ET.SubElement(co_shares, "Certificate", type="President", shares="2")
            ET.SubElement(co_shares, "Certificate", shares="1", number="8")
            
        return prettify_xml(root)


    def build_stock_market_xml(self):
        root = ET.Element("StockMarket", type="linear")
        
        # Mapping 18xx.games suffix designators to generic Rails StockSpaceType definitions
        type_mapping = {
            "c": "liquidation",   # Closed / Bankrupt zone
            "i": "acquisition",   # Insolvent / Red-letter protective zone
            "r": "bottom_tier",   # Lower track tier
            "p": "normal",        # Standard par value track tier
            "e": "end_game"       # Maximum value ceiling triggers
        }
        # Provide strict RGB colors to ensure java.awt.Color definitions do not evaluate to null
        ET.SubElement(root, "StockSpaceType", name="normal", colour="255,255,255")
        ET.SubElement(root, "StockSpaceType", name="bottom_tier", colour="255,235,204") # Light Orange
        ET.SubElement(root, "StockSpaceType", name="end_game", colour="204,255,204")    # Light Green
        ET.SubElement(root, "StockSpaceType", name="liquidation", colour="204,51,51").append(ET.Element("NoHoldLimit"))
        ET.SubElement(root, "StockSpaceType", name="acquisition", colour="153,153,153").append(ET.Element("NoHoldLimit"))


        for idx, val in enumerate(self.market_prices):
            price_match = re.match(r"(\d+)([a-z]?)", val)
            if price_match:
                price_digits = price_match.group(1)
                suffix_letter = price_match.group(2)
                space_type = type_mapping.get(suffix_letter, "normal")
                
                ET.SubElement(root, "StockSpace", 
                              name=f"A{idx + 1}", 
                              price=price_digits, 
                              type=space_type)
        return prettify_xml(root)



    def build_map_xml(self):
        root = ET.Element("Map", tileOrientation="EW", letterOrientation="vertical", even="B")
        
        for hex_name in sorted(list(self.active_coords)):
            hex_info = self.hex_details.get(hex_name, {})
            
            if isinstance(hex_info, str):
                details = hex_info
                color = "white"
            else:
                details = hex_info.get("details", "")
                color = hex_info.get("color", "white")
                
            attrs = {"name": hex_name}
            tile_id = "0"
            
            if color == "red" and "offboard" in details:
                paths = len(re.findall(r"path=", details))
                if paths == 1: tile_id = "-901"
                elif paths == 2: tile_id = "-902"
                elif paths == 3: tile_id = "-903"
                else: tile_id = "-908"
            elif "city" in details:
                if color == "gray":
                    revenue = re.search(r"revenue:(\d+)", details)
                    if revenue and revenue.group(1) == "40": tile_id = "-103"
                    elif revenue and revenue.group(1) == "30": tile_id = "-105"
                    elif revenue and revenue.group(1) == "20": tile_id = "-102"
                    else: tile_id = "-5"
                elif color == "yellow":
                    if "label=OO" in details or "city=revenue:0;city=revenue:0" in details:
                        tile_id = "-20"
                    elif "label=B" in details:
                        tile_id = "-11"
                    elif "label=NY" in details:
                        tile_id = "-21"
                    else:
                        tile_id = "-10"
                else:
                    tile_id = "-10"
            elif "town" in details:
                towns = len(re.findall(r"town", details))
                tile_id = "-2" if towns > 1 else "-1"
            elif color == "gray":
                tile_id = "-7"
                
            attrs["tile"] = tile_id
            
            if "offboard=revenue:" in details:
                rev_match = re.search(r"offboard=revenue:([^;]+)", details)
                if rev_match:
                    vals = [v.split('_')[1] for v in rev_match.group(1).split('|') if '_' in v]
                    if vals: attrs["value"] = ",".join(vals)
            
            if "terrain:mountain" in details:
                attrs.update({"cost": "120", "terrain": "hills"})
            elif "terrain:water" in details:
                attrs.update({"cost": "80", "terrain": "river"})
                
            label_m = re.search(r"label=([A-Z]+)", details)
            if label_m: attrs["label"] = label_m.group(1)

            border_m = re.findall(r"edge:(\d+),type:impassable", details)
            if border_m:
                attrs["impassable"] = self.calculate_neighbor(hex_name, border_m[0])

            if hex_name in self.location_names:
                attrs["city"] = self.location_names[hex_name]
            
            hex_el = ET.SubElement(root, "Hex", **attrs)

        return prettify_xml(root)


    def build_tileset_xml(self):
       # Explicitly hook into the game-specific filtered definition file
        root = ET.Element("TileManager", tiles="Tiles.xml")
        
        # 1. Dynamically discover only preprinted tiles that are actively utilized on the map
        active_map_tiles = {"0"}
        
        for hex_name in self.active_coords:
            hex_info = self.hex_details.get(hex_name, {})
            details = hex_info if isinstance(hex_info, str) else hex_info.get("details", "")
            color = "white" if isinstance(hex_info, str) else hex_info.get("color", "white")
            
            # Replicate the exact map ID derivation rules to find which preprinted IDs exist
            if color == "red" and "offboard" in details:
                paths = len(re.findall(r"path=", details))
                if paths == 1: active_map_tiles.add("-901")
                elif paths == 2: active_map_tiles.add("-902")
                elif paths == 3: active_map_tiles.add("-903")
                else: active_map_tiles.add("-908")
            elif "city" in details:
                if color == "gray":
                    revenue = re.search(r"revenue:(\d+)", details)
                    if revenue and revenue.group(1) == "40": active_map_tiles.add("-103")
                    elif revenue and revenue.group(1) == "30": active_map_tiles.add("-105")
                    elif revenue and revenue.group(1) == "20": active_map_tiles.add("-102")
                    else: active_map_tiles.add("-5")
                elif color == "yellow":
                    if "label=OO" in details or "city=revenue:0;city=revenue:0" in details: active_map_tiles.add("-20")
                    elif "label=B" in details: active_map_tiles.add("-11")
                    elif "label=NY" in details: active_map_tiles.add("-21")
                    else: active_map_tiles.add("-10")
                else:
                    active_map_tiles.add("-10")
            elif "town" in details:
                towns = len(re.findall(r"town", details))
                active_map_tiles.add("-2" if towns > 1 else "-1")
            elif color == "gray":
                active_map_tiles.add("-7")

       # 2. Load the scraped master reference upgrade path dictionary
        master_upgrades = {}
        upgrades_json_path = os.path.join(os.path.dirname(__file__), "master_tile_upgrades.json")
        if os.path.exists(upgrades_json_path):
            try:
                with open(upgrades_json_path, "r", encoding="utf-8") as f:
                    master_upgrades = json.load(f)
            except Exception:
                pass

        # Create a set of all valid tile IDs included in this specific game box for strict filtering
        allowed_box_ids = {t["id"] for t in self.tiles_manifest}

        # 3. Append only the preprinted tiles used by this game map with filtered master upgrades
        for p_id in sorted(list(active_map_tiles), key=int): 
            tile_node = ET.SubElement(root, "Tile", id=p_id)
            if p_id in master_upgrades:
                # Intersect master options with what is physically available inside the manifest box
                valid_routes = [uid.strip() for uid in master_upgrades[p_id].split(",") 
                                if uid.strip() in allowed_box_ids]
                if valid_routes:
                    ET.SubElement(tile_node, "Upgrade", id=",".join(valid_routes))
            
        # 4. Append the exact quantities and intersected upgrade paths for active standard tiles
        for t in self.tiles_manifest: 
            tile_node = ET.SubElement(root, "Tile", id=t["id"], quantity=str(t["quantity"]))
            if t["id"] in master_upgrades:
                valid_routes = [uid.strip() for uid in master_upgrades[t["id"]].split(",") 
                                if uid.strip() in allowed_box_ids]
                if valid_routes:
                    ET.SubElement(tile_node, "Upgrade", id=",".join(valid_routes))



        return prettify_xml(root)
    
    

    def build_tiles_definition_xml(self):
        master_tiles_path = "src/main/resources/tiles/Tiles.xml"
        
        if not os.path.exists(master_tiles_path):
            raise FileNotFoundError(f"CRITICAL ERROR: Baseline dictionary file could not be resolved at '{master_tiles_path}'.")

        tree = ET.parse(master_tiles_path)
        template_root = tree.getroot()

        root = ET.Element("Tiles")

        # Reuse the exact same dynamic master lookup strategy for allowed preprinted elements
        allowed_game_ids = {"0"}
        for tile_node in template_root.findall("Tile"):
            t_id = tile_node.get("id")
            if t_id and (t_id.startswith("-") or t_id == "0"):
                allowed_game_ids.add(t_id)
        
        # Merge with the active game's requested standard manifests
        manifest_ids = {t["id"] for t in self.tiles_manifest}
        allowed_game_ids = allowed_game_ids.union(manifest_ids)

        # Extract and copy ONLY the matching relevant tiles from the master baseline template
        for tile_node in template_root.findall("Tile"):
            t_id = tile_node.get("id")
            if t_id in allowed_game_ids:
                root.append(tile_node)

        existing_ids = {node.get("id") for node in root.findall("Tile") if node.get("id")}

        # Append the unique custom layouts (e.g., 1860's 741-789) if they aren't in the baseline template yet
        for t in self.tiles_manifest:
            if t["id"] in existing_ids:
                continue  
                
            colour = "yellow" if t["color"] == "standard" else t["color"]
            tile_node = ET.SubElement(root, "Tile", id=t["id"], colour=colour, name=f"Tile {t['id']}")
            
            for st in t.get("stations", []):
                attrs = {"id": st["id"], "type": st["type"], "value": st["value"]}
                if st["slots"] != "0": 
                    attrs["slots"] = st["slots"]
                ET.SubElement(tile_node, "Station", **attrs)
                
            for side_a, side_b in t["paths"]:
                ET.SubElement(tile_node, "Track", **{"from": side_a, "to": side_b, "gauge": "normal"})
                
        return prettify_xml(root)




    def build_game_options_xml(self):
        root = ET.Element("GameOptions")
        ET.SubElement(root, "GameOption", name="Variant", values="Standard", default="Standard")
        ET.SubElement(root, "GameOption", name="RouteAlgorithm", values="Permissive", default="Permissive")
        return prettify_xml(root)

    def run(self):
        self.parse_all()
        os.makedirs(self.output_dir, exist_ok=True)
        targets = {
            "Game.xml": self.build_game_xml(),
            "CompanyManager.xml": self.build_company_manager_xml(),
            "StockMarket.xml": self.build_stock_market_xml(),
            "Map.xml": self.build_map_xml(),
            "TileSet.xml": self.build_tileset_xml(),
            "Tiles.xml": self.build_tiles_definition_xml(),
            "GameOptions.xml": self.build_game_options_xml(),
        }
        for filename, payload in targets.items():
            with open(os.path.join(self.output_dir, filename), "w", encoding="utf-8") as f:
                f.write(payload)


    def _populate_map_data(self):
        match = re.search(r"HEXES\s*=\s*\{(.*?)\}\.freeze", self.map_rb, re.DOTALL)
        if match:
            hex_block = match.group(1)
            color_blocks = re.finditer(r"([a-z]+):\s*\{((?:[^{}]|\[[^\]]*\])*)\}", hex_block, re.DOTALL)
            for color_match in color_blocks:
                color = color_match.group(1)
                content = color_match.group(2)
                lines = re.finditer(r"(?:\[(.*?)\]|%w\[(.*?)\])\s*=>\s*'([^']*)'", content)
                for line in lines:
                    coord_str = line.group(1) or line.group(2)
                    coords = [c.strip().replace("'", "").replace('"', "") 
                              for c in re.split(r'[,\s]+', coord_str.strip()) if c.strip()]
                    details = line.group(3)
                    for coord in coords:
                        self.active_coords.add(coord)
                        self.hex_details[coord] = {"color": color, "details": details}


    def calculate_neighbor(self, hex_name, edge):
        # Flat-topped hex offset logic for 1860
        col = ord(hex_name[0]) - ord('A')
        row = int(hex_name[1:])
        offsets = {0: (0, -2), 1: (1, -1), 2: (1, 1), 3: (0, 2), 4: (-1, 1), 5: (-1, -1)}
        d_col, d_row = offsets.get(int(edge), (0, 0))
        new_col = chr(ord('A') + col + d_col)
        new_row = row + d_row
        return f"{new_col}{new_row}"
    



    def _get_tile_upgrades(self, base_tile):
        # Color rank setup
        color_rank = {"standard": 1, "yellow": 1, "green": 2, "brown": 3, "gray": 4, "fixed": 0}
        base_color_str = base_tile.get("color", "standard").lower()
        base_color = color_rank.get(base_color_str, 1)
        
        # Fixed or gray tiles do not upgrade further
        if base_color == 0 or base_color == 4:
            return ""

        # Helper to rotate endpoints dynamically
        def rotate_endpoint(endpoint, offset):
            if endpoint.startswith("side"):
                side_num = int(endpoint.replace("side", ""))
                return f"side{(side_num + offset) % 6}"
            return endpoint # Keep city/town IDs relative

        # Normalize base paths into sets of frozen tuples
        base_paths = {tuple(sorted(p)) for p in base_tile["paths"]}
        base_town_count = sum(1 for s in base_tile.get("stations", []) if s["type"] == "Town")
        base_city_count = sum(1 for s in base_tile.get("stations", []) if s["type"] == "City")
        
        valid_upgrades = []

        for target in self.tiles_manifest:
            target_color_str = target.get("color", "standard").lower()
            target_color = color_rank.get(target_color_str, 1)
            
            # Must progress exactly 1 tier up the color chart
            if target_color != base_color + 1:
                continue

            # Infrastructure Check: Target must sustain or improve stations
            target_town_count = sum(1 for s in target.get("stations", []) if s["type"] == "Town")
            target_city_count = sum(1 for s in target.get("stations", []) if s["type"] == "City")
            if target_town_count < base_town_count or target_city_count < base_city_count:
                continue

            # Geometry Check: Attempt all 6 orientations
            for rotation in range(6):
                rotated_target_paths = set()
                for start, end in target["paths"]:
                    r_start = rotate_endpoint(start, rotation)
                    r_end = rotate_endpoint(end, rotation)
                    rotated_target_paths.add(tuple(sorted((r_start, r_end))))

                # If all base tracks are perfectly preserved in this orientation, it's a valid match!
                if base_paths.issubset(rotated_target_paths):
                    valid_upgrades.append(target["id"])
                    break

        return ",".join(valid_upgrades)


    def extract_tiles_manifest(self):
        self.tiles_manifest = []
        master_lookup = {}
        master_tiles_path = "src/main/resources/tiles/Tiles.xml"
        
        # 1. Load the reference master layouts to enrich simple manifest tiles
        if os.path.exists(master_tiles_path):
            try:
                tree = ET.parse(master_tiles_path)
                for tile_node in tree.getroot().findall("Tile"):
                    t_id = tile_node.get("id")
                    if t_id:
                        color = tile_node.get("colour", "standard")
                        stations = []
                        for st in tile_node.findall("Station"):
                            stations.append({
                                "type": st.get("type", "City"),
                                "slots": st.get("slots", "0"),
                                "value": st.get("value", "0")
                            })
                        paths = []
                        for tr in tile_node.findall("Track"):
                            paths.append((tr.get("from"), tr.get("to")))
                        master_lookup[t_id] = {"color": color, "stations": stations, "paths": paths}
            except Exception:
                pass

        # 2. STRICT EXTRACTION: Isolate the TILES block to prevent parameter bleed
        tiles_block_match = re.search(r"TILES\s*=\s*\{(.*?)\}\.freeze", self.map_rb, re.DOTALL)
        if tiles_block_match:
            simple_tiles = re.findall(r"'(\d+)'\s*=>\s*(\d+)", tiles_block_match.group(1))
            for tile_id, qty in simple_tiles:
                master_data = master_lookup.get(tile_id, {"color": "standard", "paths": [], "stations": []})
                self.tiles_manifest.append({
                    "id": tile_id,
                    "quantity": qty,
                    "color": master_data["color"],
                    "paths": master_data["paths"],
                    "stations": master_data["stations"]
                })
        
        # 3. Advanced parser for custom layout structures (e.g., 1860)
        custom_blocks = re.findall(r"'(\d+)'\s*=>\s*\{\s*'count'\s*=>\s*(\d+),\s*'color'\s*=>\s*'([^']+)'.*?'code'\s*=>\s*'([^']+)'", self.map_rb, re.DOTALL)
        for tile_id, count, color, code in custom_blocks:
            parts = code.split(";")
            stations_list = []
            paths_list = []
            
            for part in parts:
                part = part.strip()
                if part.startswith("city") or part.startswith("town") or part.startswith("halt"):
                    s_type = "City" if part.startswith("city") else "Town"
                    rev_match = re.search(r"revenue:(\d+)", part)
                    val = rev_match.group(1) if rev_match else "0"
                    slots = "1" if s_type == "City" else "0"
                    
                    stations_list.append({
                        "id": f"city{len(stations_list) + 1}",
                        "type": s_type,
                        "slots": slots,
                        "value": val
                    })
            
            paths = re.findall(r"path=a:([\d_\w]+),b:([\d_\w]+)", code)
            for a, b in paths:
                def map_endpoint(point):
                    if point.startswith('_'):
                        idx = int(point.replace('_', ''))
                        return f"city{idx + 1}"
                    elif point.isdigit():
                        return f"side{point}"
                    return "city1"
                paths_list.append((map_endpoint(a), map_endpoint(b)))
                
            self.tiles_manifest.append({
                "id": tile_id,
                "quantity": count,
                "color": color,
                "paths": paths_list,
                "stations": stations_list
            })




if __name__ == "__main__":
    GAME_ID = "1846"
    SRC_DIR = "../18xx/lib/engine/game/g_1830"
    OUTPUT_DIR = "src/main/resources/data/1830"
    RELATIVE_CLASSPATH_PATH = "data/1830"
    converter = RailsXMLConverter(GAME_ID, SRC_DIR, OUTPUT_DIR, RELATIVE_CLASSPATH_PATH)
    converter.run()