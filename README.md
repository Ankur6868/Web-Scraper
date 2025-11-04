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

## Build Instructions

```bash
# Compile the scraper
javac -cp "lib/*" -d classes src/AlaskaSenateScraper.java

# Run the scraper
java -cp "classes;lib/*" AlaskaSenateScraper
```

Note: Use `classes:lib/*` instead of `classes;lib/*` on Unix-like systems.

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
3. Add district map information
4. Include committee memberships
5. Add caching for repeated runs

