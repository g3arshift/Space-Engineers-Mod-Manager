package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import com.gearshiftgaming.se_mod_manager.frontend.domain.UiService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

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

	private final String STEAM_MOD_TAGS_SELECTOR;

	private final String STEAM_MOD_DESCRIPTION_SELECTOR;

	private final String STEAM_MOD_NOT_FOUND_SELECTOR;

	private final String MODIO_MOD_SCRAPING_SELECTOR;


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
		this.STEAM_MOD_TAGS_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.tags.cssSelector");
		this.STEAM_MOD_DESCRIPTION_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.description.cssSelector");
		this.STEAM_MOD_NOT_FOUND_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.notFound.cssSelector");

		this.MODIO_MOD_SCRAPING_SELECTOR = PROPERTIES.getProperty("semm.modio.modScraper.tags.cssSelector");
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

	public List<Future<Result<String[]>>> generateModInformation(List<Mod> modList) {
		List<Future<Result<String[]>>> futures = new ArrayList<>(modList.size());

		//TODO: Not sure if this catch will properly feed back up for each failed try...
		try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
			for (Mod m : modList) {
				futures.add(executorService.submit(scrapeModInformation(m.getId(), m.getModType())));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return futures;
	}

	//Scrape the web pages of the mods we want the information from
	private Callable<Result<String[]>> scrapeModInformation(String modId, ModType modType) throws IOException {
		Result<String[]> modScrapeResult = new Result<>();
		if (modType == ModType.STEAM) {
			Document modPage = Jsoup.connect(STEAM_WORKSHOP_URL + modId).get();

			if (modPage.title().equals("Steam Community :: Error")) {
				modScrapeResult.addMessage("Item with ID \"" + modId + "\" cannot be found.", ResultType.FAILED);
			} else {
				//The first item is mod name, the second is last updated, the third is a combined string of the tags, and the fourth is the raw HTML of the description.
				String[] modInfo = new String[4];
				String modName = modPage.title().split("Workshop::")[1];
				if (checkIfModIsMod(modId, modType, modPage)) {
					modInfo[0] = modName;

					String lastUpdated = StringUtils.substringBetween(modPage.select(STEAM_MOD_LAST_UPDATED_SELECTOR).toString(),
							"<div class=\"detailsStatRight\">\n ",
							"\n</div>");
					//Append a year if we don't find one. This regex looks for any four contiguous digits.
					Pattern yearPattern = Pattern.compile("\\b\\d{4}\\b");
					if (!yearPattern.matcher(lastUpdated).find()) {
						String[] lastUpdatedParts = lastUpdated.split(" @ ");
						lastUpdatedParts[0] += ", " + Year.now();
						lastUpdated = lastUpdatedParts[0] + " @ " + lastUpdatedParts[1];
					}
					modInfo[1] = lastUpdated;

					Element modTagElement = modPage.select(STEAM_MOD_TAGS_SELECTOR).getFirst();
					List<String> modTags = new ArrayList<>();
					for (int i = 1; i < modTagElement.childNodes().size(); i += 2) {
						modTags.add(modTagElement.childNodes().get(i).childNodes().getFirst().toString());
					}
					StringBuilder concatenatedModTags = new StringBuilder();
					for (int i = 0; i < modTags.size(); i++) {
						if (i + 1 < modTags.size()) {
							concatenatedModTags.append(modTags.get(i)).append(",");
						} else {
							concatenatedModTags.append(modTags.get(i));
						}
					}
					modInfo[2] = concatenatedModTags.toString();

					modInfo[3] = modPage.select(STEAM_MOD_DESCRIPTION_SELECTOR).getFirst().toString();

					modScrapeResult.addMessage("Successfully scraped information for mod " + modId + "!", ResultType.SUCCESS);
					modScrapeResult.setPayload(modInfo);
				} else {
					modScrapeResult.addMessage(modId + " is for either a workshop item that is not a mod, for the wrong game, or is not publicly available on the workshop.", ResultType.INVALID);
				}
			}
		} else {
			//TODO: Implement modIO stuff.
			//TODO: REmove this, here for testing and figuring out how to get the mod info I need
			Document doc = Jsoup.connect(MODIO_URL + modId).get();
			System.out.println("Hold here");
			//return () -> Jsoup.connect(MOD_IO_URL + mod.getId()).get().title() + (checkIfModIsMod(mod.getId()) ? "" : "_NOT_A_MOD");
		}
		return () -> modScrapeResult;
	}

	//Check if the mod we're scraping is actually a workshop mod.
	//Mod.io will NOT load without JS running, so we have to open a full headless browser, which is slow as hell.
	private boolean checkIfModIsMod(String modId, ModType modType, Document modPage) throws IOException {
		if (modType == ModType.STEAM) {
			return (modPage.select(STEAM_MOD_TYPE_SELECTOR).getFirst().childNodes().getFirst().toString().equals("Mod"));
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

