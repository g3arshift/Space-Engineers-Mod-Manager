package com.gearshiftgaming.se_mod_manager.backend.domain.mod;

import com.gearshiftgaming.se_mod_manager.backend.data.modlist.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.Mod;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.ModType;
import com.gearshiftgaming.se_mod_manager.backend.models.mod.SteamMod;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.Result;
import com.gearshiftgaming.se_mod_manager.backend.models.shared.ResultType;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

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

    private static final String STEAM_WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

    private static final String MOD_IO_URL = "https://mod.io/search/mods/";

    private final ModlistRepository modlistRepository;

    private final String steamModTypeSelector;

    private final String steamModLastUpdatedSelector;

    private final String steamModFirstPostedSelector;

    private final String steamModTagsSelector;

    private final String steamModDescriptionSelector;

    private final String steamModVerificationSelector;

    private final Pattern steamModIdPattern;

    private final String steamCollectionGameNameSelector;

    private final String steamCollectionModIdSelector;

    private final String steamCollectionVerificationSelector;

    private final int modIoScrapingTimeout;

    //TODO: Split this into two subclasses, one for steam one for mod.io. Much more maintainable then.

    //TODO: Download mods using steamCMD to the user directory. Have some sort of UI indication they're downloading in the UI.
    // Once downloaded, get modified paths and modify conflict table.
    public ModInfoService(ModlistRepository modlistRepository, Properties PROPERTIES) {
        this.modlistRepository = modlistRepository;

        this.steamModTypeSelector = PROPERTIES.getProperty("semm.steam.modScraper.workshop.type.cssSelector");
        this.steamModLastUpdatedSelector = PROPERTIES.getProperty("semm.steam.modScraper.workshop.lastUpdated.cssSelector");
        this.steamModFirstPostedSelector = PROPERTIES.getProperty("semm.steam.modScraper.workshop.firstPosted.cssSelector");
        this.steamModTagsSelector = PROPERTIES.getProperty("semm.steam.modScraper.workshop.tags.cssSelector");
        this.steamModDescriptionSelector = PROPERTIES.getProperty("semm.steam.modScraper.workshop.description.cssSelector");
        this.steamModVerificationSelector = PROPERTIES.getProperty("semm.steam.modScraper.workshop.workshopVerification.cssSelector");

        this.steamModIdPattern = Pattern.compile(PROPERTIES.getProperty("semm.steam.mod.id.pattern"));

        this.steamCollectionGameNameSelector = PROPERTIES.getProperty("semm.steam.collectionScraper.workshop.gameName.cssSelector");
        this.steamCollectionModIdSelector = PROPERTIES.getProperty("semm.steam.collectionScraper.workshop.collectionContents.cssSelector");
        this.steamCollectionVerificationSelector = PROPERTIES.getProperty("semm.steam.collectionScraper.workshop.collectionVerification.cssSelector");

        this.modIoScrapingTimeout = Integer.parseInt(PROPERTIES.getProperty("semm.modio.modScraper.timeout"));
    }

    public List<String> getModIdsFromFile(File modlistFile, ModType modType) throws IOException {
        if (modType == ModType.STEAM) {
            return modlistRepository.getSteamModList(modlistFile);
        } else {
            return modlistRepository.getModIoModUrls(modlistFile);
        }
    }

    public List<Result<String>> scrapeSteamCollectionModIds(String collectionId) throws IOException {
        List<Result<String>> modIdScrapeResults = new ArrayList<>();

        Document collectionPage = Jsoup.connect(STEAM_WORKSHOP_URL + collectionId).get();

        String gameName = collectionPage.select(steamCollectionGameNameSelector).getFirst().childNodes().getFirst().toString().trim();
        String foundBreadcrumbName = collectionPage.select(steamCollectionVerificationSelector).getFirst().childNodes().getFirst().toString();
        if (!gameName.equals("Space Engineers")) { //Game name check
            Result<String> wrongGameResult = new Result<>();
            wrongGameResult.addMessage("The collection must be a Space Engineers collection!", ResultType.FAILED);
            modIdScrapeResults.add(wrongGameResult);
        } else if (!foundBreadcrumbName.equals("Collections")) { //Steam item check (makes sure it's a collection)
            Result<String> notACollectionResult = new Result<>();
            notACollectionResult.addMessage("You must provide a link or ID of a collection!", ResultType.FAILED);
            modIdScrapeResults.add(notACollectionResult);
        } else {
            Elements elements = collectionPage.select(steamCollectionModIdSelector);
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
                        modId.append(steamModIdPattern.matcher(node.childNodes().get(1).toString())
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

        Element ogImageUrl = doc.selectFirst("meta[property=og:image]");
        if (ogImageUrl == null) {
            modIdResult.addMessage("Invalid Mod.io URL entered!", ResultType.INVALID);
            return modIdResult;
        }

        String contentUrl = ogImageUrl.attr("content");

        // Match: /mods/{anyHexOrFolder}/{numericId}/
        Pattern pattern = Pattern.compile("/mods/[^/]+/(\\d+)/");
        Matcher matcher = pattern.matcher(contentUrl);
        if(!matcher.find()) {
            modIdResult.addMessage("Invalid Mod.io URL entered!", ResultType.INVALID);
            return modIdResult;
        }

        modIdResult.setPayload(matcher.group(1));
        modIdResult.addMessage("Successfully scraped Mod.io Mod ID from URL.", ResultType.SUCCESS);

        return modIdResult;
    }

    //Scrape the web pages of the mods we want the information from
    public Result<String[]> scrapeModInformation(Mod mod) {
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

        String itemType = modPage.select(steamModVerificationSelector)
                .stream()
                .findFirst()
                .flatMap(element -> element.childNodes().stream().findFirst())
                .map(Object::toString).orElse("");
        if (!itemType.equals("Workshop")) { //Makes sure it isn't something like a screenshot
            modScrapeResult.addMessage("Item with ID \"" + modId + "\" is not part of the workshop, it is part of " + itemType + ".", ResultType.FAILED);
            return modScrapeResult;
        }

        String workshopType = modPage.select(steamCollectionVerificationSelector)
                .stream()
                .findFirst()
                .flatMap(element -> element.childNodes().stream().findFirst())
                .map(Object::toString).orElse("");
        if (workshopType.equals("Collections")) { //Makes sure it's not a collection
            modScrapeResult.addMessage("\"" + modPage.title().split("Steam Workshop::")[1] + "\" is a collection, not a mod!", ResultType.FAILED);
            return modScrapeResult;
        }

        String modName = modPage.title().contains("Workshop::") ? modPage.title().split("Workshop::")[1] : modPage.title();
        if (steamPageDoesNotContainMod(modPage)) {
            if (!modPage.select(steamModTypeSelector).isEmpty()) {
                modScrapeResult.addMessage("\"" + modPage.title().split("Workshop::")[1] + "\" is not a mod, it is a " +
                        modPage.select(steamModTypeSelector).getFirst().childNodes().getFirst().toString() + ".", ResultType.FAILED);
            } else {
                modScrapeResult.addMessage("\"" + modPage.title().split("Workshop::")[1] + "\" is for either a workshop item that is not a mod, for the wrong game, or is not publicly available on the workshop.", ResultType.INVALID);
            }
            return modScrapeResult;
        }

        //The first item is mod name, second is a combined string of the tags, third is the raw HTML of the description, and fourth is last updated.
        String[] modInfo = new String[4];
        modInfo[0] = modName;

        Elements modTagElements = modPage.select(steamModTagsSelector);
        Element modTagElement;
        if (!modTagElements.isEmpty()) {
            modTagElement = modPage.select(steamModTagsSelector).getFirst();
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

        modInfo[2] = modPage.select(steamModDescriptionSelector).getFirst().toString();

        String lastUpdated;
        if (modPage.select(steamModLastUpdatedSelector).isEmpty()) {
            lastUpdated = StringUtils.substringBetween(modPage.select(steamModFirstPostedSelector).toString(),
                    "<div class=\"detailsStatRight\">\n ",
                    "\n</div>");
        } else {
            lastUpdated = StringUtils.substringBetween(modPage.select(steamModLastUpdatedSelector).toString(),
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

        //Normalizes our datetime input from international formatting to US.
        if (Character.isDigit(lastUpdated.charAt(0))) {
            String[] dateParts = lastUpdated.split(" ");
            String day = dateParts[0];
            String month = dateParts[1];
            String year = dateParts[2];
            String time = dateParts[4];

            month = month.replace(",", "");
            lastUpdated = String.format("%s %s, %s @ %s", month, day, year, time);
        }
        modInfo[3] = lastUpdated;

        modScrapeResult.addMessage("Successfully scraped information for mod " + modId + "!", ResultType.SUCCESS);
        modScrapeResult.setPayload(modInfo);

        return modScrapeResult;
    }

    private Result<String[]> scrapeModIoMod(String modId) {
        Result<String[]> modScrapeResult = new Result<>();
        //By this point we should have a valid ModIO ID to look up the mods by for the correct game. Need to verify tags and that it is a mod, however.
        try (Playwright scraper = Playwright.create();
             Browser browser = scraper.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true).setChromiumSandbox(false))) {
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
                if (jsonText != null) { //We only get this to not be null if mod.io is giving us an error
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
                //webPage.waitForSelector(new Page.WaitForSelectorOptions().setTimeout(MOD_IO_SCRAPING_TIMEOUT));
                webPage.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(modIoScrapingTimeout));
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
        //Get mod tags. The first one will always be the mod type.
        List<String> uniqueTags = modPage.select("a[href]").stream()
                .map(element -> element.attr("href"))
                .filter(href -> href.startsWith("/g/spaceengineers?tags="))
                .map(href -> href.substring(href.indexOf("=") + 1))
                .distinct()
                .toList();

        String modType = uniqueTags.getFirst();
        if (modIoPageDoesNotContainMod(modType)) {
            modScrapeResult.addMessage(String.format("%s is not a mod, it is a %s.", modPage.title().split(" for Space Engineers - mod.io")[0], modType), ResultType.FAILED);
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

        modInfo[1] = String.join(",", uniqueTags.subList(1, uniqueTags.size()));

        /* Get the HTML we need for the proper rendering of the description.
        There SHOULD only be one tag using .tw-view-text with a parent that has the class tw-flex and tw-flex col.

        Descriptions can be in another format though where the user didn't enter one, so we will check for that too.*/
        //FIXME: This is really sloppy.
        Element fullDescription;
        fullDescription = findDescriptionFromViewTextTag(modScrapeResult, modPage);
        if (fullDescription == null)
            fullDescription = findDescriptionFromBreakWordsTag(modScrapeResult, modPage);

        //If we've failed our second check, fail it back.
        if (fullDescription == null) return;

        modInfo[2] = fullDescription.toString();
        if (modInfo[2].isEmpty()) {
            modScrapeResult.addMessage(String.format("Failed to get description for \"%s\".", modInfo[0]), ResultType.FAILED);
            return;
        }

        //This is awful and terrible but Mod.io does some very annoying things with how it returns data.
        //Find the script tag that contains the JSON-LD for the news article, because for some reason that's where mod.io stuffed the lastUpdated tag.
        //CSS style selector for the data we want
        Element newsArticleScript = modPage.selectFirst("script[type='application/ld+json']#NewsArticle");

        if (newsArticleScript == null || newsArticleScript.childNodeSize() == 0) {
            modScrapeResult.addMessage(String.format("Failed to get last updated date for \"%s\"", modInfo[0]), ResultType.FAILED);
            return;
        }

        String jsonContent = newsArticleScript.childNodes().getFirst().toString();
        JsonObject newsArticleJson = new Gson().fromJson(jsonContent, JsonObject.class);
        if (!newsArticleJson.has("dateModified")) {
            modScrapeResult.addMessage(String.format("Could not find dateModified for \"%s\"", modInfo[0]), ResultType.FAILED);
            return;
        }

        LocalDateTime lastUpdated = LocalDateTime.ofInstant(
                Instant.parse(newsArticleJson.get("dateModified").getAsString()),
                ZoneId.systemDefault());

        modInfo[3] = String.valueOf(lastUpdated.getYear());
        //We originally used a MonthDay here so the rest of the application expects that format. As a result, we need to prepend a 0 if the month value or day value is < 10.
        modInfo[4] = String.format("--%s%s-%s%s", lastUpdated.getMonthValue() < 10 ? "0" : "", lastUpdated.getMonthValue(),
                lastUpdated.getDayOfMonth() < 10 ? "0" : "", lastUpdated.getDayOfMonth());
        modInfo[5] = String.format("%s:%s:%s", lastUpdated.getHour(), lastUpdated.getMinute(), lastUpdated.getSecond());

        modScrapeResult.addMessage("Successfully scraped information for mod " + modId + "!", ResultType.SUCCESS);
        modScrapeResult.setPayload(modInfo);
    }

    @Nullable
    private Element findDescriptionFromViewTextTag(Result<String[]> modScrapeResult, Document modPage) {
        //Check for the first mod description pattern
        Elements descriptionTags = modPage.select(".tw-view-text");
        return findDescriptionElementByClass(modScrapeResult, descriptionTags);
    }

    @Nullable
    private Element findDescriptionFromBreakWordsTag(Result<String[]> modScrapeResult, Document modPage) {
        List<Element> descriptionTags = modPage.select(".tw-break-words").stream()
                .filter(element -> "tw-break-words".equals(element.className()))
                .toList();
        return findDescriptionElementByClass(modScrapeResult, descriptionTags);
    }

    @Nullable
    private Element findDescriptionElementByClass(Result<String[]> modScrapeResult, List<Element> descriptionTags) {
        Element fullDescription = null;
        for (Element element : descriptionTags) {
            if (element.parent() != null && element.parent().hasClass("tw-flex") && element.parent().hasClass("tw-flex-col")) {
                fullDescription = element;
                break;
            }
        }

        if (fullDescription == null) {
            modScrapeResult.addMessage("Failed to get description tag.", ResultType.FAILED);
            return null;
        }

        for (int i = 0; i < 3; i++) {
            fullDescription = fullDescription.parent();
            if (fullDescription == null) {
                modScrapeResult.addMessage("Parent chain ended early during iteration " + i, ResultType.FAILED);
                return null;
            }
        }

        return fullDescription;
    }

    //Check if the mod we're scraping is actually a workshop mod.
    private boolean steamPageDoesNotContainMod(Document modPage) {
        Elements typeElements = modPage.select(steamModTypeSelector);
        if (!typeElements.isEmpty()) {
            Node modTypeNode = typeElements.getFirst().childNodes().stream().findFirst().orElse(null);
            return modTypeNode == null || !modTypeNode.toString().equals("Mod");
        } else {
            return true;
        }
    }

    private boolean modIoPageDoesNotContainMod(String tag) {
        return !tag.equals("Mod");
    }
}

