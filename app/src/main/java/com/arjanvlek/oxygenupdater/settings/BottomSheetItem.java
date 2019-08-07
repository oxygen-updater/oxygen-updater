package com.arjanvlek.oxygenupdater.settings;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
@SuppressWarnings("WeakerAccess")
@Getter
@Setter
@Builder
public class BottomSheetItem {

	private String title;

	private String subtitle;

	private String value;

	private Object secondaryValue;
}
