package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.*;
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
        if (!gameName.equals("Space Engineers")) {
            Result<String> wrongGameResult = new Result<>();
            wrongGameResult.addMessage("The collection must be a Space Engineers collection!", ResultType.FAILED);
            modIdScrapeResults.add(wrongGameResult);
        } else if (!foundBreadcrumbName.equals("Collections")) {
            Result<String> notACollectionResult = new Result<>();
            notACollectionResult.addMessage("You must provide a link or ID of a collection!", ResultType.FAILED);
            modIdScrapeResults.add(notACollectionResult);
        } else {
            Elements elements = collectionPage.select(STEAM_COLLECTION_MOD_ID_SELECTOR);
            List<Node> nodes = elements.getFirst().childNodes();

            for (Node node : nodes) {
                Result<String> modIdResult = new Result<>();
                if (node.hasAttr("data-panel")) {
                    try {
                        String modId = STEAM_MOD_ID_PATTERN.matcher(node.childNodes().get(1).toString())
                                .results()
                                .map(MatchResult::group)
                                .collect(Collectors.joining())
                                .substring(3);
                        modIdResult.addMessage("Successfully grabbed mod ID.", ResultType.SUCCESS);
                        modIdResult.setPayload(modId);
                    } catch (RuntimeException e) {
                        modIdResult.addMessage(e.toString(), ResultType.FAILED);
                    }
					modIdScrapeResults.add(modIdResult);
                }
            }
        }

        return modIdScrapeResults;
    }

    /*
    Give a mod IO url, we get the actual ID of the mod. This is done by grabbing the resource ID contained within the URL of the mod primary image.
    Images are required for Mod.io mods, and the URL displays even without the JS running, so this is a more efficient way to get the ID before the more costly
        scraping process which opens a full headless, embedded web browser.
     */
    public Result<String> getModIoIdFromUrlName(String modName) throws IOException {
        Result<String> modIdResult = new Result<>();
        final String MOD_IO_NAME_URL = "https://mod.io/g/spaceengineers/m/";
        final Pattern MOD_ID_FROM_IMAGE_URL = Pattern.compile("(?<=/)\\d+(?=/)");

        Document doc = Jsoup.connect(MOD_IO_NAME_URL + modName).get();

        try {
            String modId = MOD_ID_FROM_IMAGE_URL.matcher(doc.select(MOD_IO_MOD_JSOUP_MOD_ID_SELECTOR).toString())
                    .results()
                    .map(MatchResult::group)
                    .toList().getLast();

            if (!modId.isBlank()) {
                modIdResult.setPayload(modId);
                modIdResult.addMessage("Successfully scraped Mod.io Mod ID from URL.", ResultType.SUCCESS);
            }
        } catch (NoSuchElementException e) {
            modIdResult.addMessage("Invalid Mod.io URL entered!", ResultType.INVALID);
        }

        return modIdResult;
    }


    public Result<String[]> generateModInformation(@NotNull Mod mod) throws IOException {
        return scrapeModInformation(mod);
    }

    //Scrape the web pages of the mods we want the information from
    private Result<String[]> scrapeModInformation(Mod mod) throws IOException {
        Result<String[]> modScrapeResult;
        if (mod instanceof SteamMod) {
            modScrapeResult = scrapeSteamMod(mod.getId());
        } else {
            modScrapeResult = scrapeModIoMod(mod.getId());
        }
        return modScrapeResult;
    }

    private Result<String[]> scrapeSteamMod(String modId) throws IOException {
        Result<String[]> modScrapeResult = new Result<>();
        try {
            Document modPage = Jsoup.connect(STEAM_WORKSHOP_URL + modId).get();
            if (modPage.title().equals("Steam Community :: Error")) { //Makes sure it's a valid link at all
                modScrapeResult.addMessage("Mod with ID \"" + modId + "\" cannot be found.", ResultType.FAILED);
            } else if (!modPage.select(STEAM_MOD_VERIFICATION_SELECTOR).getFirst().childNodes().getFirst().toString().equals("Workshop")) { //Makes sure it isn't something like a screenshot
                modScrapeResult.addMessage("Item with ID \"" + modId + "\" is not a mod.", ResultType.FAILED);
            } else if (modPage.select(STEAM_COLLECTION_VERIFICATION_SELECTOR).getFirst().childNodes().getFirst().toString().equals("Collections")) { //Makes sure it's not a collection
                modScrapeResult.addMessage("\"" + modPage.title().split("Steam Workshop::")[1] + "\" is a collection, not a mod!", ResultType.FAILED);
            } else {
                //The first item is mod name, second is a combined string of the tags, third is the raw HTML of the description, and fourth is last updated.
                String[] modInfo = new String[4];
                String modName = modPage.title().split("Workshop::")[1];
                if (checkIfModIsMod(ModType.STEAM, modPage)) {
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
                } else {
                    if (!modPage.select(STEAM_MOD_TYPE_SELECTOR).isEmpty()) {
                        modScrapeResult.addMessage("\"" + modPage.title().split("Workshop::")[1] + "\" is not a mod, it is a " +
                                modPage.select(STEAM_MOD_TYPE_SELECTOR).getFirst().childNodes().getFirst().toString() + ".", ResultType.FAILED);
                    } else {
                        modScrapeResult.addMessage("\"" + modPage.title().split("Workshop::")[1] + "\" is for either a workshop item that is not a mod, for the wrong game, or is not publicly available on the workshop.", ResultType.INVALID);
                    }
                }
            }
        } catch (Exception e) {
            modScrapeResult.addMessage(e.toString(), ResultType.FAILED);
        }
        return modScrapeResult;
    }

    private Result<String[]> scrapeModIoMod(String modId) {
        Result<String[]> modScrapeResult = new Result<>();
        //By this point we should have a valid ModIO ID to lookup the mods by for the correct game. Need to verify tags and that it is a mod, however.
        WebDriver driver = getChromeWebDriver();

        try {
            driver.get(MOD_IO_URL + modId);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(MOD_IO_SCRAPING_TIMEOUT));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(MOD_IO_SCRAPING_WAIT_CONDITION_SELECTOR)));

            String pageSource = driver.getPageSource();
            if (pageSource != null) {
                //modInfo:
                // 0. Name
                // 1. Tags
                // 2. Description
                // 3. Year
                // 4. Month + day
                // 5. Hour
                String[] modInfo = new String[6];
                Document modPage = Jsoup.parse(pageSource);

                if (checkIfModIsMod(ModType.MOD_IO, modPage)) {
                    String modName = modPage.title().split(" for Space Engineers - mod.io")[0];
                    modInfo[0] = modName;

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

                    modInfo[2] = modPage.select(MOD_IO_MOD_DESCRIPTION_SELECTOR).getFirst().childNodes().getLast().toString();

                    String lastUpdatedRaw = modPage.select(MOD_IO_MOD_LAST_UPDATED_SELECTOR).getFirst().childNodes().getFirst().toString();
                    String lastUpdatedQuantifier = lastUpdatedRaw.substring(lastUpdatedRaw.length() - 1);
                    int duration = Integer.parseInt(lastUpdatedRaw.substring(0, lastUpdatedRaw.length() - 1));

                    switch (lastUpdatedQuantifier) {
                        case "h" -> {//Mod IO year + month + day + hour
                            modInfo[3] = Year.now().toString();
                            modInfo[4] = MonthDay.now().toString();
                            modInfo[5] = LocalTime.now().minusHours(duration).toString();
                        }
                        case "d" -> {//Mod IO year + month + day
                            modInfo[3] = Year.now().toString();
                            modInfo[4] = MonthDay.from(LocalDate.now().minusDays(duration)).toString();
                        }
                        case "y" -> //Mod IO year only
                                modInfo[3] = Year.now().minusYears(duration).toString();
                        default -> throw new IllegalStateException("Unexpected value: " + lastUpdatedQuantifier);
                    }

                    modScrapeResult.addMessage("Successfully scraped information for mod " + modId + "!", ResultType.SUCCESS);
                    modScrapeResult.setPayload(modInfo);
                } else {
                    modScrapeResult.addMessage(modPage.title().split(" for Space Engineers - mod.io")[0] + " is not a mod, it is a " +
                            StringUtils.substringBetween(Objects.requireNonNull(modPage.selectFirst(MOD_IO_MOD_TYPE_SELECTOR)).childNodes().getFirst().toString(),
                                    "<span>", "</span>") + ".", ResultType.FAILED);
                }
            }
        } catch (Exception e) {
            //This really isn't how I want to do the flow control, but Selenium immediately throws this as an exception if it times out so there's not a lot I can do.
            if (e.getMessage().startsWith("Expected condition failed: waiting for presence of element")) {
                modScrapeResult.addMessage("Mod with ID \"" + modId + "\" cannot be found.", ResultType.FAILED);
            } else {
                modScrapeResult.addMessage(e.toString(), ResultType.FAILED);
            }
        } finally {
            driver.quit();
        }

        return modScrapeResult;
    }

    //Check if the mod we're scraping is actually a workshop mod.
    //Mod.io will NOT load without JS running, so we have to open a full headless browser, which is slow as hell.
    private boolean checkIfModIsMod(ModType modType, Document modPage) {
        if (modType == ModType.STEAM) {
            if (!modPage.select(STEAM_MOD_TYPE_SELECTOR).isEmpty()) {
                return (modPage.select(STEAM_MOD_TYPE_SELECTOR).getFirst().childNodes().getFirst().toString().equals("Mod"));
            } else {
                return false;
            }
        } else {
            Element element = modPage.selectFirst(MOD_IO_MOD_TYPE_SELECTOR);
            if (element != null) {
                return element.childNodes().getFirst().toString().startsWith("Mod", 6);
            } else {
                return false;
            }
        }
    }

    @NotNull
    private WebDriver getChromeWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.images", 2); // 2 means block images
        options.setExperimentalOption("prefs", prefs);

        return new ChromeDriver(options);
    }
}

