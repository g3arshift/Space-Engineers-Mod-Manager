package com.gearshiftgaming.se_mod_manager.backend.domain;

import com.gearshiftgaming.se_mod_manager.backend.data.ModlistRepository;
import com.gearshiftgaming.se_mod_manager.backend.models.*;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.checkerframework.checker.units.qual.C;
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

import javax.print.Doc;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

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
	private final String STEAM_MOD_SCRAPING_SELECTOR;

	private final String MODIO_MOD_SCRAPING_SELECTOR;

	@Setter
	@Getter
	//TODO: Move this to backend controller, and decide and log there.
	//"Retrieving mod information from Steam Workshop..."
	//"Not retrieving mod information from Steam Workshop."
	private boolean workshopConnectionActive;

	public ModlistService(ModlistRepository MODLIST_REPOSITORY, Properties PROPERTIES) {
		this.MODLIST_REPOSITORY = MODLIST_REPOSITORY;
		this.PROPERTIES = PROPERTIES;
		this.STEAM_MOD_SCRAPING_SELECTOR = PROPERTIES.getProperty("semm.steam.modScraper.workshop.type.cssSelector");
		this.MODIO_MOD_SCRAPING_SELECTOR = PROPERTIES.getProperty("semm.modio.modScraper.tags.cssSelector");
	}

	public Future<String> getModInfoById(Mod mod) throws IOException, ExecutionException, InterruptedException {
			return generateModInformation(mod);
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

	private Future<String> generateModInformation(Mod mod) throws ExecutionException, InterruptedException {
		Future<String> future;

		try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
			future = executorService.submit(scrapeModInformation(mod));
			//TODO: Remove this catch
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return future;
	}

	//TODO: Should instead create a function called generateModList or something more appropriate that calls two methods, one to generate info for steam mods, the other for ModIO mods, and perform those operations on the given modlist.
	// Or, better yet, just have a flag for mod type and pass that to our scrape and check. They also need type flags.
	//TODO: Do this with the concurrency API
	//Take in our list of mod ID's and fill out the rest of their fields.
	private void generateModListInformation(List<Mod> modList) throws ExecutionException, InterruptedException {
		List<Future<String>> futures = new ArrayList<>(modList.size());

		//Create multiple virtual threads to efficiently scrape the page. We're using virtual ones here since this is IO intensive, not CPU
		try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
			for (Mod m : modList) {
				futures.add(executorService.submit(scrapeModInformation(m)));
			}
			//TODO: Remove this catch
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		for (int i = 0; i < modList.size(); i++) {
			//TODO: Is this supposed to have a space?
			String[] modInfo = futures.get(i).get().split(" Workshop::");
			setModInformation(modList.get(i), modInfo);
		}
	}

	public void setModInformation(Mod mod, String[] modInfo) {
		if (modInfo[0].contains("_NOT_A_MOD")) {
			mod.setFriendlyName(modInfo[0]);
		} else {
			mod.setPublishedServiceName(modInfo[0]);
			mod.setFriendlyName(modInfo[1]);
		}
	}

	//TODO: We should probably store the dom object with how much we're going to have to be checking on these pages
	//Scrape the Steam Workshop HTML pages for their titles, which are our friendly names
	private Callable<String> scrapeModInformation(Mod mod) throws IOException {
		if (mod.getModType() == ModType.STEAM) {
			Element element = Jsoup.connect(STEAM_WORKSHOP_URL + mod.getId()).get().body();
			return () -> Jsoup.connect(STEAM_WORKSHOP_URL + mod.getId()).get().title() + (checkIfModIsMod(mod) ? "" : "_NOT_A_MOD");
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
	private boolean checkIfModIsMod(Mod mod) throws IOException {
		if (mod.getModType() == ModType.STEAM) {
			return (Jsoup.connect(STEAM_WORKSHOP_URL + mod.getId()).get().select(STEAM_MOD_SCRAPING_SELECTOR).getFirst().childNodes().getFirst().toString().equals("Mod"));
		} else {
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

