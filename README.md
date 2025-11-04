# Alaska Senate Scraper

A Java-based web scraper to extract information about Alaska State Senators from the [Alaska State Legislature website](https://akleg.gov/senate.php).

## Project Timeline

1. Initial Setup & Research (1 hour)
   - Project structure setup
   - Dependency identification
   - Target website analysis
   - API/structure investigation

2. Development Phase (2.5 hours)
   - Initial implementation with Selenium (1 hour)
   - Switch to HTTP-based approach (30 mins)
   - HTML parsing and data extraction (1 hour)

3. Testing & Refinement (1.5 hours)
   - Pattern refinement
   - JSON output formatting
   - Error handling improvements
   - Duplicate removal fixes

Total Development Time: ~5 hours

## Project Structure

```
Scraper/
├── src/
│   └── AlaskaSenateScraper.java
├── lib/
│   └── (JSON libraries)
├── classes/
│   └── (Compiled .class files)
├── alaska_senate_template.json
├── alaska_senate.json
└── README.md
```

## Features

- Extracts senator information including:
  - Name
  - Title
  - Party Affiliation
  - District
  - City
  - Contact Information (Phone)
  - Profile URL

## Dependencies

The project uses:
- Java HTTP Client (built-in)
- JSON library (in lib/ directory)

## Build & Run Instructions

```powershell
# Compile the scraper
javac -cp "lib/*" -d classes src/AlaskaSenateScraper.java

# Run the scraper for the Senate (default)
java -cp "classes;lib/*" AlaskaSenateScraper

# Run the scraper for the House
java -cp "classes;lib/*" AlaskaSenateScraper --chamber=house

# Short form: pass `house` or `senate` as the first arg
java -cp "classes;lib/*" AlaskaSenateScraper house
```

Notes:
- On Unix/macOS use `:` as the classpath separator: `java -cp "classes:lib/*" ...`
- The scraper writes `alaska_senate.json` or `alaska_house.json` depending on the chamber selected.

## Output

The scraper generates a JSON file (`alaska_senate.json`) containing an array of senator objects with the following structure:

```json
{
    "name": "Senator Name",
    "title": "Senator",
    "party": "Party Affiliation",
    "profile": "Profile URL",
    "otherinfo": "City: City Name | District: Letter | Phone: Number"
}
```

## Technical Notes

1. Initially attempted using Selenium but switched to pure HTTP requests for better performance and reliability
2. Uses regex patterns for HTML parsing instead of a DOM parser for simplicity
3. Handles different URL formats (relative, absolute, protocol-relative)
4. Includes error handling for network issues and parsing failures
5. Deduplicates senator entries

## Challenges Overcome

1. HTML structure variations in the senate page
2. URL normalization across different formats
3. Duplicate entry handling
4. Contact information parsing and formatting

## Future Improvements

1. Add email extraction (currently protected by Cloudflare)
2. Implement proper HTML parsing using JSoup
3. Add a command-line flag to control output format (CSV/JSON) and optional filters (party/district)

## Title detection & example outputs

How titles are detected
- Primary: the scraper looks for a leadership span inside each person listing: `<span class="position">...</span>` and uses that text as the JSON `title` (this preserves values like "Majority Leader", "Minority Leader", "Senate President", and "Speaker of the House").
- Fallbacks: if the `position` span isn't present, the scraper extracts title/role information from the nearby listing details (`<ul class="list-info">` or `<dl>...</dl>`), but it will default to `"Senator"` (or `"Representative"` if you adapt for the House page) when no leadership label appears.
- Example patterns the scraper recognizes:
   - `<span class="position">Majority Leader</span>`
   - `<span class="position">Minority Leader</span>`
   - `<span class="position">Senate President</span>`
   - `<span class="position">Speaker of the House</span>` (on the House listing)

Fields you can expect in the JSON for each person
- `name` — full name
- `title` — role/title detected (e.g., "Senator", "Majority Leader", "Speaker of the House")
- `type` — membership status (currently we set this to `"current"` for scraped members)
- `country` — set to `"Alaska"`
- `party` — party affiliation when available
- `profile` / `url` — normalized profile link taken from the anchor that wraps the member image/name
- `otherinfo` — compact string with City/District/Phone where available

Example (Senate) JSON entry

```json
{
   "name": "Cathy Giessel",
   "title": "Majority Leader",
   "type": "current",
   "country": "Alaska",
   "party": "Republican",
   "profile": "http://www.akleg.gov/basis/Member/Detail/34?code=gie",
   "otherinfo": "District: District E"
}
```

Example (House) JSON entry with Speaker

```json
{
   "name": "Bryce Edgmon",
   "title": "Speaker of the House",
   "type": "current",
   "country": "Alaska",
   "party": "Republican",
   "profile": "http://www.akleg.gov/basis/Member/Detail/34?code=edg",
   "otherinfo": "City: Anchorage | District: District 37"
}
```

Notes and gotchas
- The scraper uses regex-based parsing against the listing pages and is tuned to the current site structure. If the legislature site changes markup significantly, titles may require small updates to the detection patterns.
- For robust parsing and future maintenance I recommend switching to an HTML parser (JSoup) and using DOM queries to collect the `position` span and other fields explicitly.
3. Add district map information
4. Include committee memberships
5. Add caching for repeated runs

## License

This project is provided as-is for educational purposes.