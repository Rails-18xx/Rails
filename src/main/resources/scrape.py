#!/usr/bin/env python3
"""
18xx Rules Difference List - Final Token-Isolated 1835 Scraper
Target Root: ./src/main/resources/gamespecifics/
"""

import os
import sys
import urllib.request
import urllib.error
import re
from bs4 import BeautifulSoup

# 1. IMMUTABLE REGISTRY OF ALL SECTIONS
RULES_SECTIONS = {
    "1_1": "1.1 - Starting cash",
    "1_2": "1.2 - Does the price of a private company drop by 5 for no sale in the first round?",
    "1_3": "1.3 - Can you sell comany shares in the first round?",
    "1_4": "1.4 - Can you make advance bids?",
    "2_1": "2.1 - Is there a specific order to buying and selling on your turn?",
    "2_2": "2.2 - Ae you limited to buying one certificate on your turn?",
    "2_3": "2.3 - When can you first sell shares in a company?",
    "2_4": "2.4 - Does the bank pool have a per-company share limit?",
    "2_5": "2.5 - Player certificate limits",
    "2_6": "2.6 - Player certificates limits for shares in one company",
    "2_7": "2.7 - Does the stock price drop when stock is sold?",
    "2_8": "2.8 - Does the stock price go up at the end of the share dealing round for a fully-held corporation?",
    "2_9": "2.9 - Can you buy a certificate and immediately sell a certificate in the same company?",
    "2_10": "2.10 - Can companies buy shares?",
    "2_11": "2.11 - What ends a share dealing round?",
    "2_12": "2.12 - Can a player sell private companies to another player?",
    "3_1": "3.1 -  Do you lay the base station token immediately upon floating?",
    "3_2": "3.2 -  How many shares must be sold for a company to float?",
    "3_3": "3.3 -  Does a company get full capitalisation upon floating?",
    "3_4": "3.4 - How is a share company's initial (par) price determined?",
    "4_1": "4.1 - In what order to companies operate?",
    "4_2": "4.2 - If you sell shares so that their tokens end up in one stack, what order are they stacked in?",
    "5_1": "5.1 - Where can you make an initial tile lay?",
    "5_2": "5.2 - Can you lay two tiles in a turn?",
    "5_3": "5.3 - Must a tile replacement extend existing track?",
    "5_4": "5.4 - Do villages upgrade?",
    "6_1": "6.1 - Cost of station markers",
    "6_2": "6.2 -Can you lay more than one station marker per turn?",
    "6_3": "6.3 - Where can you lay a station marker?",
    "6_4": "6.4 - When is a company's first station marker laid?",
    "7_1": "7.1 - Can you run into a city completely filled by rival station markers?",
    "7_2": "7.2 - Can you do a run that passes through a city completely filled by rival station markers?",
    "7_3": "7.3 - Unusual rules about running",
    "7_4": "7.4 - Can one train run to two stations on the same tile?",
    "7_5": "7.5 - Is double-heading allowed?",
    "7_6": "7.6 - Rules about villages",
    "7_7": "7.7 - Must the maximum possible revenue be claimed?",
    "8_1": "8.1 - Does stock move right for payment of dividends?",
    "8_2": "8.2 - What dividend payments go into the company's treasury?",
    "8_3": "8.3 - Does stock move left for withheld earnings?",
    "8_4": "8.4 - Can a company make a partial payout?",
    "9_1": "9.1 - Can companies buy trains from one another?",
    "9_2": "9.2 - Must a major share company buy a train if it does not have one?",
    "9_3": "9.3 - Can trains be sold back to the bank?",
    "9_4": "9.4 - When a company is forced to buy a train and cannot buy one with its own means, what train may it then buy?",
    "9_5": "9.5 - Must a minor company buy a train if it does not have one?",
    "9_6": "9.6 - PCan trains of the final type be purchased as soon as one train of the next-to-last type is purchased?",
    "9_7": "9.7 - Can a company buy more than one train from the bank per OR?",
    "10_1": "10.1 - Are private companies purchasable between players?",
    "10_2": "10.2 - Are private companies purchasable by share companies?",
    "10_3": "10.3 - Does a private company prevent builds in its home hex(es) while it is owned by a player?",
    "10_4": "10.4 -  Does using a private company's special property close it?",
    "10_5": "10.5 - When do private companies close?",
    "10_6": "10.6 - Can you buy and sell private companies in other ways?",
    "11_1": "11.1 - Can the Director's certificate ever be in the bank pool?",
    "11_2": "11.2 - Can you exchange the Director's certificate for regular shares from another player when you sell shares to the bank pool?",
    "11_3": "11.3 - After a sale of shares forces a change in Director, who gets it in case of a tie?",
    "12": "12 - Game Phases",
    "13": "13 - End of Game",
    "14": "14 - Secrecy",
    "15": "15 - Inventories",
    "16": "16 - Miscellaneous"
}

# Added user additions and variations to multi-word targets
MULTI_WORD_GAMES = [
    "1835 Rheinland", "1838 Rheinland", "1822 CA", "1822CA", "1822 MRS", "1822 NRS", "1822 MX", "1822 PNW",
    "1817 Volatility", "1817 NA", "1817 World", "1824 v2", "1826 v2", "1829 North", "1829 South", 
    "1829 Mainline", "1830 v2", "1830 v3", "1830 Cardgame", "1831 v2", "1835 v2", "1837 Saxonia", "1840 Vienna Tramways", 
    "1841 v2", "1844/1854", "1846 v2", "1847 Anniversary Edition", "1847AE", "1848 v2", "1849 v1", 
    "1849 v2", "1849 v3", "1853 v2", "1854 v2", "1860 v1", "1860 v2", "1860 v3", "1861 v2", "1861/1867", 
    "1862 Railways of the Eastern Counties", "1862 USA/Canada", "1862EA v2", "1862EA", "1869 The Golden Spike", 
    "1869 USA West", "1873 Marflow", "1876 v2", "1876-30 v2", "1876-30", "1876-35 v2", "1876-35", "1879 Winsome", "1880 China", 
    "1883 2nd Edition", "1883 Express d'Orient", "1888 North China", "1893 Cologne", "1899 v1", "1899 v2", 
    "18Chesapeake Off the Rails", "18EZ Levels 2 and 3", "18EZ Level 1", "18GA v2", "18Ireland v2", 
    "18NewEngland 2 Companies Pack", "18NY v2", "18Rhl Rhineland", "18Ruhr Extension 01", 
    "18Ruhr Extension 02", "18West v3", "Steam Over Holland", "Railroad Barons", "Rolling Stock", 
    "Rolling Stock Stars", "Ur 1830 BC", "1847 Expert Game", "1835 Level 3", "1835 Expert Game", "1827 Jr.", "1827Jr", "18Africa","18EZ Level 2", "18SY-O"
]

SINGLE_WORD_GAMES = [
    "1761", "1800", "1812", "1817", "1822", "1824", "1825", "1826", "1828", "1829", "1830", "1830BC",
    "1830Lummerland", "1830NL", "1831", "1832", "1833NE", "1834", "1835", "1836", "1837", "1837SX", 
    "1839", "1841", "1842", "1844", "1846", "1847", "1848", "1849", "1850", "1851", "1851Moon", 
    "1853", "1854", "1856", "1857", "1858", "1859", "1860", "1861", "1862", "1867", "1868", "1870", 
    "1873", "1876", "1878", "1879", "1880", "1881", "1883", "1886", "1888", "1889", "1890", "1891", 
    "1893", "1894", "1895", "1898", "1899", "2038", "18Africa", "18AL", "18Ardennes", "18ARG", "18BE", "18BL", 
    "18C2C", "18CH", "18Chesapeake", "18CLE", "18Cuba", "18CZ", "18DDR", "18Dixie", "18DO-Dortmund", 
    "18EC", "18ESP", "18EU", "18EUS", "18EZ", "18FL", "18GA", "18GB", "18GL", "18GM", "18HeXX", 
    "18Hiawatha", "18IN", "18India", "18Ireland", "18JP-T", "18Kaas", "18Kids", "18Lilliput", 
    "18Magyarorszag", "18MEX", "18Milwaukee", "18MS", "18MW", "18NE", "18Neb", "18NewEngland", 
    "18NK", "18NL", "18NW", "18NY", "18NYC", "18OE", "18OL", "18PA", "18RoyalGorge", "18Ruhr", 
    "18SA", "18Scan", "18SJ", "18SS", "18Svea", "18SY", "18TN", "18Tokaido", "18TraXX", "18US", 
    "18USA", "18VA", "Basic Game", "18West", "18Wisconsin", "18Zoo", "18??", "Poseidon", "Crisis", "Rest", "All others",
    "Poseidor"
]

def generate_fwtwr_url(section_key):
    base_url = "http://www.fwtwr.com/18xx/rules_difference_list/"
    return f"{base_url}{section_key.replace('.', '_').strip()}.htm"

def clean_game_variants(text, target_game):
    """Normalizes 1835 sub-variants (Basic Game, retuned versions) directly to the target token."""
    if target_game == "1835":
        text = re.sub(r'\b1835\s+\(v\d+\.\d+\s+retuned\)', '1835', text, flags=re.IGNORECASE)
        text = re.sub(r'\b1835\s+Basic\s+Game\b', '1835', text, flags=re.IGNORECASE)
    return text

def strip_remaining_exclusion_games(text):
    """Iteratively identifies and strips any alternative games left over inside the prose string."""
    working = text.strip()
    all_games = sorted(MULTI_WORD_GAMES + SINGLE_WORD_GAMES, key=len, reverse=True)
    
    while True:
        matched = False
        for g in all_games:
            if working.lower().startswith(g.lower()):
                working = working[len(g):].lstrip()
                working = re.sub(r'^(?:,\s*|\s+and\s+|\s+|-|:|;)+', '', working, flags=re.IGNORECASE).lstrip()
                matched = True
                break
        if not matched:
            break
    return working

def extract_section_12(target_game, soup):
    results = []
    rows = soup.find_all('tr')
    capture = False
    game_header_pattern = re.compile(r'^(17\d{2}|18\d{2}|20\d{2}|Poseidon|Crisis|Steam|Railroad|Rolling)', re.IGNORECASE)
    
    for row in rows:
        cells = [c.get_text(" ", strip=True) for c in row.find_all(['td', 'th'])]
        if not cells:
            continue
        first_cell = clean_game_variants(cells[0], target_game)
        if first_cell == target_game:
            capture = True
            # For section 12 (table format), omit the leading target game word
            results.append(" | ".join(cells[1:]))
            continue
        if capture:
            if game_header_pattern.match(first_cell) and first_cell != target_game:
                if not (first_cell.isdigit() and len(first_cell) < 4):
                    break
            results.append(" | ".join(cells))
    return results

def identify_leading_games(line):
    working_line = re.sub(r'\s+', ' ', line.replace('\xa0', ' ')).strip()
    detected_games = []
    
    all_multi = sorted(MULTI_WORD_GAMES + ["1835 (v2.3 retuned)", "1835 Basic Game"], key=len, reverse=True)
    all_single = sorted(SINGLE_WORD_GAMES, key=len, reverse=True)
    
    while working_line:
        matched = False
        for g in all_multi:
            if working_line.lower().startswith(g.lower()):
                rem = working_line[len(g):]
                if not rem or rem[0] in [',', ' ', '\t', '-', ':', '.', ';']:
                    norm_g = "1835" if "1835" in g else g
                    detected_games.append(norm_g)
                    working_line = rem.lstrip()
                    matched = True
                    break
        if matched:
            working_line = re.sub(r'^(?:,\s*|\s+and\s+|\s+|-|:|;)+', '', working_line, flags=re.IGNORECASE).lstrip()
            continue
            
        for g in all_single:
            if working_line.lower().startswith(g.lower()):
                rem = working_line[len(g):]
                if not rem or rem[0] in [',', ' ', '\t', '-', ':', '.', ';']:
                    detected_games.append(g)
                    working_line = rem.lstrip()
                    matched = True
                    break
        if matched:
            working_line = re.sub(r'^(?:,\s*|\s+and\s+|\s+|-|:|;)+', '', working_line, flags=re.IGNORECASE).lstrip()
            continue
        break
        
    return detected_games, working_line.strip()

def clean_final_prose(text):
    text = re.sub(r'^[.,\s\-\u00a0;:]+', '', text).strip()
    return text

def extract_game_from_html(key, target_game, raw_html_content):
    if isinstance(raw_html_content, bytes):
        html_text = raw_html_content.decode('windows-1252', errors='replace') 
    else:
        html_text = raw_html_content
        
    html_text = re.sub(r'(?i)<br\s*?/?>', ' ', html_text)
    html_text = clean_game_variants(html_text, target_game)
    soup = BeautifulSoup(html_text, 'html.parser')
    
    for text_node in soup.find_all(string=True):
        if '\n' in text_node or '\r' in text_node:
            text_node.replace_with(text_node.replace('\n', ' ').replace('\r', ''))
    
    if key == "12":
        return extract_section_12(target_game, soup)
        
    results = []
    
    for row in soup.find_all('tr'):
        cells = row.find_all(['td', 'th'])
        if not cells: 
            continue
        first_cell_text = clean_game_variants(cells[0].get_text(" ", strip=True), target_game)
        if target_game == first_cell_text:
            cleaned_cells = [c.get_text(" ", strip=True) for c in cells[1:]]
            cleaned_line = " | ".join(cleaned_cells)
            cleaned_line = strip_remaining_exclusion_games(cleaned_line)
            if cleaned_line:
                results.append(cleaned_line)
                
    if not results:
        for table in soup.find_all('table'):
            table.decompose()
            
        raw_lines = soup.get_text('\n').split('\n')
        is_capturing = False
        captured_lines = []
        
        for line in raw_lines:
            cleaned_line = line.strip()
            if not cleaned_line:
                continue
                
            detected_games, leftover = identify_leading_games(cleaned_line)
            contains_target = target_game in detected_games
            has_prefixes = len(detected_games) > 0
            
            if has_prefixes:
                if is_capturing and captured_lines:
                    full_prose = clean_final_prose(" ".join(captured_lines))
                    full_prose = strip_remaining_exclusion_games(full_prose)
                    if full_prose:
                        results.append(full_prose)
                    captured_lines = []
                
                if contains_target:
                    is_capturing = True
                    if leftover:
                        captured_lines.append(leftover)
                else:
                    is_capturing = False
            else:
                if is_capturing:
                    captured_lines.append(cleaned_line)
                    
        if is_capturing and captured_lines:
            full_prose = clean_final_prose(" ".join(captured_lines))
            full_prose = strip_remaining_exclusion_games(full_prose)
            if full_prose:
                results.append(full_prose)
                        
    return results

def run_extractor(target_game):
    target_dir = os.path.join(".", "src", "main", "resources", "gamespecifics", target_game.replace(" ", "_"))
    os.makedirs(target_dir, exist_ok=True)
    output_filepath = os.path.join(target_dir, "raw_rules_summary.txt")
    
    print(f"Target Directory Synchronized: {target_dir}")
    headers = { 'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)' }
    
    with open(output_filepath, "w", encoding="utf-8") as out_file:
        out_file.write(f"=== RAW RULES EXTRACT FOR {target_game} ===\n\n")
        
        for key, title in RULES_SECTIONS.items():
            url = generate_fwtwr_url(key)
            try:
                req = urllib.request.Request(url, headers=headers)
                with urllib.request.urlopen(req) as response:
                    html_content = response.read()
                    
                hits = extract_game_from_html(key, target_game, html_content)
                if hits:
                    # New clean format output style mapping header to URL directly
                    out_file.write(f"{title} ({url})\n")
                    for hit in hits:
                        out_file.write(f"{hit}\n")
                    out_file.write("-" * 50 + "\n")
            except urllib.error.HTTPError:
                pass
            except Exception as e:
                print(f"Error parsing section {title}: {str(e)}")
                
    print(f"Success! Clean output written to: {output_filepath}")

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "1835"
    run_extractor(target)