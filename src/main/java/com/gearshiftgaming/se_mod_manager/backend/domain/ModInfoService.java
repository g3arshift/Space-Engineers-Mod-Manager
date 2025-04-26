package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This is the class containing all the logic responsible for retrieving mod information for mods and a modlist.
 * In particular, this class scrapes information when adding or updating mods.
 * <p>
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModInfoService {

    private final String STEAM_WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

    private final String MOD_IO_URL = "https://mod.io/search/mods/";

    private final ModlistRepository MODLIST_REPOSITORY;

    private final String STEAM_MOD_TYPE_SELECTOR;

    private final String STEAM_MOD_LAST_UPDATED_SELECTOR;

    private final String STEAM_MOD_FIRST_POSTED_SELECTOR;

    private final String STEAM_MOD_TAGS_SELECTOR;

    private final String STEAM_MOD_DESCRIPTION_SELECTOR;

    private final String STEAM_MOD_VERIFICATION_SELECTOR;

    private final Pattern STEAM_MOD_ID_PATTERN;

    private final String STEAM_COLLECTION_GAME_NAME_SELECTOR;

    private final String STEAM_COLLECTION_MOD_ID_SELECTOR;

    private final String STEAM_COLLECTION_VERIFICATION_SELECTOR;

    private final String MOD_IO_MOD_TYPE_SELECTOR;

    private final String MOD_IO_MOD_JSOUP_MOD_ID_SELECTOR;

    private final String MOD_IO_MOD_LAST_UPDATED_SELECTOR;

    private final String MOD_IO_MOD_TAGS_SELECTOR;

    private final String MOD_IO_MOD_DESCRIPTION_SELECTOR;

    private final int MOD_IO_SCRAPING_TIMEOUT;

    private final String MOD_IO_SCRAPING_WAIT_CONDITION_SELECTOR;

    //TODO: Download mods using steamCMD to the user directory. Have some sort of UI indication they're downloading in the UI.
    // Once downloaded, get modified paths and modify conflict table.
    public ModInfoService(ModlistRepository MODLIST_REPOSITORY, Properties PROPERTIES) {
        this.MODLIST_REPOSITORY = MODLIST_REPOSITORY;

        this.STEAM_MOD_TYPE_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.type.cssSelector");
        this.STEAM_MOD_LAST_UPDATED_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.lastUpdated.cssSelector");
        this.STEAM_MOD_FIRST_POSTED_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.firstPosted.cssSelector");
        this.STEAM_MOD_TAGS_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.tags.cssSelector");
        this.STEAM_MOD_DESCRIPTION_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.description.cssSelector");
        this.STEAM_MOD_VERIFICATION_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.workshopVerification.cssSelector");

        this.STEAM_MOD_ID_PATTERN = Pattern.compile(PROPERTIES.getProperty("semm.steam.mod.id.pattern"));

        this.STEAM_COLLECTION_GAME_NAME_SELECTOR = PROPERTIES.getProperty("semm.steam.collectionScraper.workshop.gameName.cssSelector");
        this.STEAM_COLLECTION_MOD_ID_SELECTOR = PROPERTIES.getProperty("semm.steam.collectionScraper.workshop.collectionContents.cssSelector");
        this.STEAM_COLLECTION_VERIFICATION_SELECTOR = PROPERTIES.getProperty("semm.steam.collectionScraper.workshop.collectionVerification.cssSelector");

        this.MOD_IO_MOD_TYPE_SELECTOR = PROPERTIES.getProperty("semm.modio.modScraper.type.cssSelector");
        this.MOD_IO_MOD_JSOUP_MOD_ID_SELECTOR = PROPERTIES.getProperty("semm.modio.modScraper.jsoup.modId.cssSelector");
        this.MOD_IO_MOD_LAST_UPDATED_SELECTOR = PROPERTIES.getProperty("semm.modio.modScraper.lastUpdated.cssSelector");
        this.MOD_IO_MOD_TAGS_SELECTOR = PROPERTIES.getProperty("semm.modio.modScraper.tags.cssSelector");
        this.MOD_IO_MOD_DESCRIPTION_SELECTOR = PROPERTIES.getProperty("semm.modio.modScraper.description.cssSelector");
        this.MOD_IO_SCRAPING_TIMEOUT = Integer.parseInt(PROPERTIES.getProperty("semm.modio.modScraper.timeout"));
        this.MOD_IO_SCRAPING_WAIT_CONDITION_SELECTOR = PROPERTIES.getProperty("semm.modIo.modScraper.waitCondition.cssSelector");
    }

    public List<String> getModIdsFromFile(File modlistFile, ModType modType) throws IOException {
        if (modType == ModType.STEAM) {
            return MODLIST_REPOSITORY.getSteamModList(modlistFile);
        } else {
            return MODLIST_REPOSITORY.getModIoModUrls(modlistFile);
        }
    }

    public List<Result<String>> scrapeSteamCollectionModIds(String collectionId) throws IOException {
        List<Result<String>> modIdScrapeResults = new ArrayList<>();

        Document collectionPage = Jsoup.connect(STEAM_WORKSHOP_URL + collectionId).get();

        String gameName = collectionPage.select(STEAM_COLLECTION_GAME_NAME_SELECTOR).getFirst().childNodes().getFirst().toString().trim();
        String foundBreadcrumbName = collectionPage.select(STEAM_COLLECTION_VERIFICATION_SELECTOR).getFirst().childNodes().getFirst().toString();
        if (!gameName.equals("Space Engineers")) { //Game name check
            Result<String> wrongGameResult = new Result<>();
            wrongGameResult.addMessage("The collection must be a Space Engineers collection!", ResultType.FAILED);
            modIdScrapeResults.add(wrongGameResult);
        } else if (!foundBreadcrumbName.equals("Collections")) { //Steam item check (makes sure it's a collection)
            Result<String> notACollectionResult = new Result<>();
            notACollectionResult.addMessage("You must provide a link or ID of a collection!", ResultType.FAILED);
            modIdScrapeResults.add(notACollectionResult);
        } else {
            Elements elements = collectionPage.select(STEAM_COLLECTION_MOD_ID_SELECTOR);
            List<Node> nodes = elements.getFirst().childNodes();

            if (nodes.get(1).attributes().get("class").equals("collectionNoChildren")) { //Empty collection check
                Result<String> emptyCollectionResult = new Result<>();
                emptyCollectionResult.addMessage("No items in this collection.", ResultType.FAILED);
                modIdScrapeResults.add(emptyCollectionResult);
            } else {
                for (Node node : nodes) {
                    Result<String> modIdResult = new Result<>();
                    if (node.hasAttr("data-panel")) { //All the nodes that have the actual info we need have this attribute
                        StringBuilder modId = new StringBuilder();
                        modId.append(STEAM_MOD_ID_PATTERN.matcher(node.childNodes().get(1).toString())
                                .results()
                                .map(MatchResult::group)
                                .collect(Collectors.joining()));

                        //We need to remove the id= portion of the string.
                        if (modId.length() <= 3) {
                            modIdResult.addMessage("Failed to grab mod ID.", ResultType.FAILED);
                        } else {
                            modId.replace(0, modId.length(), modId.substring(3));
                            modIdResult.addMessage("Successfully grabbed mod ID.", ResultType.SUCCESS);
                            modIdResult.setPayload(modId.toString());
                        }
                        modIdScrapeResults.add(modIdResult);
                    }
                }
            }
        }

        return modIdScrapeResults;
    }

    /**
     * Given a mod IO url, we get the actual ID of the mod. This is done by grabbing the resource ID contained within the URL of the mod primary image.
     * Images are required for Mod.io mods, and the URL displays even without the JS running, so this is a more efficient way to get the ID before the more costly
     * scraping process which opens a full headless, embedded web browser.
     */
    public Result<String> getModIoIdFromName(String name) throws IOException {
        Result<String> modIdResult = new Result<>();
        final String MOD_IO_NAME_URL = "https://mod.io/g/spaceengineers/m/";
        final Pattern MOD_ID_FROM_IMAGE_URL = Pattern.compile("(?<=/)\\d+(?=/)");

        Document doc = Jsoup.connect(MOD_IO_NAME_URL + name).get();

        List<String> matches = MOD_ID_FROM_IMAGE_URL.matcher(doc.select(MOD_IO_MOD_JSOUP_MOD_ID_SELECTOR).toString())
                .results()
                .map(MatchResult::group)
                .toList();
        if (matches.isEmpty()) {
            modIdResult.addMessage("Invalid Mod.io URL entered!", ResultType.INVALID);
            return modIdResult;
        }

        String modId = matches.getLast();
        if (!modId.isBlank()) {
            modIdResult.setPayload(modId);
            modIdResult.addMessage("Successfully scraped Mod.io Mod ID from URL.", ResultType.SUCCESS);
        }

        return modIdResult;
    }

    //Scrape the web pages of the mods we want the information from
    public Result<String[]> scrapeModInformation(Mod mod) throws InterruptedException {
        Result<String[]> modScrapeResult;
        if (mod instanceof SteamMod) {
            modScrapeResult = scrapeSteamMod(mod.getId());
        } else {
            modScrapeResult = scrapeModIoMod(mod.getId());
        }
        return modScrapeResult;
    }

    private Result<String[]> scrapeSteamMod(String modId) {
        Result<String[]> modScrapeResult = new Result<>();

        Document modPage;
        //Check to make sure the connection works first.
        try {
            // Needs a fix for mod.io too.
            modPage = Jsoup.connect(STEAM_WORKSHOP_URL + modId)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .get();
        } catch (IOException e) {
            modScrapeResult.addMessage(getStackTrace(e), ResultType.FAILED);
            modScrapeResult.addMessage("Failed to load mod page: " + STEAM_WORKSHOP_URL + modId, ResultType.FAILED);
            return modScrapeResult;
        }

        if (modPage.title().equals("Steam Community :: Error")) { //Makes sure it's a valid link at all
            modScrapeResult.addMessage("Mod with ID \"" + modId + "\" cannot be found.", ResultType.FAILED);
            return modScrapeResult;
        }

        String itemType = modPage.select(STEAM_MOD_VERIFICATION_SELECTOR)
                .stream()
                .findFirst()
                .flatMap(element -> element.childNodes().stream().findFirst())
                .map(Object::toString).orElse("");
        if (!itemType.equals("Workshop")) { //Makes sure it isn't something like a screenshot
            modScrapeResult.addMessage("Item with ID \"" + modId + "\" is not part of the workshop, it is part of " + itemType + ".", ResultType.FAILED);
            return modScrapeResult;
        }

        String workshopType = modPage.select(STEAM_COLLECTION_VERIFICATION_SELECTOR)
                .stream()
                .findFirst()
                .flatMap(element -> element.childNodes().stream().findFirst())
                .map(Object::toString).orElse("");
        if (workshopType.equals("Collections")) { //Makes sure it's not a collection
            modScrapeResult.addMessage("\"" + modPage.title().split("Steam Workshop::")[1] + "\" is a collection, not a mod!", ResultType.FAILED);
            return modScrapeResult;
        }

        String modName = modPage.title().contains("Workshop::") ? modPage.title().split("Workshop::")[1] : modPage.title();
        if (pageDoesNotContainMod(ModType.STEAM, modPage)) {
            if (!modPage.select(STEAM_MOD_TYPE_SELECTOR).isEmpty()) {
                modScrapeResult.addMessage("\"" + modPage.title().split("Workshop::")[1] + "\" is not a mod, it is a " +
                        modPage.select(STEAM_MOD_TYPE_SELECTOR).getFirst().childNodes().getFirst().toString() + ".", ResultType.FAILED);
            } else {
                modScrapeResult.addMessage("\"" + modPage.title().split("Workshop::")[1] + "\" is for either a workshop item that is not a mod, for the wrong game, or is not publicly available on the workshop.", ResultType.INVALID);
            }
            return modScrapeResult;
        }

        //The first item is mod name, second is a combined string of the tags, third is the raw HTML of the description, and fourth is last updated.
        String[] modInfo = new String[4];
        modInfo[0] = modName;

        Elements modTagElements = modPage.select(STEAM_MOD_TAGS_SELECTOR);
        Element modTagElement;
        if (!modTagElements.isEmpty()) {
            modTagElement = modPage.select(STEAM_MOD_TAGS_SELECTOR).getFirst();
        } else {
            modTagElement = null;
        }

        List<String> modTags = new ArrayList<>();
        StringBuilder concatenatedModTags = new StringBuilder();
        if (modTagElement != null) {
            for (int i = 1; i < modTagElement.childNodes().size(); i += 2) {
                modTags.add(modTagElement.childNodes().get(i).childNodes().getFirst().toString());
            }

            for (int i = 0; i < modTags.size(); i++) {
                if (i + 1 < modTags.size()) {
                    concatenatedModTags.append(modTags.get(i)).append(",");
                } else {
                    concatenatedModTags.append(modTags.get(i));
                }
            }
        } else {
            concatenatedModTags.append("None");
        }
        modInfo[1] = concatenatedModTags.toString();

        modInfo[2] = modPage.select(STEAM_MOD_DESCRIPTION_SELECTOR).getFirst().toString();

        String lastUpdated;
        if (modPage.select(STEAM_MOD_LAST_UPDATED_SELECTOR).isEmpty()) {
            lastUpdated = StringUtils.substringBetween(modPage.select(STEAM_MOD_FIRST_POSTED_SELECTOR).toString(),
                    "<div class=\"detailsStatRight\">\n ",
                    "\n</div>");
        } else {
            lastUpdated = StringUtils.substringBetween(modPage.select(STEAM_MOD_LAST_UPDATED_SELECTOR).toString(),
                    "<div class=\"detailsStatRight\">\n ",
                    "\n</div>");
        }

        //Append a year if we don't find one. This regex looks for any four contiguous digits.
        Pattern yearPattern = Pattern.compile("\\b\\d{4}\\b");
        if (!yearPattern.matcher(lastUpdated).find()) {
            String[] lastUpdatedParts = lastUpdated.split(" @ ");
            lastUpdatedParts[0] += ", " + Year.now();
            lastUpdated = lastUpdatedParts[0] + " @ " + lastUpdatedParts[1];
        }
        modInfo[3] = lastUpdated;

        modScrapeResult.addMessage("Successfully scraped information for mod " + modId + "!", ResultType.SUCCESS);
        modScrapeResult.setPayload(modInfo);

        return modScrapeResult;
    }

    private Result<String[]> scrapeModIoMod(String modId) throws InterruptedException {
        Result<String[]> modScrapeResult = new Result<>();
        //By this point we should have a valid ModIO ID to look up the mods by for the correct game. Need to verify tags and that it is a mod, however.
        try (Playwright scraper = Playwright.create(); Browser browser = scraper.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true).setChromiumSandbox(false))) {
            Page webPage = browser.newContext(new Browser.NewContextOptions()
                    .setLocale("en-US")
                    .setExtraHTTPHeaders(Map.of("Accept-Language", "en-US,en;q=0.9"))).newPage();
            String pageSource = fetchModIoPageContent(webPage, modId, modScrapeResult);

            if (!pageSource.isEmpty()) {
                parseModIoModInfo(pageSource, modId, modScrapeResult);
                if (modScrapeResult.getPayload() != null) {
                    modScrapeResult.addMessage("Successfully scraped information for mod " + modId + "!", ResultType.SUCCESS);
                }
            }
        } catch (Exception e) {
            modScrapeResult.addMessage(getStackTrace(e), ResultType.FAILED);
            modScrapeResult.addMessage(String.format("Failed to scrape mod information from Mod.io for mod \"%s\". Please see the log for more information.", modId), ResultType.FAILED);
        }
        return modScrapeResult;
    }

    private String fetchModIoPageContent(Page webPage, String modId, Result<String[]> scrapeResult) throws InterruptedException {
        int retries = 0;
        final int MAX_RETRIES = 3;
        int delay = 1000;
        Random random = new Random();
        String pageSource = "";

        webPage.navigate(MOD_IO_URL + modId);
        while (retries < MAX_RETRIES && StringUtils.countMatches(pageSource, "\n") < 1) {
            try {
                String jsonText = StringUtils.substringBetween(webPage.content(), "<pre>", "</pre>");
                if (jsonText != null) {
                    JsonObject response = new Gson().fromJson(jsonText, JsonObject.class);
                    if (response != null) {
                        JsonElement element = response.get("error");
                        String errorCode = element.getAsJsonObject().get("code").toString();
                        switch (errorCode) {
                            case "404" ->
                                    throw new ModNotFoundException("Mod with ID \"" + modId + "\" cannot be found.");
                            case "429" -> throw new RateLimitException("Mod.io is rate limiting you.");
                            default -> throw new RuntimeException("Unknown Mod.io error: " + errorCode);
                        }
                    } else
                        //We shouldn't ever really reach this because it is a scenario where we SOMEHOW are encountering an error, but it's not getting us a value from the webpage.
                        retries++;
                }
                webPage.waitForSelector(MOD_IO_SCRAPING_WAIT_CONDITION_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(MOD_IO_SCRAPING_TIMEOUT));
                pageSource = webPage.content();
            } catch (RateLimitException e) {
                retries++;
                if (retries < MAX_RETRIES) {
                    //TODO: Tool around with the delay for retries AND for the separation between thread calls.
                    Thread.sleep(delay);
                    delay += random.nextInt(2000);
                    webPage.reload();
                } else {
                    scrapeResult.addMessage("Mod.io is rate limiting you, please wait a little and try again later.", ResultType.FAILED);
                    return "";
                }
            } catch (TimeoutError e) {
                scrapeResult.addMessage("Connection timed out while waiting to open page for mod \"" + modId + "\".", ResultType.FAILED);
                return "";
            } catch (Exception e) {
                scrapeResult.addMessage(getStackTrace(e), ResultType.FAILED);
                return "";
            }
        }
        return pageSource;
    }

    private void parseModIoModInfo(String pageSource, String modId, Result<String[]> modScrapeResult) {
        Document modPage = Jsoup.parse(pageSource);
        if (pageDoesNotContainMod(ModType.MOD_IO, modPage)) {
            String itemType = modPage.select(MOD_IO_MOD_TYPE_SELECTOR)
                    .stream()
                    .findFirst()
                    .flatMap(element -> element.childNodes().stream().findFirst())
                    .map(Object::toString).orElse("");
            if (!itemType.isEmpty()) {
                modScrapeResult.addMessage(modPage.title().split(" for Space Engineers - mod.io")[0] + " is not a mod, it is a " +
                        StringUtils.substringBetween(itemType, "<span>", "</span>") + ".", ResultType.FAILED);
            } else {
                modScrapeResult.addMessage("Unknown error when scraping mod.io.", ResultType.FAILED);
            }
            return;
        }

        //modInfo:
        // 0. Name
        // 1. Tags
        // 2. Description
        // 3. Year
        // 4. Month + day
        // 5. Hour
        String[] modInfo = new String[6];
        //Get mod name
        modInfo[0] = modPage.title().split(" for Space Engineers - mod.io")[0];

        //Get mod tags
        List<Node> tagNodes = modPage.select(MOD_IO_MOD_TAGS_SELECTOR).getLast().childNodes();
        StringBuilder concatenatedModTags = new StringBuilder();
        for (int i = 1; i < tagNodes.size(); i++) {
            String tag = StringUtils.substringBetween(tagNodes.get(i).toString(), "<a href=\"/g/spaceengineers?tags-in=", "\"");
            if (i + 1 < tagNodes.size()) {
                concatenatedModTags.append(tag).append(",");
            } else {
                concatenatedModTags.append(tag);
            }
        }
        modInfo[1] = concatenatedModTags.toString();

        //Get mod description
        modInfo[2] = modPage.select(MOD_IO_MOD_DESCRIPTION_SELECTOR)
                .stream()
                .findFirst()
                .map(element -> element.childNodes().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining())
                        .trim())
                .orElse("");
        if (modInfo[2].isEmpty()) {
            modScrapeResult.addMessage(String.format("Failed to get description for \"%s\".", modInfo[0]), ResultType.FAILED);
            return;
        }

        //Get mod last updated information
        String lastUpdatedRaw = modPage.select(MOD_IO_MOD_LAST_UPDATED_SELECTOR).getFirst().childNodes().getFirst().toString();
        String lastUpdatedFormatted = Optional.ofNullable(StringUtils.substringBetween(lastUpdatedRaw, "<span>", "</span>")).orElse("'");
        if(lastUpdatedFormatted.isEmpty()) {
            modScrapeResult.addMessage(String.format("Failed to get last updated information for \"%s\".", modInfo[0]), ResultType.FAILED);
            return;
        }
        String lastUpdatedQuantifier = lastUpdatedFormatted.substring(lastUpdatedFormatted.length() - 1);
        int duration = Integer.parseInt(lastUpdatedFormatted.substring(0, lastUpdatedFormatted.length() - 1));
        switch (lastUpdatedQuantifier) {
            case "h" -> {//Mod IO year + month + day + hour
                modInfo[3] = Year.now().toString();
                modInfo[4] = MonthDay.now().toString();
                modInfo[5] = LocalTime.now().minusHours(duration).toString();
            }
            case "d" -> {//Mod IO year + month + day
                modInfo[3] = Year.of(LocalDate.now().minusDays(duration).getYear()).toString();
                modInfo[4] = MonthDay.from(LocalDate.now().minusDays(duration)).toString();
            }
            case "y" -> //Mod IO year only
                    modInfo[3] = Year.now().minusYears(duration).toString();
            default -> throw new IllegalStateException("Unexpected value: " + lastUpdatedQuantifier);
        }

        modScrapeResult.addMessage("Successfully scraped information for mod " + modId + "!", ResultType.SUCCESS);
        modScrapeResult.setPayload(modInfo);
    }

    //Check if the mod we're scraping is actually a workshop mod.
    //Mod.io will NOT load without JS running, so we have to open a full headless browser, which is slow as hell.
    private boolean pageDoesNotContainMod(ModType modType, Document modPage) {
        if (modType == ModType.STEAM) {
            Elements typeElements = modPage.select(STEAM_MOD_TYPE_SELECTOR);
            if (!typeElements.isEmpty()) {
                Node modTypeNode = typeElements.getFirst().childNodes().stream().findFirst().orElse(null);
                return modTypeNode == null || !modTypeNode.toString().equals("Mod");
            } else {
                return true;
            }
        } else {
            Element element = modPage.selectFirst(MOD_IO_MOD_TYPE_SELECTOR);
            if (element != null) {
                Node modTypeNode = element.childNodes().stream().findFirst().orElse(null);
                //return element.childNodes().getFirst().toString().startsWith("Mod", 6);
                return modTypeNode == null || !modTypeNode.toString().contains("Mod");
            } else {
                return true;
            }
        }
    }
}

