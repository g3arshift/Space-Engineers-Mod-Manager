package com.gearshiftgaming.se_mod_manager.frontend.models.helper;

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
public class DropdownLabelHelper {

	public static String truncateWithEllipsisWithRealWidth(String text, double maxWidth) {
		Text tempText = new Text(text);

		//I have no idea why this works, but without these two lines it won't get the proper calculation. You'd think applyCss is all you'd need, but no.
		new Scene(new Group(tempText));
		tempText.applyCss();

		if (tempText.getBoundsInLocal().getWidth() <= maxWidth) {
			return text;
		}

		// If the text is too long, truncate it
		String ellipsis = "...";
		String truncatedText = text;

		while (tempText.getBoundsInLocal().getWidth() + 15 > maxWidth - tempText.getFont().getSize()) {
			if (truncatedText.length() <= 1) {
				return truncatedText; // Prevent empty string or single character
			}
			truncatedText = truncatedText.substring(0, truncatedText.length() - 1);
			tempText.setText(truncatedText + ellipsis);
		}

		return truncatedText + ellipsis;
	}

	public static String truncateWithEllipsis(String text, double maxWidth) {
		Text tempText = new Text(text);

		if (tempText.getBoundsInLocal().getWidth() <= maxWidth) {
			return text;
		}

		// If the text is too long, truncate it
		String ellipsis = "...";
		String truncatedText = text;

		while (tempText.getBoundsInLocal().getWidth() > maxWidth - tempText.getFont().getSize()) {
			if (truncatedText.length() <= 1) {
				return truncatedText; // Prevent empty string or single character
			}
			truncatedText = truncatedText.substring(0, truncatedText.length() - 1);
			tempText.setText(truncatedText + ellipsis);
		}

		return truncatedText + ellipsis;
	}
}
