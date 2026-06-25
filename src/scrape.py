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
    "1_2": "1.2 - Private company price drop",
    "1_3": "1.3 - Selling shares in first round",
    "1_4": "1.4 - Advance bids",
    "2_1": "2.1 - Order of buying/selling",
    "2_2": "2.2 - Buying one certificate limit",
    "2_3": "2.3 - When can you first sell shares?",
    "2_4": "2.4 - Bank pool per-company limit",
    "2_5": "2.5 - Player certificate limits",
    "2_6": "2.6 - Player certificate limits (single company)",
    "2_7": "2.7 - Stock price drop on sale",
    "2_8": "2.8 - Stock price increase (fully-held)",
    "2_9": "2.9 - Buy/Sell same company",
    "2_10": "2.10 - Companies buying shares",
    "2_11": "2.11 - Ending share dealing round",
    "2_12": "2.12 - Player to player private sales",
    "3_1": "3.1 - Base station token on float",
    "3_2": "3.2 - Shares sold to float",
    "3_3": "3.3 - Full capitalisation on float",
    "3_4": "3.4 - Initial par price determination",
    "4_1": "4.1 - Order of operations",
    "4_2": "4.2 - Token stack order",
    "5_1": "5.1 - Initial tile lay location",
    "5_2": "5.2 - Two tiles in a turn",
    "5_3": "5.3 - Tile replacement extension",
    "5_4": "5.4 - Village upgrades",
    "6_1": "6.1 - Station marker cost",
    "6_2": "6.2 - Multiple station markers per turn",
    "6_3": "6.3 - Station marker location",
    "6_4": "6.4 - First station marker timing",
    "7_1": "7.1 - Running into filled city",
    "7_2": "7.2 - Passing through filled city",
    "7_3": "7.3 - Unusual running rules",
    "7_4": "7.4 - Running to two stations on one tile",
    "7_5": "7.5 - Double-heading",
    "7_6": "7.6 - Village rules",
    "7_7": "7.7 - Max revenue claim",
    "8_1": "8.1 - Stock move right for dividend",
    "8_2": "8.2 - Dividends to treasury",
    "8_3": "8.3 - Stock move left for withhold",
    "8_4": "8.4 - Partial payout",
    "9_1": "9.1 - Buying trains between companies",
    "9_2": "9.2 - Forced train purchase (major)",
    "9_3": "9.3 - Selling trains to bank",
    "9_4": "9.4 - Forced train purchase (cannot afford)",
    "9_5": "9.5 - Forced train purchase (minor)",
    "9_6": "9.6 - Purchase of final train type",
    "9_7": "9.7 - Multiple train purchases",
    "10_1": "10.1 - Private company purchase between players",
    "10_2": "10.2 - Private purchase by share companies",
    "10_3": "10.3 - Private blocking home hex builds",
    "10_4": "10.4 - Private company closing on use",
    "10_5": "10.5 - Private company closing timing",
    "10_6": "10.6 - Other private company transfers",
    "11_1": "11.1 - Director's cert in bank pool",
    "11_2": "11.2 - Director's cert exchange",
    "11_3": "11.3 - Director change tie-break",
    "12": "12 - Game Phases",
    "13": "13 - End of Game",
    "14": "14 - Secrecy",
    "15": "15 - Inventories",
    "16": "16 - Miscellaneous"
}

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
    "Rolling Stock Stars", "Ur 1830 BC", "1847 Expert Game", "1835 Level 3", "1835 Expert Game", "1827 Jr.", "1827Jr", "18Africa"
]

SINGLE_WORD_GAMES = [
    "1761", "1800", "1812", "1817", "1822", "1824", "1825", "1826", "1828", "1829", "1830", "1830BC",
    "1830Lummerland", "1830NL", "1831", "1832", "1833NE", "1834", "1835", "1836", "1837", "1837SX", 
    "1839", "1841", "1842", "1844", "1846", "1847", "1848", "1849", "1850", "1851", "1851Moon", 
    "1853", "1854", "1856", "1857", "1858", "1859", "1860", "1861", "1862", "1867", "1868", "1870", 
    "1873", "1876", "1878", "1879", "1880", "1881", "1883", "1886", "1888", "1889", "1890", "1891", 
    "1893", "1894", "1895", "1898", "1899", "18Africa", "18AL", "18Ardennes", "18ARG", "18BE", "18BL", 
    "18C2C", "18CH", "18Chesapeake", "18CLE", "18Cuba", "18CZ", "18DDR", "18Dixie", "18DO-Dortmund", 
    "18EC", "18ESP", "18EU", "18EUS", "18EZ", "18FL", "18GA", "18GB", "18GL", "18GM", "18HeXX", 
    "18Hiawatha", "18IN", "18India", "18Ireland", "18JP-T", "18Kaas", "18Kids", "18Lilliput", 
    "18Magyarorszag", "18MEX", "18Milwaukee", "18MS", "18MW", "18NE", "18Neb", "18NewEngland", 
    "18NK", "18NL", "18NW", "18NY", "18NYC", "18OE", "18OL", "18PA", "18RoyalGorge", "18Ruhr", 
    "18SA", "18Scan", "18SJ", "18SS", "18Svea", "18SY", "18TN", "18Tokaido", "18TraXX", "18US", 
    "18USA", "18VA", "18West", "18Wisconsin", "18Zoo", "18??", "Poseidon", "Crisis", "Rest", "All others"
]

def generate_fwtwr_url(section_key):
    base_url = "http://www.fwtwr.com/18xx/rules_difference_list/"
    return f"{base_url}{section_key.replace('.', '_').strip()}.htm"

def extract_section_12(target_game, soup):
    results = []
    rows = soup.find_all('tr')
    capture = False
    game_header_pattern = re.compile(r'^(17\d{2}|18\d{2}|20\d{2}|Poseidon|Crisis|Steam|Railroad|Rolling)', re.IGNORECASE)
    
    for row in rows:
        cells = [c.get_text(" ", strip=True) for c in row.find_all(['td', 'th'])]
        if not cells:
            continue
        first_cell = cells[0]
        if first_cell == target_game:
            capture = True
            results.append(" | ".join(cells))
            continue
        if capture:
            if game_header_pattern.match(first_cell) and first_cell != target_game:
                if not (first_cell.isdigit() and len(first_cell) < 4):
                    break
            results.append(" | ".join(cells))
    return results

def identify_leading_games(line):
    working_line = line.strip()
    detected_games = []
    
    all_multi = sorted(MULTI_WORD_GAMES, key=len, reverse=True)
    all_single = sorted(SINGLE_WORD_GAMES, key=len, reverse=True)
    
    while working_line:
        matched = False
        for g in all_multi:
            if working_line.lower().startswith(g.lower()):
                rem = working_line[len(g):]
                if not rem or rem[0] in [',', ' ', '\t', '\xa0', '-', ':', '.', ';']:
                    detected_games.append(g)
                    working_line = rem.lstrip()
                    matched = True
                    break
        if matched:
            working_line = re.sub(r'^(?:,\s*|\s+and\s+|\s+|-|:|;)+', '', working_line, flags=re.IGNORECASE).lstrip()
            continue
            
        for g in all_single:
            if working_line.lower().startswith(g.lower()):
                rem = working_line[len(g):]
                if not rem or rem[0] in [',', ' ', '\t', '\xa0', '-', ':', '.', ';']:
                    detected_games.append(g)
                    working_line = rem.lstrip()
                    matched = True
                    break
        if matched:
            working_line = re.sub(r'^(?:,\s*|\s+and\s+|\s+|-|:|;)+', '', working_line, flags=re.IGNORECASE).lstrip()
            continue
        break
        
    contains_target = "1835" in detected_games
    has_prefixes = len(detected_games) > 0
    return contains_target, has_prefixes, working_line.strip()

def clean_final_prose(text):
    text = re.sub(r'^[.,\s\-\u00a0;:]+', '', text).strip()
    return text

def extract_game_from_html(key, target_game, raw_html_content):
    if isinstance(raw_html_content, bytes):
        html_text = raw_html_content.decode('windows-1252', errors='replace') 
    else:
        html_text = raw_html_content
        
    html_text = re.sub(r'(?i)<br\s*?/?>', ' ', html_text)
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
        first_cell_text = cells[0].get_text(" ", strip=True)
        if target_game == first_cell_text:
            cleaned_cells = [c.get_text(" ", strip=True) for c in cells]
            results.append(" | ".join(cleaned_cells))
                
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
                
            contains_target, has_prefixes, leftover = identify_leading_games(cleaned_line)
            
            if has_prefixes:
                if is_capturing and captured_lines:
                    full_prose = clean_final_prose(" ".join(captured_lines))
                    if full_prose:
                        results.append(f"{target_game} {full_prose}")
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
            if full_prose:
                results.append(f"{target_game} {full_prose}")
                        
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
                    out_file.write(f"[{title}] ({url})\n")
                    for hit in hits:
                        out_file.write(f"  {hit}\n")
                    out_file.write("-" * 50 + "\n")
            except urllib.error.HTTPError:
                pass
            except Exception as e:
                print(f"Error parsing section {title}: {str(e)}")
                
    print(f"Success! Clean output written to: {output_filepath}")

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "1835"
    run_extractor(target)