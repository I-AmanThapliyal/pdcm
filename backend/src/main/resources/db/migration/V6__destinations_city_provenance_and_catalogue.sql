-- V6: city + provenance columns, legacy backfill, ~80 Wikidata-verified destinations.
-- Structured coordinates/names from Wikidata (CC0). Descriptions are original PDCM wording.
-- No visit_reports are inserted.

ALTER TABLE destinations
    ADD COLUMN city VARCHAR(120),
    ADD COLUMN source_reference VARCHAR(120),
    ADD COLUMN source_url VARCHAR(500),
    ADD COLUMN source_license VARCHAR(80);

CREATE INDEX idx_destinations_city ON destinations (city);
CREATE INDEX idx_destinations_lat_lon ON destinations (latitude, longitude);

-- Legacy Day-1 seeds: provenance unknown; city filled from known place context.
UPDATE destinations SET city = CASE slug
    WHEN 'taj-mahal' THEN 'Agra'
    WHEN 'gateway-of-india' THEN 'Mumbai'
    WHEN 'india-gate' THEN 'New Delhi'
    WHEN 'red-fort' THEN 'New Delhi'
    WHEN 'jaipur-city-palace' THEN 'Jaipur'
    WHEN 'hawa-mahal' THEN 'Jaipur'
    WHEN 'amber-fort' THEN 'Jaipur'
    WHEN 'varanasi-ghats' THEN 'Varanasi'
    WHEN 'golden-temple' THEN 'Amritsar'
    WHEN 'meenakshi-temple' THEN 'Madurai'
    WHEN 'goa-calangute-beach' THEN 'Calangute'
    WHEN 'kovalam-beach' THEN 'Kovalam'
    WHEN 'manali-mall-road' THEN 'Manali'
    WHEN 'shimla-ridge' THEN 'Shimla'
    WHEN 'darjeeling-mall' THEN 'Darjeeling'
    WHEN 'mysore-palace' THEN 'Mysuru'
    WHEN 'hampi-virupaksha-temple' THEN 'Hampi'
    WHEN 'kaziranga-national-park' THEN 'Kohora'
    WHEN 'cherai-beach' THEN 'Cherai'
    WHEN 'pondicherry-promenade' THEN 'Puducherry'
    ELSE city END,
    source_reference = 'LEGACY_SEED',
    source_url = NULL,
    source_license = 'UNKNOWN'
WHERE source_reference IS NULL;

INSERT INTO destinations (name, slug, description, region, city, category, latitude, longitude, crowd_sensitivity, active, source_reference, source_url, source_license) VALUES
('Aga Khan Palace', 'aga-khan-palace', 'Historic palace linked to independence-era events on the edge of Pune.', 'Maharashtra', 'Pune', 'HISTORICAL', 18.552403, 73.901522, 'MEDIUM', TRUE, 'Wikidata Q2344976', 'https://www.wikidata.org/wiki/Q2344976', 'CC0'),
('Ajanta Caves', 'ajanta-caves', 'Rock-cut Buddhist caves known for murals in the Deccan hills.', 'Maharashtra', 'Aurangabad', 'HISTORICAL', 20.553417, 75.700472, 'MEDIUM', TRUE, 'Wikidata Q184427', 'https://www.wikidata.org/wiki/Q184427', 'CC0'),
('Amarnath Temple', 'amarnath-temple', 'High-altitude cave shrine that draws seasonal pilgrims in Kashmir.', 'Jammu and Kashmir', 'Pahalgam', 'TEMPLE', 34.216667, 75.516667, 'HIGH', TRUE, 'Wikidata Q455167', 'https://www.wikidata.org/wiki/Q455167', 'CC0'),
('Bandipur National Park', 'bandipur-national-park', 'Tiger reserve and dry forest on the Karnataka border.', 'Karnataka', 'Bandipur', 'WILDLIFE', 11.664444, 76.627222, 'LOW', TRUE, 'Wikidata Q665110', 'https://www.wikidata.org/wiki/Q665110', 'CC0'),
('Bekal Fort', 'bekal-fort', 'Seaside fort overlooking the Arabian Sea in northern Kerala.', 'Kerala', 'Bekal', 'HISTORICAL', 12.3929, 75.0336, 'MEDIUM', TRUE, 'Wikidata Q3519346', 'https://www.wikidata.org/wiki/Q3519346', 'CC0'),
('Belur Math', 'belur-math', 'Ramakrishna Math campus on the Hooghly riverfront.', 'West Bengal', 'Kolkata', 'TEMPLE', 22.632397, 88.356518, 'MEDIUM', TRUE, 'Wikidata Q816234', 'https://www.wikidata.org/wiki/Q816234', 'CC0'),
('Bhubaneswar Old Town Temples', 'bhubaneswar-old-town-temples', 'Temple cluster around Lingaraja and related shrines in Bhubaneswar.', 'Odisha', 'Bhubaneswar', 'CITY', 20.295, 85.825, 'MEDIUM', TRUE, 'Wikidata Q171771', 'https://www.wikidata.org/wiki/Q171771', 'CC0'),
('Bikaner Old City', 'bikaner-old-city', 'Desert city core known for markets and approaches to Junagarh Fort.', 'Rajasthan', 'Bikaner', 'CITY', 28.018056, 73.316944, 'MEDIUM', TRUE, 'Wikidata Q200718', 'https://www.wikidata.org/wiki/Q200718', 'CC0'),
('Bodh Gaya', 'bodh-gaya', 'Pilgrimage town centered on the Mahabodhi complex.', 'Bihar', 'Bodh Gaya', 'TEMPLE', 24.695, 84.9925, 'HIGH', TRUE, 'Wikidata Q176767', 'https://www.wikidata.org/wiki/Q176767', 'CC0'),
('Charminar', 'charminar', 'Four-minaret monument and bazaar landmark in old Hyderabad.', 'Telangana', 'Hyderabad', 'HISTORICAL', 17.361607, 78.474657, 'HIGH', TRUE, 'Wikidata Q1771696', 'https://www.wikidata.org/wiki/Q1771696', 'CC0'),
('Chhatrapati Shivaji Maharaj Terminus', 'chhatrapati-shivaji-maharaj-terminus', 'Victorian Gothic railway terminus and Mumbai landmark.', 'Maharashtra', 'Mumbai', 'HISTORICAL', 18.9398, 72.8355, 'HIGH', TRUE, 'Wikidata Q235884', 'https://www.wikidata.org/wiki/Q235884', 'CC0'),
('Chilika Lake', 'chilika-lake', 'Coastal lagoon known for birdlife and island boat routes.', 'Odisha', 'Satapada', 'NATURE', 19.716667, 85.316667, 'LOW', TRUE, 'Wikidata Q2047322', 'https://www.wikidata.org/wiki/Q2047322', 'CC0'),
('City Palace Udaipur', 'city-palace-udaipur', 'Lakeside palace complex overlooking Lake Pichola.', 'Rajasthan', 'Udaipur', 'HISTORICAL', 24.576, 73.683, 'HIGH', TRUE, 'Wikidata Q2723521', 'https://www.wikidata.org/wiki/Q2723521', 'CC0'),
('Dal Lake', 'dal-lake', 'Urban lake in Srinagar known for houseboats and gardens.', 'Jammu and Kashmir', 'Srinagar', 'NATURE', 34.12, 74.87, 'MEDIUM', TRUE, 'Wikidata Q325391', 'https://www.wikidata.org/wiki/Q325391', 'CC0'),
('Dudhwa National Park', 'dudhwa-national-park', 'Terai forest reserve with grasslands in northern Uttar Pradesh.', 'Uttar Pradesh', 'Lakhimpur Kheri', 'WILDLIFE', 28.470872, 80.699263, 'LOW', TRUE, 'Wikidata Q253455', 'https://www.wikidata.org/wiki/Q253455', 'CC0'),
('Elephanta Caves', 'elephanta-caves', 'Island rock-cut cave temples in Mumbai Harbour.', 'Maharashtra', 'Mumbai', 'HISTORICAL', 18.963333, 72.931389, 'MEDIUM', TRUE, 'Wikidata Q649384', 'https://www.wikidata.org/wiki/Q649384', 'CC0'),
('Ellora Caves', 'ellora-caves', 'Rock-cut temples and monasteries carved into Deccan cliffs.', 'Maharashtra', 'Aurangabad', 'HISTORICAL', 20.026389, 75.179167, 'MEDIUM', TRUE, 'Wikidata Q189616', 'https://www.wikidata.org/wiki/Q189616', 'CC0'),
('Fatehpur Sikri', 'fatehpur-sikri', 'Planned Mughal capital complex near Agra.', 'Uttar Pradesh', 'Fatehpur Sikri', 'HISTORICAL', 27.091111, 77.661111, 'MEDIUM', TRUE, 'Wikidata Q179046', 'https://www.wikidata.org/wiki/Q179046', 'CC0'),
('Gangtok', 'gangtok', 'Hill capital of Sikkim with ridge views and monastery roads.', 'Sikkim', 'Gangtok', 'HILL_STATION', 27.33, 88.62, 'MEDIUM', TRUE, 'Wikidata Q186141', 'https://www.wikidata.org/wiki/Q186141', 'CC0'),
('Gir National Park', 'gir-national-park', 'Forest sanctuary known for the Asiatic lion.', 'Gujarat', 'Sasan Gir', 'WILDLIFE', 21.13556, 70.79667, 'LOW', TRUE, 'Wikidata Q337028', 'https://www.wikidata.org/wiki/Q337028', 'CC0'),
('Gokarna', 'gokarna', 'Temple town and beach stretch on the Karnataka coast.', 'Karnataka', 'Gokarna', 'BEACH', 14.55, 74.31667, 'MEDIUM', TRUE, 'Wikidata Q1025271', 'https://www.wikidata.org/wiki/Q1025271', 'CC0'),
('Gulmarg', 'gulmarg', 'Meadow resort known for skiing and gondola views.', 'Jammu and Kashmir', 'Gulmarg', 'HILL_STATION', 34.047778, 74.384722, 'HIGH', TRUE, 'Wikidata Q1025272', 'https://www.wikidata.org/wiki/Q1025272', 'CC0'),
('Haridwar', 'haridwar', 'Ganges pilgrimage city known for riverfront aarti.', 'Uttarakhand', 'Haridwar', 'TEMPLE', 29.966667, 78.166667, 'HIGH', TRUE, 'Wikidata Q273030', 'https://www.wikidata.org/wiki/Q273030', 'CC0'),
('Hemis National Park', 'hemis-national-park', 'High-altitude park protecting snow leopard habitat in Ladakh.', 'Ladakh', 'Hemis', 'WILDLIFE', 33.983333, 77.433333, 'LOW', TRUE, 'Wikidata Q1604804', 'https://www.wikidata.org/wiki/Q1604804', 'CC0'),
('Howrah Bridge', 'howrah-bridge', 'Cantilever bridge spanning the Hooghly between Kolkata and Howrah.', 'West Bengal', 'Kolkata', 'CITY', 22.58527, 88.34694, 'HIGH', TRUE, 'Wikidata Q739641', 'https://www.wikidata.org/wiki/Q739641', 'CC0'),
('Jaisalmer Fort', 'jaisalmer-fort', 'Living fort of golden sandstone above the Thar Desert town.', 'Rajasthan', 'Jaisalmer', 'HISTORICAL', 26.9127, 70.9126, 'HIGH', TRUE, 'Wikidata Q3634368', 'https://www.wikidata.org/wiki/Q3634368', 'CC0'),
('Jantar Mantar Jaipur', 'jantar-mantar-jaipur', 'Open-air astronomical observatory instruments in Jaipur.', 'Rajasthan', 'Jaipur', 'HISTORICAL', 26.924722, 75.825, 'MEDIUM', TRUE, 'Wikidata Q508634', 'https://www.wikidata.org/wiki/Q508634', 'CC0'),
('Jim Corbett National Park', 'jim-corbett-national-park', 'Hill forest tiger reserve in the foothills of Uttarakhand.', 'Uttarakhand', 'Ramnagar', 'WILDLIFE', 29.533333, 78.933333, 'MEDIUM', TRUE, 'Wikidata Q949297', 'https://www.wikidata.org/wiki/Q949297', 'CC0'),
('Jodhpur Blue City Views', 'jodhpur-blue-city-views', 'Desert city known for blue-washed lanes below Mehrangarh.', 'Rajasthan', 'Jodhpur', 'CITY', 26.294444, 73.027778, 'MEDIUM', TRUE, 'Wikidata Q200019', 'https://www.wikidata.org/wiki/Q200019', 'CC0'),
('Junagarh Fort', 'junagarh-fort', 'Red sandstone fort complex in the center of Bikaner.', 'Rajasthan', 'Bikaner', 'HISTORICAL', 28.02201, 73.31772, 'MEDIUM', TRUE, 'Wikidata Q2758341', 'https://www.wikidata.org/wiki/Q2758341', 'CC0'),
('Kanheri Caves', 'kanheri-caves', 'Buddhist rock-cut caves inside Sanjay Gandhi National Park.', 'Maharashtra', 'Mumbai', 'HISTORICAL', 19.2083, 72.9064, 'LOW', TRUE, 'Wikidata Q1356198', 'https://www.wikidata.org/wiki/Q1356198', 'CC0'),
('Kanyakumari', 'kanyakumari', 'Southern tip of mainland India with coastal temples and sunrise views.', 'Tamil Nadu', 'Kanyakumari', 'NATURE', 8.091111, 77.541667, 'HIGH', TRUE, 'Wikidata Q222267', 'https://www.wikidata.org/wiki/Q222267', 'CC0'),
('Keoladeo National Park', 'keoladeo-national-park', 'Wetland bird sanctuary near Bharatpur.', 'Rajasthan', 'Bharatpur', 'WILDLIFE', 27.166667, 77.516667, 'MEDIUM', TRUE, 'Wikidata Q235878', 'https://www.wikidata.org/wiki/Q235878', 'CC0'),
('Khajuraho Temples', 'khajuraho-temples', 'Group of medieval temples known for sculpted sandstone walls.', 'Madhya Pradesh', 'Khajuraho', 'HISTORICAL', 24.85, 79.933333, 'MEDIUM', TRUE, 'Wikidata Q167298', 'https://www.wikidata.org/wiki/Q167298', 'CC0'),
('Kodaikanal', 'kodaikanal', 'Lake-centered hill station in the Palani Hills.', 'Tamil Nadu', 'Kodaikanal', 'HILL_STATION', 10.233333, 77.483333, 'MEDIUM', TRUE, 'Wikidata Q253851', 'https://www.wikidata.org/wiki/Q253851', 'CC0'),
('Konark Sun Temple', 'konark-sun-temple', 'Thirteenth-century temple designed as a stone chariot.', 'Odisha', 'Konark', 'TEMPLE', 19.887444, 86.094596, 'MEDIUM', TRUE, 'Wikidata Q752091', 'https://www.wikidata.org/wiki/Q752091', 'CC0'),
('Kumbhalgarh Fort', 'kumbhalgarh-fort', 'Hill fort with a long continuous wall in the Aravallis.', 'Rajasthan', 'Kumbhalgarh', 'HISTORICAL', 25.149167, 73.581111, 'MEDIUM', TRUE, 'Wikidata Q1246283', 'https://www.wikidata.org/wiki/Q1246283', 'CC0'),
('Leh Palace', 'leh-palace', 'Nine-storey royal palace overlooking the Indus valley town of Leh.', 'Ladakh', 'Leh', 'HISTORICAL', 34.166278, 77.586389, 'MEDIUM', TRUE, 'Wikidata Q3043543', 'https://www.wikidata.org/wiki/Q3043543', 'CC0'),
('Lonavala', 'lonavala', 'Western Ghats hill town popular for monsoon viewpoints.', 'Maharashtra', 'Lonavala', 'HILL_STATION', 18.748061, 73.407219, 'HIGH', TRUE, 'Wikidata Q1929511', 'https://www.wikidata.org/wiki/Q1929511', 'CC0'),
('Lotus Temple', 'lotus-temple', 'Bahai House of Worship known for its lotus-shaped dome.', 'Delhi', 'New Delhi', 'TEMPLE', 28.553325, 77.2586, 'MEDIUM', TRUE, 'Wikidata Q940843', 'https://www.wikidata.org/wiki/Q940843', 'CC0'),
('Mahabaleshwar', 'mahabaleshwar', 'Plateau hill station with strawberry farms and valley viewpoints.', 'Maharashtra', 'Mahabaleshwar', 'HILL_STATION', 17.925, 73.6575, 'MEDIUM', TRUE, 'Wikidata Q463273', 'https://www.wikidata.org/wiki/Q463273', 'CC0'),
('Marine Drive', 'marine-drive', 'Crescent seaside promenade along the Back Bay in south Mumbai.', 'Maharashtra', 'Mumbai', 'CITY', 18.944, 72.823, 'HIGH', TRUE, 'Wikidata Q985828', 'https://www.wikidata.org/wiki/Q985828', 'CC0'),
('Mehrangarh Fort', 'mehrangarh-fort', 'Massive fort rising above the blue lanes of Jodhpur.', 'Rajasthan', 'Jodhpur', 'HISTORICAL', 26.298056, 73.018889, 'HIGH', TRUE, 'Wikidata Q1483099', 'https://www.wikidata.org/wiki/Q1483099', 'CC0'),
('Munnar', 'munnar', 'Tea-estate hill town in the Western Ghats of Kerala.', 'Kerala', 'Munnar', 'HILL_STATION', 10.089167, 77.059722, 'MEDIUM', TRUE, 'Wikidata Q1025124', 'https://www.wikidata.org/wiki/Q1025124', 'CC0'),
('Mussoorie', 'mussoorie', 'Ridge hill station above Dehradun with colonial-era promenades.', 'Uttarakhand', 'Mussoorie', 'HILL_STATION', 30.456111, 78.078056, 'HIGH', TRUE, 'Wikidata Q668395', 'https://www.wikidata.org/wiki/Q668395', 'CC0'),
('Nahargarh Fort', 'nahargarh-fort', 'Hill fort overlooking Jaipur with sunset city views.', 'Rajasthan', 'Jaipur', 'HISTORICAL', 26.9375, 75.815833, 'MEDIUM', TRUE, 'Wikidata Q2723401', 'https://www.wikidata.org/wiki/Q2723401', 'CC0'),
('Nainital', 'nainital', 'Lake basin hill station in the Kumaon hills.', 'Uttarakhand', 'Nainital', 'HILL_STATION', 29.391944, 79.454167, 'HIGH', TRUE, 'Wikidata Q773213', 'https://www.wikidata.org/wiki/Q773213', 'CC0'),
('Ooty', 'ooty', 'Nilgiri hill station known for botanical gardens and toy-train approaches.', 'Tamil Nadu', 'Udhagamandalam', 'HILL_STATION', 11.408611, 76.693889, 'HIGH', TRUE, 'Wikidata Q9888', 'https://www.wikidata.org/wiki/Q9888', 'CC0'),
('Pahalgam', 'pahalgam', 'Lidder valley town used as a base for highland walks.', 'Jammu and Kashmir', 'Pahalgam', 'HILL_STATION', 34.032222, 75.322222, 'MEDIUM', TRUE, 'Wikidata Q2085326', 'https://www.wikidata.org/wiki/Q2085326', 'CC0'),
('Pangong Tso', 'pangong-tso', 'High-altitude lake straddling the Ladakh landscape.', 'Ladakh', 'Pangong', 'NATURE', 33.704722, 78.75, 'MEDIUM', TRUE, 'Wikidata Q1032254', 'https://www.wikidata.org/wiki/Q1032254', 'CC0'),
('Pattadakal Temples', 'pattadakal-temples', 'Chalukya temple ensemble on the Malaprabha river.', 'Karnataka', 'Pattadakal', 'HISTORICAL', 15.951333, 75.814639, 'LOW', TRUE, 'Wikidata Q817250', 'https://www.wikidata.org/wiki/Q817250', 'CC0'),
('Pench National Park', 'pench-national-park', 'Tiger reserve spanning forest tracts of Madhya Pradesh and Maharashtra.', 'Madhya Pradesh', 'Seoni', 'WILDLIFE', 21.6716, 79.3033, 'LOW', TRUE, 'Wikidata Q2720864', 'https://www.wikidata.org/wiki/Q2720864', 'CC0'),
('Periyar National Park', 'periyar-national-park', 'Wildlife reserve around a reservoir in the Cardamom Hills.', 'Kerala', 'Thekkady', 'WILDLIFE', 9.5775, 77.18, 'MEDIUM', TRUE, 'Wikidata Q548153', 'https://www.wikidata.org/wiki/Q548153', 'CC0'),
('Brihadeeswarar Temple', 'brihadeeswarar-temple', 'Chola-era temple with a towering sanctum in Thanjavur.', 'Tamil Nadu', 'Thanjavur', 'TEMPLE', 10.783056, 79.1325, 'MEDIUM', TRUE, 'Wikidata Q916943', 'https://www.wikidata.org/wiki/Q916943', 'CC0'),
('Pushkar', 'pushkar', 'Pilgrimage town around a sacred lake and Brahma temple.', 'Rajasthan', 'Pushkar', 'TEMPLE', 26.490278, 74.554167, 'HIGH', TRUE, 'Wikidata Q749170', 'https://www.wikidata.org/wiki/Q749170', 'CC0'),
('Qutb Minar', 'qutb-minar', 'Towering minaret and surrounding ruins in Mehrauli.', 'Delhi', 'New Delhi', 'HISTORICAL', 28.524355, 77.185248, 'HIGH', TRUE, 'Wikidata Q187635', 'https://www.wikidata.org/wiki/Q187635', 'CC0'),
('Rajgir', 'rajgir', 'Ancient hills and hot springs linked to early Buddhist and Jain sites.', 'Bihar', 'Rajgir', 'HISTORICAL', 25.03, 85.42, 'MEDIUM', TRUE, 'Wikidata Q863683', 'https://www.wikidata.org/wiki/Q863683', 'CC0'),
('Ranthambore National Park', 'ranthambore-national-park', 'Dry forest tiger reserve around lakes and fort ruins.', 'Rajasthan', 'Sawai Madhopur', 'WILDLIFE', 26.01733, 76.50257, 'MEDIUM', TRUE, 'Wikidata Q1466242', 'https://www.wikidata.org/wiki/Q1466242', 'CC0'),
('Rishikesh', 'rishikesh', 'Ganges town known for ashrams, bridges, and river adventure sports.', 'Uttarakhand', 'Rishikesh', 'TEMPLE', 30.1, 78.3, 'HIGH', TRUE, 'Wikidata Q502141', 'https://www.wikidata.org/wiki/Q502141', 'CC0'),
('Rohtang Pass', 'rohtang-pass', 'High mountain pass north of Manali open seasonally.', 'Himachal Pradesh', 'Manali', 'HILL_STATION', 32.371389, 77.246389, 'HIGH', TRUE, 'Wikidata Q633415', 'https://www.wikidata.org/wiki/Q633415', 'CC0'),
('Sabarmati Ashram', 'sabarmati-ashram', 'Riverside ashram associated with Mahatma Gandhi in Ahmedabad.', 'Gujarat', 'Ahmedabad', 'HISTORICAL', 23.06, 72.580833, 'MEDIUM', TRUE, 'Wikidata Q1410702', 'https://www.wikidata.org/wiki/Q1410702', 'CC0'),
('Sanchi Stupa', 'sanchi-stupa', 'Hilltop Buddhist stupas and gateways near Bhopal.', 'Madhya Pradesh', 'Sanchi', 'HISTORICAL', 23.479444, 77.739722, 'MEDIUM', TRUE, 'Wikidata Q181123', 'https://www.wikidata.org/wiki/Q181123', 'CC0'),
('Sanjay Gandhi National Park', 'sanjay-gandhi-national-park', 'Urban national park with forests and caves inside Mumbai.', 'Maharashtra', 'Mumbai', 'NATURE', 19.25, 72.9167, 'MEDIUM', TRUE, 'Wikidata Q648907', 'https://www.wikidata.org/wiki/Q648907', 'CC0'),
('Shaniwar Wada', 'shaniwar-wada', 'Fortified palace ruins in the old city of Pune.', 'Maharashtra', 'Pune', 'HISTORICAL', 18.519167, 73.855278, 'MEDIUM', TRUE, 'Wikidata Q1351060', 'https://www.wikidata.org/wiki/Q1351060', 'CC0'),
('Shillong', 'shillong', 'Hill capital of Meghalaya known for ridges and nearby waterfalls.', 'Meghalaya', 'Shillong', 'HILL_STATION', 25.574444, 91.878889, 'MEDIUM', TRUE, 'Wikidata Q207752', 'https://www.wikidata.org/wiki/Q207752', 'CC0'),
('Shore Temple', 'shore-temple', 'Seaside stone temple on the Coromandel Coast.', 'Tamil Nadu', 'Mahabalipuram', 'HISTORICAL', 12.616389, 80.198611, 'MEDIUM', TRUE, 'Wikidata Q1144279', 'https://www.wikidata.org/wiki/Q1144279', 'CC0'),
('Solang Valley', 'solang-valley', 'Adventure valley near Manali used for snow and cable activities.', 'Himachal Pradesh', 'Manali', 'HILL_STATION', 32.315133, 77.155369, 'HIGH', TRUE, 'Wikidata Q12069422', 'https://www.wikidata.org/wiki/Q12069422', 'CC0'),
('Sonamarg', 'sonamarg', 'Meadow valley on the road toward Zoji La.', 'Jammu and Kashmir', 'Sonamarg', 'HILL_STATION', 34.302778, 75.295833, 'MEDIUM', TRUE, 'Wikidata Q4379433', 'https://www.wikidata.org/wiki/Q4379433', 'CC0'),
('Statue of Unity', 'statue-of-unity', 'Tall statue complex beside the Narmada near Kevadia.', 'Gujarat', 'Kevadia', 'HISTORICAL', 21.838, 73.7191, 'HIGH', TRUE, 'Wikidata Q7604504', 'https://www.wikidata.org/wiki/Q7604504', 'CC0'),
('Sundarbans National Park', 'sundarbans-national-park', 'Mangrove tiger landscape in the Ganges delta.', 'West Bengal', 'Gosaba', 'WILDLIFE', 21.75, 88.75, 'LOW', TRUE, 'Wikidata Q532440', 'https://www.wikidata.org/wiki/Q532440', 'CC0'),
('Tadoba Andhari Tiger Reserve', 'tadoba-andhari-tiger-reserve', 'Central Indian tiger reserve with lakes and teak forest.', 'Maharashtra', 'Chandrapur', 'WILDLIFE', 20.267, 79.4291, 'LOW', TRUE, 'Wikidata Q2639563', 'https://www.wikidata.org/wiki/Q2639563', 'CC0'),
('Thanjavur', 'thanjavur', 'Historic Chola city known for temples and bronze craft.', 'Tamil Nadu', 'Thanjavur', 'CITY', 10.787, 79.137806, 'MEDIUM', TRUE, 'Wikidata Q41496', 'https://www.wikidata.org/wiki/Q41496', 'CC0'),
('Thikse Monastery', 'thikse-monastery', 'Terraced Buddhist monastery overlooking the Indus valley.', 'Ladakh', 'Thiksey', 'TEMPLE', 34.056389, 77.666667, 'MEDIUM', TRUE, 'Wikidata Q867066', 'https://www.wikidata.org/wiki/Q867066', 'CC0'),
('Tirupati', 'tirupati', 'Pilgrimage city at the foot of the Tirumala hills.', 'Andhra Pradesh', 'Tirupati', 'TEMPLE', 13.65, 79.42, 'HIGH', TRUE, 'Wikidata Q1436816', 'https://www.wikidata.org/wiki/Q1436816', 'CC0'),
('Tso Moriri', 'tso-moriri', 'High-altitude lake in Changthang with sparse shoreline settlements.', 'Ladakh', 'Korzok', 'NATURE', 32.9, 78.3, 'LOW', TRUE, 'Wikidata Q2087758', 'https://www.wikidata.org/wiki/Q2087758', 'CC0'),
('Ujjain', 'ujjain', 'Ancient city on the Shipra known for the Mahakaleshwar temple.', 'Madhya Pradesh', 'Ujjain', 'TEMPLE', 23.182778, 75.777222, 'HIGH', TRUE, 'Wikidata Q200119', 'https://www.wikidata.org/wiki/Q200119', 'CC0'),
('Vaishno Devi Temple', 'vaishno-devi-temple', 'Hill shrine reached by trek or pony routes from Katra.', 'Jammu and Kashmir', 'Katra', 'TEMPLE', 33.03, 74.948056, 'HIGH', TRUE, 'Wikidata Q611371', 'https://www.wikidata.org/wiki/Q611371', 'CC0'),
('Valley of Flowers National Park', 'valley-of-flowers-national-park', 'Alpine flower meadows in the Garhwal Himalaya.', 'Uttarakhand', 'Joshimath', 'NATURE', 30.73, 79.6175, 'LOW', TRUE, 'Wikidata Q605897', 'https://www.wikidata.org/wiki/Q605897', 'CC0'),
('Varkala Cliff Beach', 'varkala-cliff-beach', 'Cliff-backed beach and temple area on the Kerala coast.', 'Kerala', 'Varkala', 'BEACH', 8.733056, 76.716944, 'MEDIUM', TRUE, 'Wikidata Q1012951', 'https://www.wikidata.org/wiki/Q1012951', 'CC0'),
('Victoria Memorial', 'victoria-memorial', 'Marble museum-memorial set in gardens in central Kolkata.', 'West Bengal', 'Kolkata', 'HISTORICAL', 22.5449, 88.3425, 'HIGH', TRUE, 'Wikidata Q1356352', 'https://www.wikidata.org/wiki/Q1356352', 'CC0');

INSERT INTO destination_tags (destination_id, tag)
SELECT d.id, 'pune' FROM destinations d WHERE d.slug = 'aga-khan-palace'
UNION ALL
SELECT d.id, 'heritage' FROM destinations d WHERE d.slug = 'aga-khan-palace'
UNION ALL
SELECT d.id, 'palace' FROM destinations d WHERE d.slug = 'aga-khan-palace'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'ajanta-caves'
UNION ALL
SELECT d.id, 'caves' FROM destinations d WHERE d.slug = 'ajanta-caves'
UNION ALL
SELECT d.id, 'heritage' FROM destinations d WHERE d.slug = 'ajanta-caves'
UNION ALL
SELECT d.id, 'pilgrimage' FROM destinations d WHERE d.slug = 'amarnath-temple'
UNION ALL
SELECT d.id, 'himalaya' FROM destinations d WHERE d.slug = 'amarnath-temple'
UNION ALL
SELECT d.id, 'seasonal' FROM destinations d WHERE d.slug = 'amarnath-temple'
UNION ALL
SELECT d.id, 'safari' FROM destinations d WHERE d.slug = 'bandipur-national-park'
UNION ALL
SELECT d.id, 'tiger' FROM destinations d WHERE d.slug = 'bandipur-national-park'
UNION ALL
SELECT d.id, 'nature' FROM destinations d WHERE d.slug = 'bandipur-national-park'
UNION ALL
SELECT d.id, 'fort' FROM destinations d WHERE d.slug = 'bekal-fort'
UNION ALL
SELECT d.id, 'seaside' FROM destinations d WHERE d.slug = 'bekal-fort'
UNION ALL
SELECT d.id, 'kerala' FROM destinations d WHERE d.slug = 'bekal-fort'
UNION ALL
SELECT d.id, 'spiritual' FROM destinations d WHERE d.slug = 'belur-math'
UNION ALL
SELECT d.id, 'kolkata' FROM destinations d WHERE d.slug = 'belur-math'
UNION ALL
SELECT d.id, 'campus' FROM destinations d WHERE d.slug = 'belur-math'
UNION ALL
SELECT d.id, 'temples' FROM destinations d WHERE d.slug = 'bhubaneswar-old-town-temples'
UNION ALL
SELECT d.id, 'odisha' FROM destinations d WHERE d.slug = 'bhubaneswar-old-town-temples'
UNION ALL
SELECT d.id, 'heritage' FROM destinations d WHERE d.slug = 'bhubaneswar-old-town-temples'
UNION ALL
SELECT d.id, 'desert' FROM destinations d WHERE d.slug = 'bikaner-old-city'
UNION ALL
SELECT d.id, 'rajasthan' FROM destinations d WHERE d.slug = 'bikaner-old-city'
UNION ALL
SELECT d.id, 'markets' FROM destinations d WHERE d.slug = 'bikaner-old-city'
UNION ALL
SELECT d.id, 'pilgrimage' FROM destinations d WHERE d.slug = 'bodh-gaya'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'bodh-gaya'
UNION ALL
SELECT d.id, 'bihar' FROM destinations d WHERE d.slug = 'bodh-gaya'
UNION ALL
SELECT d.id, 'hyderabad' FROM destinations d WHERE d.slug = 'charminar'
UNION ALL
SELECT d.id, 'monument' FROM destinations d WHERE d.slug = 'charminar'
UNION ALL
SELECT d.id, 'bazaar' FROM destinations d WHERE d.slug = 'charminar'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'chhatrapati-shivaji-maharaj-terminus'
UNION ALL
SELECT d.id, 'mumbai' FROM destinations d WHERE d.slug = 'chhatrapati-shivaji-maharaj-terminus'
UNION ALL
SELECT d.id, 'railway' FROM destinations d WHERE d.slug = 'chhatrapati-shivaji-maharaj-terminus'
UNION ALL
SELECT d.id, 'lagoon' FROM destinations d WHERE d.slug = 'chilika-lake'
UNION ALL
SELECT d.id, 'birds' FROM destinations d WHERE d.slug = 'chilika-lake'
UNION ALL
SELECT d.id, 'odisha' FROM destinations d WHERE d.slug = 'chilika-lake'
UNION ALL
SELECT d.id, 'udaipur' FROM destinations d WHERE d.slug = 'city-palace-udaipur'
UNION ALL
SELECT d.id, 'palace' FROM destinations d WHERE d.slug = 'city-palace-udaipur'
UNION ALL
SELECT d.id, 'lake' FROM destinations d WHERE d.slug = 'city-palace-udaipur'
UNION ALL
SELECT d.id, 'srinagar' FROM destinations d WHERE d.slug = 'dal-lake'
UNION ALL
SELECT d.id, 'lake' FROM destinations d WHERE d.slug = 'dal-lake'
UNION ALL
SELECT d.id, 'houseboats' FROM destinations d WHERE d.slug = 'dal-lake'
UNION ALL
SELECT d.id, 'tiger' FROM destinations d WHERE d.slug = 'dudhwa-national-park'
UNION ALL
SELECT d.id, 'terai' FROM destinations d WHERE d.slug = 'dudhwa-national-park'
UNION ALL
SELECT d.id, 'safari' FROM destinations d WHERE d.slug = 'dudhwa-national-park'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'elephanta-caves'
UNION ALL
SELECT d.id, 'caves' FROM destinations d WHERE d.slug = 'elephanta-caves'
UNION ALL
SELECT d.id, 'mumbai' FROM destinations d WHERE d.slug = 'elephanta-caves'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'ellora-caves'
UNION ALL
SELECT d.id, 'caves' FROM destinations d WHERE d.slug = 'ellora-caves'
UNION ALL
SELECT d.id, 'heritage' FROM destinations d WHERE d.slug = 'ellora-caves'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'fatehpur-sikri'
UNION ALL
SELECT d.id, 'mughal' FROM destinations d WHERE d.slug = 'fatehpur-sikri'
UNION ALL
SELECT d.id, 'agra' FROM destinations d WHERE d.slug = 'fatehpur-sikri'
UNION ALL
SELECT d.id, 'sikkim' FROM destinations d WHERE d.slug = 'gangtok'
UNION ALL
SELECT d.id, 'hills' FROM destinations d WHERE d.slug = 'gangtok'
UNION ALL
SELECT d.id, 'capital' FROM destinations d WHERE d.slug = 'gangtok'
UNION ALL
SELECT d.id, 'lion' FROM destinations d WHERE d.slug = 'gir-national-park'
UNION ALL
SELECT d.id, 'safari' FROM destinations d WHERE d.slug = 'gir-national-park'
UNION ALL
SELECT d.id, 'gujarat' FROM destinations d WHERE d.slug = 'gir-national-park'
UNION ALL
SELECT d.id, 'beach' FROM destinations d WHERE d.slug = 'gokarna'
UNION ALL
SELECT d.id, 'temple-town' FROM destinations d WHERE d.slug = 'gokarna'
UNION ALL
SELECT d.id, 'karnataka' FROM destinations d WHERE d.slug = 'gokarna'
UNION ALL
SELECT d.id, 'ski' FROM destinations d WHERE d.slug = 'gulmarg'
UNION ALL
SELECT d.id, 'meadow' FROM destinations d WHERE d.slug = 'gulmarg'
UNION ALL
SELECT d.id, 'kashmir' FROM destinations d WHERE d.slug = 'gulmarg'
UNION ALL
SELECT d.id, 'pilgrimage' FROM destinations d WHERE d.slug = 'haridwar'
UNION ALL
SELECT d.id, 'ganges' FROM destinations d WHERE d.slug = 'haridwar'
UNION ALL
SELECT d.id, 'uttarakhand' FROM destinations d WHERE d.slug = 'haridwar'
UNION ALL
SELECT d.id, 'snow-leopard' FROM destinations d WHERE d.slug = 'hemis-national-park'
UNION ALL
SELECT d.id, 'ladakh' FROM destinations d WHERE d.slug = 'hemis-national-park'
UNION ALL
SELECT d.id, 'wildlife' FROM destinations d WHERE d.slug = 'hemis-national-park'
UNION ALL
SELECT d.id, 'kolkata' FROM destinations d WHERE d.slug = 'howrah-bridge'
UNION ALL
SELECT d.id, 'bridge' FROM destinations d WHERE d.slug = 'howrah-bridge'
UNION ALL
SELECT d.id, 'icon' FROM destinations d WHERE d.slug = 'howrah-bridge'
UNION ALL
SELECT d.id, 'fort' FROM destinations d WHERE d.slug = 'jaisalmer-fort'
UNION ALL
SELECT d.id, 'desert' FROM destinations d WHERE d.slug = 'jaisalmer-fort'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'jaisalmer-fort'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'jantar-mantar-jaipur'
UNION ALL
SELECT d.id, 'observatory' FROM destinations d WHERE d.slug = 'jantar-mantar-jaipur'
UNION ALL
SELECT d.id, 'jaipur' FROM destinations d WHERE d.slug = 'jantar-mantar-jaipur'
UNION ALL
SELECT d.id, 'tiger' FROM destinations d WHERE d.slug = 'jim-corbett-national-park'
UNION ALL
SELECT d.id, 'safari' FROM destinations d WHERE d.slug = 'jim-corbett-national-park'
UNION ALL
SELECT d.id, 'foothills' FROM destinations d WHERE d.slug = 'jim-corbett-national-park'
UNION ALL
SELECT d.id, 'jodhpur' FROM destinations d WHERE d.slug = 'jodhpur-blue-city-views'
UNION ALL
SELECT d.id, 'desert' FROM destinations d WHERE d.slug = 'jodhpur-blue-city-views'
UNION ALL
SELECT d.id, 'cityscape' FROM destinations d WHERE d.slug = 'jodhpur-blue-city-views'
UNION ALL
SELECT d.id, 'fort' FROM destinations d WHERE d.slug = 'junagarh-fort'
UNION ALL
SELECT d.id, 'bikaner' FROM destinations d WHERE d.slug = 'junagarh-fort'
UNION ALL
SELECT d.id, 'palace' FROM destinations d WHERE d.slug = 'junagarh-fort'
UNION ALL
SELECT d.id, 'caves' FROM destinations d WHERE d.slug = 'kanheri-caves'
UNION ALL
SELECT d.id, 'mumbai' FROM destinations d WHERE d.slug = 'kanheri-caves'
UNION ALL
SELECT d.id, 'forest' FROM destinations d WHERE d.slug = 'kanheri-caves'
UNION ALL
SELECT d.id, 'coast' FROM destinations d WHERE d.slug = 'kanyakumari'
UNION ALL
SELECT d.id, 'tamil-nadu' FROM destinations d WHERE d.slug = 'kanyakumari'
UNION ALL
SELECT d.id, 'viewpoint' FROM destinations d WHERE d.slug = 'kanyakumari'
UNION ALL
SELECT d.id, 'birds' FROM destinations d WHERE d.slug = 'keoladeo-national-park'
UNION ALL
SELECT d.id, 'wetland' FROM destinations d WHERE d.slug = 'keoladeo-national-park'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'keoladeo-national-park'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'khajuraho-temples'
UNION ALL
SELECT d.id, 'temples' FROM destinations d WHERE d.slug = 'khajuraho-temples'
UNION ALL
SELECT d.id, 'heritage' FROM destinations d WHERE d.slug = 'khajuraho-temples'
UNION ALL
SELECT d.id, 'lake' FROM destinations d WHERE d.slug = 'kodaikanal'
UNION ALL
SELECT d.id, 'hills' FROM destinations d WHERE d.slug = 'kodaikanal'
UNION ALL
SELECT d.id, 'tamil-nadu' FROM destinations d WHERE d.slug = 'kodaikanal'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'konark-sun-temple'
UNION ALL
SELECT d.id, 'temple' FROM destinations d WHERE d.slug = 'konark-sun-temple'
UNION ALL
SELECT d.id, 'odisha' FROM destinations d WHERE d.slug = 'konark-sun-temple'
UNION ALL
SELECT d.id, 'fort' FROM destinations d WHERE d.slug = 'kumbhalgarh-fort'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'kumbhalgarh-fort'
UNION ALL
SELECT d.id, 'rajasthan' FROM destinations d WHERE d.slug = 'kumbhalgarh-fort'
UNION ALL
SELECT d.id, 'ladakh' FROM destinations d WHERE d.slug = 'leh-palace'
UNION ALL
SELECT d.id, 'palace' FROM destinations d WHERE d.slug = 'leh-palace'
UNION ALL
SELECT d.id, 'views' FROM destinations d WHERE d.slug = 'leh-palace'
UNION ALL
SELECT d.id, 'ghats' FROM destinations d WHERE d.slug = 'lonavala'
UNION ALL
SELECT d.id, 'weekend' FROM destinations d WHERE d.slug = 'lonavala'
UNION ALL
SELECT d.id, 'maharashtra' FROM destinations d WHERE d.slug = 'lonavala'
UNION ALL
SELECT d.id, 'delhi' FROM destinations d WHERE d.slug = 'lotus-temple'
UNION ALL
SELECT d.id, 'architecture' FROM destinations d WHERE d.slug = 'lotus-temple'
UNION ALL
SELECT d.id, 'quiet-zone' FROM destinations d WHERE d.slug = 'lotus-temple'
UNION ALL
SELECT d.id, 'views' FROM destinations d WHERE d.slug = 'mahabaleshwar'
UNION ALL
SELECT d.id, 'ghats' FROM destinations d WHERE d.slug = 'mahabaleshwar'
UNION ALL
SELECT d.id, 'weekend' FROM destinations d WHERE d.slug = 'mahabaleshwar'
UNION ALL
SELECT d.id, 'mumbai' FROM destinations d WHERE d.slug = 'marine-drive'
UNION ALL
SELECT d.id, 'promenade' FROM destinations d WHERE d.slug = 'marine-drive'
UNION ALL
SELECT d.id, 'evening' FROM destinations d WHERE d.slug = 'marine-drive'
UNION ALL
SELECT d.id, 'fort' FROM destinations d WHERE d.slug = 'mehrangarh-fort'
UNION ALL
SELECT d.id, 'jodhpur' FROM destinations d WHERE d.slug = 'mehrangarh-fort'
UNION ALL
SELECT d.id, 'views' FROM destinations d WHERE d.slug = 'mehrangarh-fort'
UNION ALL
SELECT d.id, 'tea' FROM destinations d WHERE d.slug = 'munnar'
UNION ALL
SELECT d.id, 'hills' FROM destinations d WHERE d.slug = 'munnar'
UNION ALL
SELECT d.id, 'kerala' FROM destinations d WHERE d.slug = 'munnar'
UNION ALL
SELECT d.id, 'hills' FROM destinations d WHERE d.slug = 'mussoorie'
UNION ALL
SELECT d.id, 'views' FROM destinations d WHERE d.slug = 'mussoorie'
UNION ALL
SELECT d.id, 'uttarakhand' FROM destinations d WHERE d.slug = 'mussoorie'
UNION ALL
SELECT d.id, 'fort' FROM destinations d WHERE d.slug = 'nahargarh-fort'
UNION ALL
SELECT d.id, 'jaipur' FROM destinations d WHERE d.slug = 'nahargarh-fort'
UNION ALL
SELECT d.id, 'views' FROM destinations d WHERE d.slug = 'nahargarh-fort'
UNION ALL
SELECT d.id, 'lake' FROM destinations d WHERE d.slug = 'nainital'
UNION ALL
SELECT d.id, 'hills' FROM destinations d WHERE d.slug = 'nainital'
UNION ALL
SELECT d.id, 'uttarakhand' FROM destinations d WHERE d.slug = 'nainital'
UNION ALL
SELECT d.id, 'nilgiris' FROM destinations d WHERE d.slug = 'ooty'
UNION ALL
SELECT d.id, 'hills' FROM destinations d WHERE d.slug = 'ooty'
UNION ALL
SELECT d.id, 'tamil-nadu' FROM destinations d WHERE d.slug = 'ooty'
UNION ALL
SELECT d.id, 'valley' FROM destinations d WHERE d.slug = 'pahalgam'
UNION ALL
SELECT d.id, 'kashmir' FROM destinations d WHERE d.slug = 'pahalgam'
UNION ALL
SELECT d.id, 'base-town' FROM destinations d WHERE d.slug = 'pahalgam'
UNION ALL
SELECT d.id, 'lake' FROM destinations d WHERE d.slug = 'pangong-tso'
UNION ALL
SELECT d.id, 'ladakh' FROM destinations d WHERE d.slug = 'pangong-tso'
UNION ALL
SELECT d.id, 'scenic' FROM destinations d WHERE d.slug = 'pangong-tso'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'pattadakal-temples'
UNION ALL
SELECT d.id, 'temples' FROM destinations d WHERE d.slug = 'pattadakal-temples'
UNION ALL
SELECT d.id, 'karnataka' FROM destinations d WHERE d.slug = 'pattadakal-temples'
UNION ALL
SELECT d.id, 'tiger' FROM destinations d WHERE d.slug = 'pench-national-park'
UNION ALL
SELECT d.id, 'safari' FROM destinations d WHERE d.slug = 'pench-national-park'
UNION ALL
SELECT d.id, 'forest' FROM destinations d WHERE d.slug = 'pench-national-park'
UNION ALL
SELECT d.id, 'elephant' FROM destinations d WHERE d.slug = 'periyar-national-park'
UNION ALL
SELECT d.id, 'boat' FROM destinations d WHERE d.slug = 'periyar-national-park'
UNION ALL
SELECT d.id, 'kerala' FROM destinations d WHERE d.slug = 'periyar-national-park'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'brihadeeswarar-temple'
UNION ALL
SELECT d.id, 'chola' FROM destinations d WHERE d.slug = 'brihadeeswarar-temple'
UNION ALL
SELECT d.id, 'temple' FROM destinations d WHERE d.slug = 'brihadeeswarar-temple'
UNION ALL
SELECT d.id, 'pilgrimage' FROM destinations d WHERE d.slug = 'pushkar'
UNION ALL
SELECT d.id, 'lake' FROM destinations d WHERE d.slug = 'pushkar'
UNION ALL
SELECT d.id, 'rajasthan' FROM destinations d WHERE d.slug = 'pushkar'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'qutb-minar'
UNION ALL
SELECT d.id, 'delhi' FROM destinations d WHERE d.slug = 'qutb-minar'
UNION ALL
SELECT d.id, 'minaret' FROM destinations d WHERE d.slug = 'qutb-minar'
UNION ALL
SELECT d.id, 'bihar' FROM destinations d WHERE d.slug = 'rajgir'
UNION ALL
SELECT d.id, 'hills' FROM destinations d WHERE d.slug = 'rajgir'
UNION ALL
SELECT d.id, 'heritage' FROM destinations d WHERE d.slug = 'rajgir'
UNION ALL
SELECT d.id, 'tiger' FROM destinations d WHERE d.slug = 'ranthambore-national-park'
UNION ALL
SELECT d.id, 'safari' FROM destinations d WHERE d.slug = 'ranthambore-national-park'
UNION ALL
SELECT d.id, 'rajasthan' FROM destinations d WHERE d.slug = 'ranthambore-national-park'
UNION ALL
SELECT d.id, 'ganges' FROM destinations d WHERE d.slug = 'rishikesh'
UNION ALL
SELECT d.id, 'yoga' FROM destinations d WHERE d.slug = 'rishikesh'
UNION ALL
SELECT d.id, 'adventure' FROM destinations d WHERE d.slug = 'rishikesh'
UNION ALL
SELECT d.id, 'pass' FROM destinations d WHERE d.slug = 'rohtang-pass'
UNION ALL
SELECT d.id, 'snow' FROM destinations d WHERE d.slug = 'rohtang-pass'
UNION ALL
SELECT d.id, 'seasonal' FROM destinations d WHERE d.slug = 'rohtang-pass'
UNION ALL
SELECT d.id, 'gandhi' FROM destinations d WHERE d.slug = 'sabarmati-ashram'
UNION ALL
SELECT d.id, 'ashram' FROM destinations d WHERE d.slug = 'sabarmati-ashram'
UNION ALL
SELECT d.id, 'ahmedabad' FROM destinations d WHERE d.slug = 'sabarmati-ashram'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'sanchi-stupa'
UNION ALL
SELECT d.id, 'buddhist' FROM destinations d WHERE d.slug = 'sanchi-stupa'
UNION ALL
SELECT d.id, 'heritage' FROM destinations d WHERE d.slug = 'sanchi-stupa'
UNION ALL
SELECT d.id, 'forest' FROM destinations d WHERE d.slug = 'sanjay-gandhi-national-park'
UNION ALL
SELECT d.id, 'mumbai' FROM destinations d WHERE d.slug = 'sanjay-gandhi-national-park'
UNION ALL
SELECT d.id, 'day-trip' FROM destinations d WHERE d.slug = 'sanjay-gandhi-national-park'
UNION ALL
SELECT d.id, 'pune' FROM destinations d WHERE d.slug = 'shaniwar-wada'
UNION ALL
SELECT d.id, 'fort' FROM destinations d WHERE d.slug = 'shaniwar-wada'
UNION ALL
SELECT d.id, 'heritage' FROM destinations d WHERE d.slug = 'shaniwar-wada'
UNION ALL
SELECT d.id, 'meghalaya' FROM destinations d WHERE d.slug = 'shillong'
UNION ALL
SELECT d.id, 'hills' FROM destinations d WHERE d.slug = 'shillong'
UNION ALL
SELECT d.id, 'capital' FROM destinations d WHERE d.slug = 'shillong'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'shore-temple'
UNION ALL
SELECT d.id, 'temple' FROM destinations d WHERE d.slug = 'shore-temple'
UNION ALL
SELECT d.id, 'coast' FROM destinations d WHERE d.slug = 'shore-temple'
UNION ALL
SELECT d.id, 'adventure' FROM destinations d WHERE d.slug = 'solang-valley'
UNION ALL
SELECT d.id, 'snow' FROM destinations d WHERE d.slug = 'solang-valley'
UNION ALL
SELECT d.id, 'manali' FROM destinations d WHERE d.slug = 'solang-valley'
UNION ALL
SELECT d.id, 'meadow' FROM destinations d WHERE d.slug = 'sonamarg'
UNION ALL
SELECT d.id, 'kashmir' FROM destinations d WHERE d.slug = 'sonamarg'
UNION ALL
SELECT d.id, 'valley' FROM destinations d WHERE d.slug = 'sonamarg'
UNION ALL
SELECT d.id, 'statue' FROM destinations d WHERE d.slug = 'statue-of-unity'
UNION ALL
SELECT d.id, 'narmada' FROM destinations d WHERE d.slug = 'statue-of-unity'
UNION ALL
SELECT d.id, 'viewpoint' FROM destinations d WHERE d.slug = 'statue-of-unity'
UNION ALL
SELECT d.id, 'mangrove' FROM destinations d WHERE d.slug = 'sundarbans-national-park'
UNION ALL
SELECT d.id, 'tiger' FROM destinations d WHERE d.slug = 'sundarbans-national-park'
UNION ALL
SELECT d.id, 'delta' FROM destinations d WHERE d.slug = 'sundarbans-national-park'
UNION ALL
SELECT d.id, 'tiger' FROM destinations d WHERE d.slug = 'tadoba-andhari-tiger-reserve'
UNION ALL
SELECT d.id, 'safari' FROM destinations d WHERE d.slug = 'tadoba-andhari-tiger-reserve'
UNION ALL
SELECT d.id, 'maharashtra' FROM destinations d WHERE d.slug = 'tadoba-andhari-tiger-reserve'
UNION ALL
SELECT d.id, 'chola' FROM destinations d WHERE d.slug = 'thanjavur'
UNION ALL
SELECT d.id, 'temples' FROM destinations d WHERE d.slug = 'thanjavur'
UNION ALL
SELECT d.id, 'tamil-nadu' FROM destinations d WHERE d.slug = 'thanjavur'
UNION ALL
SELECT d.id, 'monastery' FROM destinations d WHERE d.slug = 'thikse-monastery'
UNION ALL
SELECT d.id, 'ladakh' FROM destinations d WHERE d.slug = 'thikse-monastery'
UNION ALL
SELECT d.id, 'views' FROM destinations d WHERE d.slug = 'thikse-monastery'
UNION ALL
SELECT d.id, 'pilgrimage' FROM destinations d WHERE d.slug = 'tirupati'
UNION ALL
SELECT d.id, 'andhra' FROM destinations d WHERE d.slug = 'tirupati'
UNION ALL
SELECT d.id, 'temple-town' FROM destinations d WHERE d.slug = 'tirupati'
UNION ALL
SELECT d.id, 'lake' FROM destinations d WHERE d.slug = 'tso-moriri'
UNION ALL
SELECT d.id, 'ladakh' FROM destinations d WHERE d.slug = 'tso-moriri'
UNION ALL
SELECT d.id, 'remote' FROM destinations d WHERE d.slug = 'tso-moriri'
UNION ALL
SELECT d.id, 'pilgrimage' FROM destinations d WHERE d.slug = 'ujjain'
UNION ALL
SELECT d.id, 'malwa' FROM destinations d WHERE d.slug = 'ujjain'
UNION ALL
SELECT d.id, 'temple-city' FROM destinations d WHERE d.slug = 'ujjain'
UNION ALL
SELECT d.id, 'pilgrimage' FROM destinations d WHERE d.slug = 'vaishno-devi-temple'
UNION ALL
SELECT d.id, 'jammu' FROM destinations d WHERE d.slug = 'vaishno-devi-temple'
UNION ALL
SELECT d.id, 'trek' FROM destinations d WHERE d.slug = 'vaishno-devi-temple'
UNION ALL
SELECT d.id, 'unesco' FROM destinations d WHERE d.slug = 'valley-of-flowers-national-park'
UNION ALL
SELECT d.id, 'alpine' FROM destinations d WHERE d.slug = 'valley-of-flowers-national-park'
UNION ALL
SELECT d.id, 'seasonal' FROM destinations d WHERE d.slug = 'valley-of-flowers-national-park'
UNION ALL
SELECT d.id, 'beach' FROM destinations d WHERE d.slug = 'varkala-cliff-beach'
UNION ALL
SELECT d.id, 'cliff' FROM destinations d WHERE d.slug = 'varkala-cliff-beach'
UNION ALL
SELECT d.id, 'kerala' FROM destinations d WHERE d.slug = 'varkala-cliff-beach'
UNION ALL
SELECT d.id, 'kolkata' FROM destinations d WHERE d.slug = 'victoria-memorial'
UNION ALL
SELECT d.id, 'museum' FROM destinations d WHERE d.slug = 'victoria-memorial'
UNION ALL
SELECT d.id, 'colonial' FROM destinations d WHERE d.slug = 'victoria-memorial';

