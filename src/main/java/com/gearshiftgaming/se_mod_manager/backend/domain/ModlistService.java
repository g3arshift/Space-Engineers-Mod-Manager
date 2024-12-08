package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
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
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class ModlistService {

	private final String STEAM_WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

	private final String MODIO_URL = "https://mod.io/search/mods/";

	//TODO: Implement other repositories for the expected data options
	private final ModlistRepository MODLIST_REPOSITORY;
	private final String STEAM_MOD_TYPE_SELECTOR;

	private final String STEAM_MOD_LAST_UPDATED_SELECTOR;

	private final String STEAM_MOD_FIRST_POSTED_SELECTOR;

	private final String STEAM_MOD_TAGS_SELECTOR;

	private final String STEAM_MOD_DESCRIPTION_SELECTOR;

	private final String STEAM_MOD_VERIFICATION_SELECTOR;

	private final String STEAM_COLLECTION_GAME_NAME_SELECTOR;

	private final String STEAM_COLLECTION_MOD_ID_SELECTOR;

	private final String STEAM_COLLECTION_VERIFICATION_SELECTOR;

	private final String MODIO_MOD_SCRAPING_SELECTOR;

	private final String MODIO_MOD_JSOUP_MODID_SELECTOR;

	@Setter
	@Getter
	//TODO: Move this to backend controller, and decide and log there.
	//"Retrieving mod information from Steam Workshop..."
	//"Not retrieving mod information from Steam Workshop."
	private boolean workshopConnectionActive;

	public ModlistService(ModlistRepository MODLIST_REPOSITORY, Properties PROPERTIES) {
		this.MODLIST_REPOSITORY = MODLIST_REPOSITORY;
		this.STEAM_MOD_TYPE_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.type.cssSelector");
		this.STEAM_MOD_LAST_UPDATED_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.lastUpdated.cssSelector");
		this.STEAM_MOD_FIRST_POSTED_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.firstPosted.cssSelector");
		this.STEAM_MOD_TAGS_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.tags.cssSelector");
		this.STEAM_MOD_DESCRIPTION_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.description.cssSelector");
		this.STEAM_MOD_VERIFICATION_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.workshopVerification.cssSelector");

		this.STEAM_COLLECTION_GAME_NAME_SELECTOR = PROPERTIES.getProperty("semm.steam.collectionScraper.workshop.gameName.cssSelector");
		this.STEAM_COLLECTION_MOD_ID_SELECTOR = PROPERTIES.getProperty("semm.steam.collectionScraper.workshop.collectionContents.cssSelector");
		this.STEAM_COLLECTION_VERIFICATION_SELECTOR = PROPERTIES.getProperty("semm.steam.collectionScraper.workshop.collectionVerification.cssSelector");

		this.MODIO_MOD_SCRAPING_SELECTOR = PROPERTIES.getProperty("semm.modio.modScraper.tags.cssSelector");
		this.MODIO_MOD_JSOUP_MODID_SELECTOR = PROPERTIES.getProperty("semm.modio.modScraper.jsoup.modId.cssSelector");
	}

	public Result<List<Mod>> getModListFromFile(String modFilePath) throws IOException {
		File modlistFile = new File(modFilePath);
		Result<List<Mod>> result = new Result<>();
		if (!modlistFile.exists()) {
			result.addMessage("File does not exist.", ResultType.INVALID);
		} else if (FilenameUtils.getExtension(modlistFile.getName()).equals("txt") || FilenameUtils.getExtension(modlistFile.getName()).equals("doc")) {
			//TODO: Add modio functionality. Ask the user if it's a steam or ModIO list.
			result.setPayload(MODLIST_REPOSITORY.getSteamModList(modlistFile));
			result.addMessage(modlistFile.getName() + " selected.", ResultType.SUCCESS);
		} else {
			result.addMessage("Incorrect file type selected. Please select a .txt or .doc file.", ResultType.INVALID);
		}
		return result;
	}

	public List<Result<String>> scrapeSteamCollectionModIds(String collectionId) throws IOException {
		Pattern steamCollectionModIdFromHtml = Pattern.compile("(id=[0-9])\\d*");
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

			for (int i = 3; i < nodes.size(); i += 4) {
				Result<String> modIdResult = new Result<>();

				try {
					String modId = steamCollectionModIdFromHtml.matcher(nodes.get(i).childNodes().get(1).toString())
							.results()
							.map(MatchResult::group)
							.collect(Collectors.joining())
							.substring(3);
					modIdResult.addMessage("Successfully grabbed mod ID.", ResultType.SUCCESS);
					modIdResult.setPayload(modId);
				} catch (RuntimeException e) {
					modIdResult.addMessage("Failed to grab mod ID.\n" + e, ResultType.FAILED);
				}

				modIdScrapeResults.add(modIdResult);
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
			String modId = MOD_ID_FROM_IMAGE_URL.matcher(doc.select(MODIO_MOD_JSOUP_MODID_SELECTOR).toString())
					.results()
					.map(MatchResult::group)
					.collect(Collectors.joining());

			if(!modId.isBlank()) {
				modIdResult.setPayload(modId);
				modIdResult.addMessage("Successfully scraped Mod.io Mod ID from URL.", ResultType.SUCCESS);
			}
		} catch (NoSuchElementException e) {
			modIdResult.addMessage("Invalid Mod.io URL entered!", ResultType.INVALID);
		}

		return modIdResult;
	}

	//TODO: The executor needs to happen all the way up in the view layer.
	// It will call a single mod info scrape function from the UI, submitting a mod to it. The UI layer will have the code to increment the variable. The call in the view layer will have code that gets the future results.
	// This is much simpler than the crazy BS we were doing before
	public Result<String[]> generateModInformation(@NotNull Mod mod) throws IOException {
		return scrapeModInformation(mod.getId(), mod.getModType());
	}

	//Scrape the web pages of the mods we want the information from
	private Result<String[]> scrapeModInformation(String modId, ModType modType) throws IOException {
		Result<String[]> modScrapeResult = new Result<>();
		if (modType == ModType.STEAM) {
			Document modPage = Jsoup.connect(STEAM_WORKSHOP_URL + modId).get();
			String workshopItemType = modPage.select(STEAM_MOD_VERIFICATION_SELECTOR).getFirst().childNodes().getFirst().toString();
			if (modPage.title().equals("Steam Community :: Error")) {
				modScrapeResult.addMessage("Mod with ID \"" + modId + "\" cannot be found.", ResultType.FAILED);
			} else if (!workshopItemType.equals("Workshop")) {
				modScrapeResult.addMessage("Item with ID \"" + modId + "\" is not a mod.", ResultType.FAILED);
			} else {
				//The first item is mod name, the second is last updated, the third is a combined string of the tags, and the fourth is the raw HTML of the description.
				String[] modInfo = new String[4];
				String modName = modPage.title().split("Workshop::")[1];
				if (checkIfModIsMod(modId, modType, modPage)) {
					modInfo[0] = modName;

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
					modInfo[1] = lastUpdated;


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
					modInfo[2] = concatenatedModTags.toString();

					modInfo[3] = modPage.select(STEAM_MOD_DESCRIPTION_SELECTOR).getFirst().toString();

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
		} else {
			//TODO: Implement modIO stuff.
			//TODO: Look down in checkIfModIsMod for how to do modIO. It requires selenium.
			//Document doc = Jsoup.connect(MODIO_URL + modId).get();
			//return () -> Jsoup.connect(MOD_IO_URL + mod.getId()).get().title() + (checkIfModIsMod(mod.getId()) ? "" : "_NOT_A_MOD");
		}
		return modScrapeResult;
	}

	//Check if the mod we're scraping is actually a workshop mod.
	//Mod.io will NOT load without JS running, so we have to open a full headless browser, which is slow as hell.
	private boolean checkIfModIsMod(String modId, ModType modType, Document modPage) {
		if (modType == ModType.STEAM) {
			if (!modPage.select(STEAM_MOD_TYPE_SELECTOR).isEmpty()) {
				return (modPage.select(STEAM_MOD_TYPE_SELECTOR).getFirst().childNodes().getFirst().toString().equals("Mod"));
			} else {
				return false;
			}
		} else {
			//TODO: This all likely can be replaced with a document as well, same as above.
			WebDriver driver = getWebDriver();
			Element element;

			try {
				driver.get(MODIO_URL + modId);
				WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
				wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(MODIO_MOD_SCRAPING_SELECTOR)));

				String pageSource = driver.getPageSource();
				Document doc;
				if (pageSource != null) {
					doc = Jsoup.parse(pageSource);
					element = doc.selectFirst(MODIO_MOD_SCRAPING_SELECTOR);
					if (element != null) {
						return element.childNodes().getFirst().toString().startsWith("Mod", 6);
					}
				}
				return false;
			} finally {
				driver.quit();
			}
		}
	}

	@NotNull
	private static WebDriver getWebDriver() {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless");
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");

		Map<String, Object> prefs = new HashMap<>();
		prefs.put("profile.default_content_setting_values.images", 2); // 2 means block images
		prefs.put("profile.default_content_setting_values.stylesheets", 2); // 2 means block stylesheets
		options.setExperimentalOption("prefs", prefs);

		return new ChromeDriver(options);
	}
}

