$ErrorActionPreference = 'Stop'
$csv = Import-Csv "C:\Drive D\java\my projects\pdcm\docs\data\wikidata-verified-candidates.csv"

function Slug([string]$name) {
  ($name.ToLower() -replace '[^a-z0-9]+','-' -replace '^-|-$','').Substring(0, [Math]::Min(200, ($name.ToLower() -replace '[^a-z0-9]+','-' -replace '^-|-$','').Length))
}

function Esc([string]$s) { $s.Replace("'", "''") }

# region|city|category|sensitivity|displayName|description|tag1,tag2,tag3
$meta = @{
  'Q2344976'='Maharashtra|Pune|HISTORICAL|MEDIUM|Aga Khan Palace|Historic palace linked to independence-era events on the edge of Pune.|pune,heritage,palace'
  'Q184427'='Maharashtra|Aurangabad|HISTORICAL|MEDIUM|Ajanta Caves|Rock-cut Buddhist caves known for murals in the Deccan hills.|unesco,caves,heritage'
  'Q455167'='Jammu and Kashmir|Pahalgam|TEMPLE|HIGH|Amarnath Temple|High-altitude cave shrine that draws seasonal pilgrims in Kashmir.|pilgrimage,himalaya,seasonal'
  'Q665110'='Karnataka|Bandipur|WILDLIFE|LOW|Bandipur National Park|Tiger reserve and dry forest on the Karnataka border.|safari,tiger,nature'
  'Q3519346'='Kerala|Bekal|HISTORICAL|MEDIUM|Bekal Fort|Seaside fort overlooking the Arabian Sea in northern Kerala.|fort,seaside,kerala'
  'Q816234'='West Bengal|Kolkata|TEMPLE|MEDIUM|Belur Math|Ramakrishna Math campus on the Hooghly riverfront.|spiritual,kolkata,campus'
  'Q171771'='Odisha|Bhubaneswar|CITY|MEDIUM|Bhubaneswar Old Town Temples|Temple cluster around Lingaraja and related shrines in Bhubaneswar.|temples,odisha,heritage'
  'Q200718'='Rajasthan|Bikaner|CITY|MEDIUM|Bikaner Old City|Desert city core known for markets and approaches to Junagarh Fort.|desert,rajasthan,markets'
  'Q176767'='Bihar|Bodh Gaya|TEMPLE|HIGH|Bodh Gaya|Pilgrimage town centered on the Mahabodhi complex.|pilgrimage,unesco,bihar'
  'Q1771696'='Telangana|Hyderabad|HISTORICAL|HIGH|Charminar|Four-minaret monument and bazaar landmark in old Hyderabad.|hyderabad,monument,bazaar'
  'Q235884'='Maharashtra|Mumbai|HISTORICAL|HIGH|Chhatrapati Shivaji Maharaj Terminus|Victorian Gothic railway terminus and Mumbai landmark.|unesco,mumbai,railway'
  'Q2047322'='Odisha|Satapada|NATURE|LOW|Chilika Lake|Coastal lagoon known for birdlife and island boat routes.|lagoon,birds,odisha'
  'Q2723521'='Rajasthan|Udaipur|HISTORICAL|HIGH|City Palace Udaipur|Lakeside palace complex overlooking Lake Pichola.|udaipur,palace,lake'
  'Q325391'='Jammu and Kashmir|Srinagar|NATURE|MEDIUM|Dal Lake|Urban lake in Srinagar known for houseboats and gardens.|srinagar,lake,houseboats'
  'Q253455'='Uttar Pradesh|Lakhimpur Kheri|WILDLIFE|LOW|Dudhwa National Park|Terai forest reserve with grasslands in northern Uttar Pradesh.|tiger,terai,safari'
  'Q649384'='Maharashtra|Mumbai|HISTORICAL|MEDIUM|Elephanta Caves|Island rock-cut cave temples in Mumbai Harbour.|unesco,caves,mumbai'
  'Q189616'='Maharashtra|Aurangabad|HISTORICAL|MEDIUM|Ellora Caves|Rock-cut temples and monasteries carved into Deccan cliffs.|unesco,caves,heritage'
  'Q179046'='Uttar Pradesh|Fatehpur Sikri|HISTORICAL|MEDIUM|Fatehpur Sikri|Planned Mughal capital complex near Agra.|unesco,mughal,agra'
  'Q186141'='Sikkim|Gangtok|HILL_STATION|MEDIUM|Gangtok|Hill capital of Sikkim with ridge views and monastery roads.|sikkim,hills,capital'
  'Q337028'='Gujarat|Sasan Gir|WILDLIFE|LOW|Gir National Park|Forest sanctuary known for the Asiatic lion.|lion,safari,gujarat'
  'Q1025271'='Karnataka|Gokarna|BEACH|MEDIUM|Gokarna|Temple town and beach stretch on the Karnataka coast.|beach,temple-town,karnataka'
  'Q1025272'='Jammu and Kashmir|Gulmarg|HILL_STATION|HIGH|Gulmarg|Meadow resort known for skiing and gondola views.|ski,meadow,kashmir'
  'Q273030'='Uttarakhand|Haridwar|TEMPLE|HIGH|Haridwar|Ganges pilgrimage city known for riverfront aarti.|pilgrimage,ganges,uttarakhand'
  'Q1604804'='Ladakh|Hemis|WILDLIFE|LOW|Hemis National Park|High-altitude park protecting snow leopard habitat in Ladakh.|snow-leopard,ladakh,wildlife'
  'Q739641'='West Bengal|Kolkata|CITY|HIGH|Howrah Bridge|Cantilever bridge spanning the Hooghly between Kolkata and Howrah.|kolkata,bridge,icon'
  'Q3634368'='Rajasthan|Jaisalmer|HISTORICAL|HIGH|Jaisalmer Fort|Living fort of golden sandstone above the Thar Desert town.|fort,desert,unesco'
  'Q508634'='Rajasthan|Jaipur|HISTORICAL|MEDIUM|Jantar Mantar Jaipur|Open-air astronomical observatory instruments in Jaipur.|unesco,observatory,jaipur'
  'Q949297'='Uttarakhand|Ramnagar|WILDLIFE|MEDIUM|Jim Corbett National Park|Hill forest tiger reserve in the foothills of Uttarakhand.|tiger,safari,foothills'
  'Q200019'='Rajasthan|Jodhpur|CITY|MEDIUM|Jodhpur Blue City Views|Desert city known for blue-washed lanes below Mehrangarh.|jodhpur,desert,cityscape'
  'Q2758341'='Rajasthan|Bikaner|HISTORICAL|MEDIUM|Junagarh Fort|Red sandstone fort complex in the center of Bikaner.|fort,bikaner,palace'
  'Q1356198'='Maharashtra|Mumbai|HISTORICAL|LOW|Kanheri Caves|Buddhist rock-cut caves inside Sanjay Gandhi National Park.|caves,mumbai,forest'
  'Q222267'='Tamil Nadu|Kanyakumari|NATURE|HIGH|Kanyakumari|Southern tip of mainland India with coastal temples and sunrise views.|coast,tamil-nadu,viewpoint'
  'Q235878'='Rajasthan|Bharatpur|WILDLIFE|MEDIUM|Keoladeo National Park|Wetland bird sanctuary near Bharatpur.|birds,wetland,unesco'
  'Q167298'='Madhya Pradesh|Khajuraho|HISTORICAL|MEDIUM|Khajuraho Temples|Group of medieval temples known for sculpted sandstone walls.|unesco,temples,heritage'
  'Q253851'='Tamil Nadu|Kodaikanal|HILL_STATION|MEDIUM|Kodaikanal|Lake-centered hill station in the Palani Hills.|lake,hills,tamil-nadu'
  'Q752091'='Odisha|Konark|TEMPLE|MEDIUM|Konark Sun Temple|Thirteenth-century temple designed as a stone chariot.|unesco,temple,odisha'
  'Q1246283'='Rajasthan|Kumbhalgarh|HISTORICAL|MEDIUM|Kumbhalgarh Fort|Hill fort with a long continuous wall in the Aravallis.|fort,unesco,rajasthan'
  'Q3043543'='Ladakh|Leh|HISTORICAL|MEDIUM|Leh Palace|Nine-storey royal palace overlooking the Indus valley town of Leh.|ladakh,palace,views'
  'Q1929511'='Maharashtra|Lonavala|HILL_STATION|HIGH|Lonavala|Western Ghats hill town popular for monsoon viewpoints.|ghats,weekend,maharashtra'
  'Q940843'='Delhi|New Delhi|TEMPLE|MEDIUM|Lotus Temple|Bahai House of Worship known for its lotus-shaped dome.|delhi,architecture,quiet-zone'
  'Q463273'='Maharashtra|Mahabaleshwar|HILL_STATION|MEDIUM|Mahabaleshwar|Plateau hill station with strawberry farms and valley viewpoints.|views,ghats,weekend'
  'Q985828'='Maharashtra|Mumbai|CITY|HIGH|Marine Drive|Crescent seaside promenade along the Back Bay in south Mumbai.|mumbai,promenade,evening'
  'Q1483099'='Rajasthan|Jodhpur|HISTORICAL|HIGH|Mehrangarh Fort|Massive fort rising above the blue lanes of Jodhpur.|fort,jodhpur,views'
  'Q1025124'='Kerala|Munnar|HILL_STATION|MEDIUM|Munnar|Tea-estate hill town in the Western Ghats of Kerala.|tea,hills,kerala'
  'Q668395'='Uttarakhand|Mussoorie|HILL_STATION|HIGH|Mussoorie|Ridge hill station above Dehradun with colonial-era promenades.|hills,views,uttarakhand'
  'Q2723401'='Rajasthan|Jaipur|HISTORICAL|MEDIUM|Nahargarh Fort|Hill fort overlooking Jaipur with sunset city views.|fort,jaipur,views'
  'Q773213'='Uttarakhand|Nainital|HILL_STATION|HIGH|Nainital|Lake basin hill station in the Kumaon hills.|lake,hills,uttarakhand'
  'Q9888'='Tamil Nadu|Udhagamandalam|HILL_STATION|HIGH|Ooty|Nilgiri hill station known for botanical gardens and toy-train approaches.|nilgiris,hills,tamil-nadu'
  'Q2085326'='Jammu and Kashmir|Pahalgam|HILL_STATION|MEDIUM|Pahalgam|Lidder valley town used as a base for highland walks.|valley,kashmir,base-town'
  'Q1032254'='Ladakh|Pangong|NATURE|MEDIUM|Pangong Tso|High-altitude lake straddling the Ladakh landscape.|lake,ladakh,scenic'
  'Q817250'='Karnataka|Pattadakal|HISTORICAL|LOW|Pattadakal Temples|Chalukya temple ensemble on the Malaprabha river.|unesco,temples,karnataka'
  'Q2720864'='Madhya Pradesh|Seoni|WILDLIFE|LOW|Pench National Park|Tiger reserve spanning forest tracts of Madhya Pradesh and Maharashtra.|tiger,safari,forest'
  'Q548153'='Kerala|Thekkady|WILDLIFE|MEDIUM|Periyar National Park|Wildlife reserve around a reservoir in the Cardamom Hills.|elephant,boat,kerala'
  'Q916943'='Tamil Nadu|Thanjavur|TEMPLE|MEDIUM|Brihadeeswarar Temple|Chola-era temple with a towering sanctum in Thanjavur.|unesco,chola,temple'
  'Q749170'='Rajasthan|Pushkar|TEMPLE|HIGH|Pushkar|Pilgrimage town around a sacred lake and Brahma temple.|pilgrimage,lake,rajasthan'
  'Q187635'='Delhi|New Delhi|HISTORICAL|HIGH|Qutb Minar|Towering minaret and surrounding ruins in Mehrauli.|unesco,delhi,minaret'
  'Q863683'='Bihar|Rajgir|HISTORICAL|MEDIUM|Rajgir|Ancient hills and hot springs linked to early Buddhist and Jain sites.|bihar,hills,heritage'
  'Q1466242'='Rajasthan|Sawai Madhopur|WILDLIFE|MEDIUM|Ranthambore National Park|Dry forest tiger reserve around lakes and fort ruins.|tiger,safari,rajasthan'
  'Q502141'='Uttarakhand|Rishikesh|TEMPLE|HIGH|Rishikesh|Ganges town known for ashrams, bridges, and river adventure sports.|ganges,yoga,adventure'
  'Q633415'='Himachal Pradesh|Manali|HILL_STATION|HIGH|Rohtang Pass|High mountain pass north of Manali open seasonally.|pass,snow,seasonal'
  'Q1410702'='Gujarat|Ahmedabad|HISTORICAL|MEDIUM|Sabarmati Ashram|Riverside ashram associated with Mahatma Gandhi in Ahmedabad.|gandhi,ashram,ahmedabad'
  'Q181123'='Madhya Pradesh|Sanchi|HISTORICAL|MEDIUM|Sanchi Stupa|Hilltop Buddhist stupas and gateways near Bhopal.|unesco,buddhist,heritage'
  'Q648907'='Maharashtra|Mumbai|NATURE|MEDIUM|Sanjay Gandhi National Park|Urban national park with forests and caves inside Mumbai.|forest,mumbai,day-trip'
  'Q1351060'='Maharashtra|Pune|HISTORICAL|MEDIUM|Shaniwar Wada|Fortified palace ruins in the old city of Pune.|pune,fort,heritage'
  'Q207752'='Meghalaya|Shillong|HILL_STATION|MEDIUM|Shillong|Hill capital of Meghalaya known for ridges and nearby waterfalls.|meghalaya,hills,capital'
  'Q1144279'='Tamil Nadu|Mahabalipuram|HISTORICAL|MEDIUM|Shore Temple|Seaside stone temple on the Coromandel Coast.|unesco,temple,coast'
  'Q12069422'='Himachal Pradesh|Manali|HILL_STATION|HIGH|Solang Valley|Adventure valley near Manali used for snow and cable activities.|adventure,snow,manali'
  'Q4379433'='Jammu and Kashmir|Sonamarg|HILL_STATION|MEDIUM|Sonamarg|Meadow valley on the road toward Zoji La.|meadow,kashmir,valley'
  'Q7604504'='Gujarat|Kevadia|HISTORICAL|HIGH|Statue of Unity|Tall statue complex beside the Narmada near Kevadia.|statue,narmada,viewpoint'
  'Q532440'='West Bengal|Gosaba|WILDLIFE|LOW|Sundarbans National Park|Mangrove tiger landscape in the Ganges delta.|mangrove,tiger,delta'
  'Q2639563'='Maharashtra|Chandrapur|WILDLIFE|LOW|Tadoba Andhari Tiger Reserve|Central Indian tiger reserve with lakes and teak forest.|tiger,safari,maharashtra'
  'Q41496'='Tamil Nadu|Thanjavur|CITY|MEDIUM|Thanjavur|Historic Chola city known for temples and bronze craft.|chola,temples,tamil-nadu'
  'Q867066'='Ladakh|Thiksey|TEMPLE|MEDIUM|Thikse Monastery|Terraced Buddhist monastery overlooking the Indus valley.|monastery,ladakh,views'
  'Q1436816'='Andhra Pradesh|Tirupati|TEMPLE|HIGH|Tirupati|Pilgrimage city at the foot of the Tirumala hills.|pilgrimage,andhra,temple-town'
  'Q2087758'='Ladakh|Korzok|NATURE|LOW|Tso Moriri|High-altitude lake in Changthang with sparse shoreline settlements.|lake,ladakh,remote'
  'Q200119'='Madhya Pradesh|Ujjain|TEMPLE|HIGH|Ujjain|Ancient city on the Shipra known for the Mahakaleshwar temple.|pilgrimage,malwa,temple-city'
  'Q611371'='Jammu and Kashmir|Katra|TEMPLE|HIGH|Vaishno Devi Temple|Hill shrine reached by trek or pony routes from Katra.|pilgrimage,jammu,trek'
  'Q605897'='Uttarakhand|Joshimath|NATURE|LOW|Valley of Flowers National Park|Alpine flower meadows in the Garhwal Himalaya.|unesco,alpine,seasonal'
  'Q1012951'='Kerala|Varkala|BEACH|MEDIUM|Varkala Cliff Beach|Cliff-backed beach and temple area on the Kerala coast.|beach,cliff,kerala'
  'Q1356352'='West Bengal|Kolkata|HISTORICAL|HIGH|Victoria Memorial|Marble museum-memorial set in gardens in central Kolkata.|kolkata,museum,colonial'
}

$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine("-- V6: city + provenance columns, legacy backfill, ~80 Wikidata-verified destinations.")
[void]$sb.AppendLine("-- Structured coordinates/names from Wikidata (CC0). Descriptions are original PDCM wording.")
[void]$sb.AppendLine("-- No visit_reports are inserted.")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("ALTER TABLE destinations")
[void]$sb.AppendLine("    ADD COLUMN city VARCHAR(120),")
[void]$sb.AppendLine("    ADD COLUMN source_reference VARCHAR(120),")
[void]$sb.AppendLine("    ADD COLUMN source_url VARCHAR(500),")
[void]$sb.AppendLine("    ADD COLUMN source_license VARCHAR(80);")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("CREATE INDEX idx_destinations_city ON destinations (city);")
[void]$sb.AppendLine("CREATE INDEX idx_destinations_lat_lon ON destinations (latitude, longitude);")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("-- Legacy Day-1 seeds: provenance unknown; city filled from known place context.")
[void]$sb.AppendLine("UPDATE destinations SET city = CASE slug")
[void]$sb.AppendLine("    WHEN 'taj-mahal' THEN 'Agra'")
[void]$sb.AppendLine("    WHEN 'gateway-of-india' THEN 'Mumbai'")
[void]$sb.AppendLine("    WHEN 'india-gate' THEN 'New Delhi'")
[void]$sb.AppendLine("    WHEN 'red-fort' THEN 'New Delhi'")
[void]$sb.AppendLine("    WHEN 'jaipur-city-palace' THEN 'Jaipur'")
[void]$sb.AppendLine("    WHEN 'hawa-mahal' THEN 'Jaipur'")
[void]$sb.AppendLine("    WHEN 'amber-fort' THEN 'Jaipur'")
[void]$sb.AppendLine("    WHEN 'varanasi-ghats' THEN 'Varanasi'")
[void]$sb.AppendLine("    WHEN 'golden-temple' THEN 'Amritsar'")
[void]$sb.AppendLine("    WHEN 'meenakshi-temple' THEN 'Madurai'")
[void]$sb.AppendLine("    WHEN 'goa-calangute-beach' THEN 'Calangute'")
[void]$sb.AppendLine("    WHEN 'kovalam-beach' THEN 'Kovalam'")
[void]$sb.AppendLine("    WHEN 'manali-mall-road' THEN 'Manali'")
[void]$sb.AppendLine("    WHEN 'shimla-ridge' THEN 'Shimla'")
[void]$sb.AppendLine("    WHEN 'darjeeling-mall' THEN 'Darjeeling'")
[void]$sb.AppendLine("    WHEN 'mysore-palace' THEN 'Mysuru'")
[void]$sb.AppendLine("    WHEN 'hampi-virupaksha-temple' THEN 'Hampi'")
[void]$sb.AppendLine("    WHEN 'kaziranga-national-park' THEN 'Kohora'")
[void]$sb.AppendLine("    WHEN 'cherai-beach' THEN 'Cherai'")
[void]$sb.AppendLine("    WHEN 'pondicherry-promenade' THEN 'Puducherry'")
[void]$sb.AppendLine("    ELSE city END,")
[void]$sb.AppendLine("    source_reference = 'LEGACY_SEED',")
[void]$sb.AppendLine("    source_url = NULL,")
[void]$sb.AppendLine("    source_license = 'UNKNOWN'")
[void]$sb.AppendLine("WHERE source_reference IS NULL;")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("INSERT INTO destinations (name, slug, description, region, city, category, latitude, longitude, crowd_sensitivity, active, source_reference, source_url, source_license) VALUES")

$values = @()
$tagRows = @()
foreach ($row in $csv) {
  $m = $meta[$row.Q]
  if (-not $m) { throw "Missing meta for $($row.Q)" }
  $parts = $m -split '\|', 7
  $region,$city,$cat,$sens,$name,$desc,$tagCsv = $parts
  $slug = Slug $name
  $url = "https://www.wikidata.org/wiki/$($row.Q)"
  $values += "('$(Esc $name)', '$(Esc $slug)', '$(Esc $desc)', '$(Esc $region)', '$(Esc $city)', '$cat', $($row.Lat), $($row.Lon), '$sens', TRUE, 'Wikidata $($row.Q)', '$url', 'CC0')"
  foreach ($tag in ($tagCsv -split ',')) {
    $tagRows += "SELECT d.id, '$(Esc $tag.Trim())' FROM destinations d WHERE d.slug = '$(Esc $slug)'"
  }
}

[void]$sb.AppendLine(($values -join ",`n") + ";")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("INSERT INTO destination_tags (destination_id, tag)")
[void]$sb.AppendLine(($tagRows -join "`nUNION ALL`n") + ";")

$out = "C:\Drive D\java\my projects\pdcm\backend\src\main\resources\db\migration\V6__destinations_city_provenance_and_catalogue.sql"
Set-Content -Path $out -Value $sb.ToString() -Encoding UTF8
Write-Output "WROTE $out"
Write-Output ("DESTINATIONS=" + $values.Count)
Write-Output ("TAGS=" + $tagRows.Count)
