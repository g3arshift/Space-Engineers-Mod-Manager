package com.gearshiftgaming.se_mod_manager.frontend.models.utility;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;

/**
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class TextTruncationUtility {

	public static String truncateWithEllipsisWithRealWidth(String text, double maxWidth) {
		Text tempText = new Text(text);

		//I have no idea why this works, but without these two lines it won't get the proper text size calculation. You'd think applyCss is all you'd need, but no.
		//Likely something with actually rendering the text.
		new Scene(new Group(tempText));
		tempText.applyCss();

		return getTruncatedText(text, maxWidth, tempText);
	}

	public static String truncateWithEllipsis(String text, double maxWidth) {
		return getTruncatedText(text, maxWidth, new Text(text));
	}

	private static String getTruncatedText(String text, double maxWidth, Text tempText) {
		if (tempText.getBoundsInLocal().getWidth() <= maxWidth) {
			return text;
		}

		String ellipsis = "...";
		int low = 0;
		int high = text.length();
		int bestFit = 0;

		while (low <= high) {
			int mid = (low + high) / 2;
			String candidate = text.substring(0, mid) + ellipsis;
			tempText.setText(candidate);

			if (tempText.getBoundsInLocal().getWidth() <= maxWidth) {
				bestFit = mid; // Update best fit
				low = mid + 1; // Try for a longer substring
			} else {
				high = mid - 1; // Try for a shorter substring
			}
		}

		return text.substring(0, bestFit) + ellipsis;
	}
}
