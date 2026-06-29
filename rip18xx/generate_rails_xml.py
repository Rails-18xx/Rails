#!/usr/bin/env python3
"""
Filename: /Users/bleeck/Rails/rip18xx/generate_rails_xml.py
Description: Generalized extraction pipeline that translates 18xx.games Ruby configurations
             (game.rb, map.rb, entities.rb) into the structured XML format required by 
             the Java 'Rails' engine. All specific paths are isolated to __main__.
"""

import os
import re
import xml.etree.ElementTree as ET
from xml.dom import minidom


def prettify_xml(element):
    """Generates an indented, scannable string representation of an XML tree."""
    rough_string = ET.tostring(element, "utf-8")
    reparsed = minidom.parseString(rough_string)
    return reparsed.toprettyxml(indent="\t")


class RailsXMLConverter:

    def __init__(self, game_id, source_dir, output_dir, relative_resource_path):
        self.game_id = game_id
        self.source_dir = source_dir
        self.output_dir = output_dir
        self.relative_resource_path = relative_resource_path

        # Raw file string dumps
        self.game_rb = ""
        self.map_rb = ""
        self.entities_rb = ""

        # Extracted configuration structures
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

    def load_source_files(self):
        """Reads target data streams directly into localized properties."""
        with open(os.path.join(self.source_dir, "game.rb"), "r", encoding="utf-8") as f:
            self.game_rb = f.read()
        with open(os.path.join(self.source_dir, "map.rb"), "r", encoding="utf-8") as f:
            self.map_rb = f.read()
        with open(os.path.join(self.source_dir, "entities.rb"), "r", encoding="utf-8") as f:
            self.entities_rb = f.read()

    def parse_all(self):
        """Orchestrates token extraction logic parsing routines."""
        self.load_source_files()

        # 1. Bank Limit Cash Constant
        bank_match = re.search(r"BANK_CASH\s*=\s*([\d_]+)", self.game_rb)
        if bank_match:
            self.bank_cash = int(bank_match.group(1).replace("_", ""))

        # 2. Extract Structural Configurations
        self.extract_cert_limits()
        self.extract_starting_cash()
        self.extract_market()
        self.extract_trains()
        self.extract_phases()
        self.extract_entities()
        self.extract_location_names()
        self.extract_tiles_manifest()

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
        """Extracts flat zigzag linear market coordinate values."""
        match = re.search(r"MARKET\s*=\s*\[\s*%w\[([^\]]+)\]", self.game_rb)
        if match:
            self.market_prices = match.group(1).split()

    def extract_trains(self):
        """Processes structured train array dictionaries via regex tracking blocks."""
        train_blocks = re.findall(r"\{\s*name:\s*'([^']+)'.*?price:\s*(\d+).*?num:\s*([^,\s\}]+)", self.game_rb, re.DOTALL)
        for m in train_blocks:
            self.trains.append({"name": m[0], "cost": m[1], "quantity": m[2].replace("'", "")})

    def extract_phases(self):
        """Builds phase tracking sequences matching operating round targets."""
        phase_blocks = re.findall(r"name:\s*'([^']+)'.*?operating_rounds:\s*(\d+)", self.game_rb, re.DOTALL)
        for m in phase_blocks:
            self.phases.append({"name": m[0], "rounds": m[1]})

    def extract_entities(self):
        """Parses standalone array blocks from entities.rb for Companies and Corporations."""
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

    def extract_tiles_manifest(self):
        """Extracts the entire dynamic quantity manifest and custom layouts out of TILES hash."""
        # Match standard simple numeric keys like '5' => 2
        simple_tiles = re.findall(r"'(\d+)'\s*=>\s*(\d+)", self.game_rb + self.map_rb)
        for tile_id, qty in simple_tiles:
            self.tiles_manifest.append({"id": tile_id, "quantity": qty, "color": "standard", "paths": []})
            
        # Match deep structural custom dictionary assignments (e.g. 741-789 custom track blocks)
        custom_blocks = re.findall(r"'(\d+)'\s*=>\s*\{\s*'count'\s*=>\s*(\d+),\s*'color'\s*=>\s*'([^']+)'.*?'code'\s*=>\s*'([^']+)'", self.map_rb, re.DOTALL)
        for tile_id, count, color, code in custom_blocks:
            # Extract individual exits/entrances from the route code line string (e.g., path=a:0,b:2)
            paths = re.findall(r"path=a:([\d_\w]+),b:([\d_\w]+)", code)
            self.tiles_manifest.append({"id": tile_id, "quantity": count, "color": color, "paths": paths})

    # --- XML GENERATORS ---
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
        for players_count, cert in self.cert_limits.items():
            cash = self.starting_cash.get(players_count, 500)
            ET.SubElement(pm, "Players", number=str(players_count), cash=str(cash), certLimit=str(cert))

        bank = ET.SubElement(root, "Component", name="Bank")
        bank.set("class", "net.sf.rails.game.financial.Bank")
        ET.SubElement(bank, "Bank", amount=str(self.bank_cash))

        ET.SubElement(root, "Component", name="TileManager", file=f"{self.relative_resource_path}/TileSet.xml").set("class", "net.sf.rails.game.TileManager")
        ET.SubElement(root, "Component", name="Map", file=f"{self.relative_resource_path}/Map.xml").set("class", "net.sf.rails.game.MapManager")
        ET.SubElement(root, "Component", name="CompanyManager", file=f"{self.relative_resource_path}/CompanyManager.xml").set("class", "net.sf.rails.game.CompanyManager")
        ET.SubElement(root, "Component", name="StockMarket", file=f"{self.relative_resource_path}/StockMarket.xml").set("class", "net.sf.rails.game.financial.StockMarket")

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
            ET.SubElement(ph, "OperatingRounds", number=p["rounds"])

        return prettify_xml(root)

    def build_company_manager_xml(self):
        root = ET.Element("CompanyManager")
        t1 = ET.SubElement(root, "CompanyType", name="Private")
        t1.set("class", "net.sf.rails.game.PrivateCompany")
        
        major_type = ET.SubElement(root, "CompanyType", name="Major")
        major_type.set("class", "net.sf.rails.game.PublicCompany")
        ET.SubElement(major_type, "Shares", unit="10")
        ET.SubElement(major_type, "StockPrice", par="yes")

        for c in self.companies:
            ET.SubElement(root, "Company", name=c["name"], longname=c["longname"], type="Private", basePrice=c["basePrice"], revenue=c["revenue"])

        for corp in self.corporations:
            co = ET.SubElement(root, "Company", name=corp["sym"], longname=corp["name"], type="Major", tokens=str(corp["tokens"]))
            ET.SubElement(co, "Home", hex=corp["home"])

        return prettify_xml(root)

    def build_stock_market_xml(self):
        root = ET.Element("StockMarket")
        for idx, val in enumerate(self.market_prices):
            clean_price = re.sub(r"[a-z]", "", val)
            if clean_price.isdigit():
                ET.SubElement(root, "StockSpace", name=f"M{idx}", price=clean_price)
        return prettify_xml(root)

    def build_map_xml(self):
        root = ET.Element("Map", tileOrientation="EW", letterOrientation="vertical", even="B")
        white_blocks = re.findall(r"\[([^\]]+)\]\s*=>\s*'([^']*)'", self.map_rb)
        for block in white_blocks:
            hex_names = [h.replace("'", "").strip() for h in block[0].split(",")]
            details = block[1]
            
            for name in hex_names:
                if not re.match(r"^[A-Z]\d+$", name):
                    continue
                loc_name = self.location_names.get(name, "")
                attrs = {"name": name, "tile": "0"}
                if "city" in details:
                    attrs["city"] = loc_name
                elif "town" in details:
                    attrs["town"] = loc_name
                    
                if "terrain:mountain" in details:
                    attrs["cost"] = "60"
                    attrs["terrain"] = "Mountain"
                elif "terrain:water" in details:
                    attrs["cost"] = "60"
                    attrs["terrain"] = "River"
                    
                ET.SubElement(root, "Hex", **attrs)
        return prettify_xml(root)

    def build_tileset_xml(self):
        """Generates functional, structured dynamic TileSet map linking pool counts."""
        root = ET.Element("TileManager", tiles="Tiles.xml")
        
        preprinted = ["0", "-1", "-2", "-3", "-7", "-8", "-10", "-39", "-41", "-58", "-114", "-143", "-800", "-801", "-802", "-803", "-804", "-805", "-806", "-807", "-808", "-809", "-810", "-901"]
        for p_id in preprinted:
            ET.SubElement(root, "Tile", id=p_id)

        for t in self.tiles_manifest:
            ET.SubElement(root, "Tile", id=t["id"], quantity=str(t["quantity"]))
            
        return prettify_xml(root)

    def build_tiles_definition_xml(self):
        """Generates game-specific topology track data vectors inside Tiles.xml."""
        root = ET.Element("Tiles")
        
        # 1. Base Preprinted Map Geometries
        preprinted = ["0", "-1", "-2", "-3", "-7", "-8", "-10", "-39", "-41", "-58", "-114", "-143", "-800", "-801", "-802", "-803", "-804", "-805", "-806", "-807", "-808", "-809", "-810", "-901"]
        for p_id in preprinted:
            ET.SubElement(root, "Tile", id=p_id, colour="fixed", name=f"Preprinted {p_id}")

        # 2. Dynamic extracted operational inventory routes
        for t in self.tiles_manifest:
            colour = "yellow" if t["color"] == "standard" else t["color"]
            tile_node = ET.SubElement(root, "Tile", id=t["id"], colour=colour, name=f"Tile {t['id']}")
            
            # Map structural edge-to-edge track segments cleanly out of Ruby code fields
            for side_a, side_b in t["paths"]:
                # Cleans track labels (like discarding center token labels '_0') to integers
                a_idx = side_a.replace("_", "")
                b_idx = side_b.replace("_", "")
                if a_idx.isdigit() and b_idx.isdigit():
                    ET.SubElement(tile_node, "Track", start=a_idx, end=b_idx)
                    
        return prettify_xml(root)

    def run(self):
        """Compiles files and dynamically ensures destination directories are initialized."""
        self.parse_all()
        os.makedirs(self.output_dir, exist_ok=True)

        targets = {
            "Game.xml": self.build_game_xml(),
            "CompanyManager.xml": self.build_company_manager_xml(),
            "StockMarket.xml": self.build_stock_market_xml(),
            "Map.xml": self.build_map_xml(),
            "TileSet.xml": self.build_tileset_xml(),
            "Tiles.xml": self.build_tiles_definition_xml(),
        }

        for filename, payload in targets.items():
            out_file = os.path.join(self.output_dir, filename)
            with open(out_file, "w", encoding="utf-8") as f:
                f.write(payload)
            print(f"[SUCCESS] Saved generated structure: {out_file}")


# =========================================================================
#   MAIN RUNTIME ENTRY POINT: CHANGE CONSTANTS HERE TO ADAPT NEW GAMES
# =========================================================================
if __name__ == "__main__":
    
    GAME_ID = "1860"
    SRC_DIR = "../18xx/lib/engine/game/g_1860"
    OUTPUT_DIR = "src/main/resources/data/1860"
    RELATIVE_CLASSPATH_PATH = "data/1860"

    converter = RailsXMLConverter(
        game_id=GAME_ID, 
        source_dir=SRC_DIR, 
        output_dir=OUTPUT_DIR, 
        relative_resource_path=RELATIVE_CLASSPATH_PATH
    )
    converter.run()