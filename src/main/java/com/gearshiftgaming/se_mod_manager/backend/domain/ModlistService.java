package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
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
	private final Properties PROPERTIES;

	private final String STEAM_WORKSHOP_URL = "https://steamcommunity.com/sharedfiles/filedetails/?id=";

	private final String MODIO_URL = "https://mod.io/search/mods/";

	//TODO: Implement other repositories for the expected data options
	private final ModlistRepository MODLIST_REPOSITORY;
	private final String STEAM_MOD_TYPE_SELECTOR;

	private final String STEAM_MOD_LAST_UPDATED_SELECTOR;

	private final String STEAM_MOD_TAGS_SELECTOR;

	private final String STEAM_MOD_DESCRIPTION_SELECTOR;

	private final String MODIO_MOD_SCRAPING_SELECTOR;

	private final String MOD_DATE_FORMAT;


	@Setter
	@Getter
	//TODO: Move this to backend controller, and decide and log there.
	//"Retrieving mod information from Steam Workshop..."
	//"Not retrieving mod information from Steam Workshop."
	private boolean workshopConnectionActive;

	public ModlistService(ModlistRepository MODLIST_REPOSITORY, Properties PROPERTIES) {
		this.MODLIST_REPOSITORY = MODLIST_REPOSITORY;
		this.PROPERTIES = PROPERTIES;
		this.STEAM_MOD_TYPE_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.type.cssSelector");
		this.STEAM_MOD_LAST_UPDATED_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.lastUpdated.cssSelector");
		this.STEAM_MOD_TAGS_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.tags.cssSelector");
		this.STEAM_MOD_DESCRIPTION_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.description.cssSelector");

		this.MODIO_MOD_SCRAPING_SELECTOR = PROPERTIES.getProperty("semm.modio.modScraper.tags.cssSelector");

		this.MOD_DATE_FORMAT = PROPERTIES.getProperty("semm.mod.dateFormat");
	}

	public void fillOutModInfoById(Mod mod) throws IOException, ExecutionException, InterruptedException {
			generateModInformation(mod);
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

	private void generateModInformation(Mod mod) {
		Future<String[]> future;

		try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
			future = executorService.submit(scrapeModInformation(mod));

			String[] modInfo = future.get();

			mod.setFriendlyName(modInfo[0]);

			DateTimeFormatter formatter = new DateTimeFormatterBuilder()
					.parseCaseInsensitive()
					.appendPattern(MOD_DATE_FORMAT)
					.toFormatter();
			//TODO: we're getting a weird time mismatch... Might be a local machine issue. Or the steam server might be 3hrs behind our location.
//			ZonedDateTime modUpdatedGmtTime = ZonedDateTime.of(LocalDateTime.parse(modInfo[1], formatter), ZoneId.of("GMT"));
//			ZonedDateTime modUpdatedCurrentTimeZone = modUpdatedGmtTime.withZoneSameInstant(ZoneId.systemDefault());
//			mod.setLastUpdated(modUpdatedCurrentTimeZone.toLocalDateTime());
			mod.setLastUpdated(LocalDateTime.parse(modInfo[1], formatter));

			//TODO: We need to set mod description.
			List<String> modTags = List.of(modInfo[2].split(","));
			mod.setCategories(modTags);

			//TODO: Remove this catch
		} catch (IOException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	//Take in our list of mod ID's and fill out the rest of their fields.
//	private void generateModListInformation(List<Mod> modList) throws ExecutionException, InterruptedException {
//		List<Future<String[]>> futures = new ArrayList<>(modList.size());
//
//		//Create multiple virtual threads to efficiently scrape the page. We're using virtual ones here since this is IO intensive, not CPU
//		try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
//			for (Mod m : modList) {
//				futures.add(executorService.submit(scrapeModInformation(m)));
//			}
//			//TODO: Remove this catch
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//
//		for (int i = 0; i < modList.size(); i++) {
//			//TODO: Is this supposed to have a space?
//			//String[] modInfo = futures.get(i).get().split(" Workshop::");
//			//setModInformation(modList.get(i), modInfo);
//		}
//	}

//	public void setModInformation(Mod mod, String[] modInfo) {
//		if (modInfo[0].contains("_NOT_A_MOD")) {
//			mod.setFriendlyName(modInfo[0]);
//		} else {
//			mod.setPublishedServiceName(modInfo[0]);
//			mod.setFriendlyName(modInfo[1]);
//		}
//	}

	//TODO: This isn't even getting entered when we put in a very bad value
	//Scrape the web pages of the mods we want the information from
	private Callable<String[]> scrapeModInformation(Mod mod) throws IOException {
		if (mod.getModType() == ModType.STEAM) {
			Document modPage = Jsoup.connect(STEAM_WORKSHOP_URL + mod.getId()).get();
			//The first item is mod name, the second is last updated, and the third is a combined string of the tags.
			String[] modInfo = new String[4];
			String modName = modPage.title().split("Workshop::")[1];
			modInfo[0] = modName + (checkIfModIsMod(mod, modPage) ? "" : "_NOT_A_MOD");

			String lastUpdated = StringUtils.substringBetween(modPage.select(STEAM_MOD_LAST_UPDATED_SELECTOR).toString(),
					"<div class=\"detailsStatRight\">\n ",
					"\n</div>");
			//Append a year if we don't find one.
			Pattern yearPattern = Pattern.compile("\\b\\d{4}\\b");
			if(!yearPattern.matcher(lastUpdated).find()) {
				String[] lastUpdatedParts = lastUpdated.split(" @ ");
				lastUpdatedParts[0] += ", " + Year.now();
				lastUpdated = lastUpdatedParts[0] + " @ " + lastUpdatedParts[1];
			}
			modInfo[1] = lastUpdated;

			Element modTagElement = modPage.select(STEAM_MOD_TAGS_SELECTOR).getFirst();
			List<String> modTags = new ArrayList<>();
			for(int i = 1; i < modTagElement.childNodes().size(); i+=2) {
				modTags.add(modTagElement.childNodes().get(i).childNodes().getFirst().toString());
			}
			StringBuilder concatenatedModTags = new StringBuilder();
			for(int i = 0; i < modTags.size(); i++) {
				if(i + 1 < modTags.size()) {
					concatenatedModTags.append(modTags.get(i)).append(",");
				} else {
					concatenatedModTags.append(modTags.get(i));
				}
			}
			modInfo[2] = concatenatedModTags.toString();

			//TODO: We need to scrape mode description too. We also need to actually get proper links and make them clickable in the mod description tab.
			//TODO: This is the correct scrape, but full of the HTML stuff. We want to preserve links and make them hyperlinks in the mod description though.
			modInfo[3] = modPage.select(STEAM_MOD_DESCRIPTION_SELECTOR).getFirst().toString();

			return () -> modInfo;
			//return () -> Jsoup.connect(STEAM_WORKSHOP_URL + mod.getId()).get().title() + (checkIfModIsMod(mod) ? "" : "_NOT_A_MOD");
		} else {
			//TODO: Implement modIO stuff.
			//TODO: REmove this, here for testing and figuring out how to get the mod info I need
			Document doc = Jsoup.connect(MODIO_URL + mod.getId()).get();
			System.out.println("Hold here");
			//return () -> Jsoup.connect(MOD_IO_URL + mod.getId()).get().title() + (checkIfModIsMod(mod.getId()) ? "" : "_NOT_A_MOD");
			return null;
		}
	}

	//Check if the mod we're scraping is actually a workshop mod.
	//Mod.io will NOT load without JS running, so we have to open a full headless browser, which is slow as hell.
	private boolean checkIfModIsMod(Mod mod, Document modPage) throws IOException {
		if (mod.getModType() == ModType.STEAM) {
			return (modPage.select(STEAM_MOD_TYPE_SELECTOR).getFirst().childNodes().getFirst().toString().equals("Mod"));
		} else {
			//TODO: This all likely can be replaced with a document as well, same as above.
			WebDriver driver = getWebDriver();
			Element element;

			try {
				driver.get(MODIO_URL + mod.getId());
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

