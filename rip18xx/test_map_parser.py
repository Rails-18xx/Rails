import unittest
import sys
import os

# Append the project root so we can import the generator
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from rip18xx.generate_rails_xml import RailsXMLConverter

class TestMapParsingLimits(unittest.TestCase):
    def setUp(self):
        # Initialize the converter with dummy paths
        self.converter = RailsXMLConverter("1830", "dummy", "dummy", "dummy")
        
        # Inject standard breaking parameters representing typical ruby output anomalies
        self.converter.map_rb = """
        LOCATION_NAMES = {
          'D2' => 'Lansing',
          'F2' => 'Chicago',
        }.freeze

        HEXES = {
          red: {
            ['F2'] => 'offboard=revenue:yellow_40|brown_70;path=a:3,b:_0;path=a:4,b:_0;path=a:5,b:_0',
          },
          gray: {
            ['D2'] => 'city=revenue:20;path=a:5,b:_0;path=a:4,b:_0',
          },
          white: {
            ['C17'] => 'upgrade=cost:120,terrain:mountain;border=edge:2,type:impassable',
          }
        }.freeze
        """
        
    def test_dynamic_parsing(self):
        self.converter.extract_location_names()
        self.converter._populate_map_data()
        
        # Verify edge impassable breaking translation
        self.assertIn("C17", self.converter.hex_details)
        self.assertEqual(self.converter.hex_details["C17"]["color"], "white")
        
        # Generate the XML string and verify critical nodes
        xml_output = self.converter.build_map_xml()
        
        self.assertTrue(all(x in xml_output for x in ['name="D2"', 'tile="-102"', 'city="Lansing"']))
        self.assertTrue(all(x in xml_output for x in ['name="F2"', 'tile="-903"', 'city="Chicago"', 'value="40,70"']))
        self.assertTrue('terrain="hills"' in xml_output and 'cost="120"' in xml_output)




def test_tile_manifest_integrity(self):
        # Inject map.rb manifest snippet
        self.converter.map_rb = """
        TILES = { '1' => 1, '2' => 2, '7' => 4 }.freeze
        """
        self.converter.extract_tiles_manifest()
        
        # Assert exact length tracking
        self.assertEqual(len(self.converter.tiles_manifest), 3)
        self.assertEqual(self.converter.tiles_manifest[0]["quantity"], "1")
        
        tileset_out = self.converter.build_tileset_xml()
        # Verify zero bloat quantity configurations are introduced
        self.assertNotIn('quantity="300"', tileset_out)

        
if __name__ == '__main__':
    unittest.main()