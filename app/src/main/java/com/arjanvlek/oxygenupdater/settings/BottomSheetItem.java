package com.arjanvlek.oxygenupdater.views;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
@Getter
@Setter
@Builder
public class BottomSheetItem {

	private String title;

	private String subtitle;

	private String value;

	private Integer intValue;
}
