import json
import os
import re

# The definitive Map of IDs to Keys and Questions
RULE_MANIFEST = {
    "1.1": {"key": "startingCash", "question": "Starting cash"},
    "1.2": {"key": "privatePriceDrop", "question": "Does the price of a private company drop by 5 for no sale in the first round?"},
    "1.4": {"key": "advanceBids", "question": "Can you make advance bids?"},
    "2.1": {"key": "buySellOrder", "question": "Is there a specific order to buying and selling on your turn?"},
    "2.3": {"key": "firstSellRound", "question": "When can you first sell shares in a company?"},
    "2.4": {"key": "poolLimit", "question": "Does the bank pool have a per-company share limit?"},
    "2.5": {"key": "certLimit", "question": "Player certificate limits"},
    "2.9": {"key": "sameCompanyBuySell", "question": "Can you buy a certificate and immediately sell a certificate in the same company?"},
    "2.10": {"key": "companyBuyShares", "question": "Can companies buy shares?"},
    "2.11": {"key": "roundEnd", "question": "What ends a share dealing round?"},
    "3.4": {"key": "parPrice", "question": "How is a share company's initial (par) price determined?"},
    "5.1": {"key": "initialTile", "question": "Where can you make an initial tile lay?"},
    "5.3": {"key": "tileReplacement", "question": "Must a tile replacement extend existing track?"},
    "6.1": {"key": "stationCost", "question": "Cost of station markers"},
    "6.2": {"key": "multiStation", "question": "Can you lay more than one station marker per turn?"},
    "6.3": {"key": "stationLocation", "question": "Where can you lay a station marker?"},
    "6.4": {"key": "firstStationTiming", "question": "When is a company's first station marker laid?"},
    "7.4": {"key": "twoStationsOneTile", "question": "Can one train run to two stations on the same tile?"},
    "7.7": {"key": "maxRevenue", "question": "Must the maximum possible revenue be claimed?"},
    "9.4": {"key": "forcedTrainAfford", "question": "When a company is forced to buy a train and cannot buy one with its own means, what train may it then buy?"},
    "9.5": {"key": "forcedTrainMinor", "question": "Must a minor company buy a train if it does not have one?"},
    "10.2": {"key": "privateTradeCompany", "question": "Are private companies purchasable by share companies?"},
    "10.4": {"key": "privateClosing", "question": "Does using a private company's special property close it?"},
    "12": {"key": "gamePhases", "question": "Game Phases"},
    "16": {"key": "miscellaneous", "question": "Miscellaneous"}
}

def parse_phases(lines):
    phases = []
    # Lines after the header
    for line in lines:
        parts = [p.strip() for p in line.split('|')]
        if len(parts) >= 3:
            phases.append({"train": parts[0], "phase": parts[1], "effects": parts[2]})
    return phases

def run_translation(game_name):
    base_dir = os.path.join("gamespecifics", game_name)
    input_path = os.path.join(base_dir, "raw_rules_summary.txt")
    output_path = os.path.join(base_dir, "rules.json")

    with open(input_path, 'r', encoding='utf-8') as f:
        content = f.read()

    blocks = content.split('--------------------------------------------------')
    json_data = {"game": game_name, "rules": {}}

    for block in blocks:
        lines = [l.strip() for l in block.split('\n') if l.strip()]
        if not lines: continue
        
        # Extract the ID from the first line (e.g., "1.2 - ...")
        first_line = lines[0]
        id_match = re.match(r'^([\d\.]+)', first_line)
        if not id_match: continue
        
        rule_id = id_match.group(1)
        if rule_id not in RULE_MANIFEST: continue
        
        manifest = RULE_MANIFEST[rule_id]
        answer_body = " ".join(lines[1:])
        
        # If it's the Phase table, parse specifically
        if rule_id == "12":
            json_data["rules"][manifest["key"]] = {
                "question": manifest["question"],
                "answer": parse_phases(lines[1:]),
                "fulltext": answer_body
            }
        else:
            json_data["rules"][manifest["key"]] = {
                "question": manifest["question"],
                "answer": answer_body,
                "fulltext": answer_body
            }

    with open(output_path, 'w', encoding='utf-8') as jf:
        json.dump(json_data, jf, indent=2)
    print(f"JSON compiled successfully: {output_path}")

if __name__ == "__main__":
    run_translation("1835")