package com.gearshiftgaming.se_mod_manager.backend.data.steam;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SimpleSteamLibraryFoldersVdfParser {
    private int lineIndex = 0;

    public Map<String, Object> parseVdf(String filePath) throws IOException {
        if (Files.notExists(Path.of(filePath)))
            throw new FileNotFoundException();

        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }
        }

        return parseBlock(lines);
    }


    /**
     * Recursively parses a block of lines representing a nested key-value structure,
     * such as Valve's VDF (Valve Data Format), starting from the current {@code lineIndex}.
     * <p>
     * This method expects each block to begin with a key followed by an opening curly brace {@code { },
     * and to end with a closing curly brace {@code } }. Keys may either map directly to string values,
     * or to nested blocks. It populates and returns a {@link HashMap} representing the parsed structure.
     * <p>
     * This method modifies and relies on a shared {@code lineIndex} field that tracks the current
     * position in the list of lines across recursive calls.
     *
     * @param lines The list of lines to parse from a VDF file.
     * @return A map representing the parsed structure from the current block.
     */
    private HashMap<String, Object> parseBlock(List<String> lines) {
        HashMap<String, Object> map = new HashMap<>();

        while (lineIndex < lines.size()) {
            String line = lines.get(lineIndex++).trim();

            //If our line is blank we want to just check the next line, but if it is a right curly brace exit the block.
            if (line.isEmpty()) continue;
            if (line.equals("}")) return map;

            String[] tokens = extractQuotedStrings(line);

            if (tokens.length != 1 && tokens.length != 2)
                throw new IllegalStateException("Unexpected token count (" + tokens.length + ") in line: " + line);

            //Handle the actual block
            if(tokens.length == 1) {
                String key = tokens[0];
                if (lineIndex >= lines.size() || !lines.get(lineIndex).equals("{"))
                    throw new IllegalStateException("Expected '{' after key: " + key);

                lineIndex++; //Consume our left curly brace
                map.put(key, parseBlock(lines));
            } else
                map.put(tokens[0], tokens[1]);
        }
        return map;
    }

    private String[] extractQuotedStrings(String line) {
        List<String> parts = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        //This gets our words between double quotes by checking if we're already in a quote at a given character, and if not, advancing until we are.
        // Each character between double quotes is added to a string builder as we go, and we check every character in the string.
        for (char c : line.toCharArray()) {
            if (c == '"') {
                if (inQuotes) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                inQuotes = !inQuotes;
            } else if (inQuotes) {
                current.append(c);
            }
        }

        return parts.toArray(new String[0]);
    }
}
