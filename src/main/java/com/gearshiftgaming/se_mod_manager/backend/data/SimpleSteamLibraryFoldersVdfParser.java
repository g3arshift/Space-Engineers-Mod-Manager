package com.gearshiftgaming.se_mod_manager.backend.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class SimpleSteamLibraryFoldersVdfParser {
    private int lineIndex = 0;

    public HashMap<String, Object> parseVdf(String filePath) throws IOException {
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

    private HashMap<String, Object> parseBlock(List<String> lines) {
        HashMap<String, Object> map = new HashMap<>();

        //This will always skip our first line, since that should just be the word "libraryfolders"
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
                continue;
            }

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
