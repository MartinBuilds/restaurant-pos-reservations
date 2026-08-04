package bg.martinandonov.restaurant.menu.dto;

public class MenuAvailabilityResponse {

	private final Long menuItemId;
	private final String menuItemName;
	private final boolean manualAvailable;
	private final boolean available;
	private final String availabilityReason;
	private final Long maxPossibleServings;

	public MenuAvailabilityResponse(
			Long menuItemId,
			String menuItemName,
			boolean manualAvailable,
			boolean available,
			String availabilityReason,
			Long maxPossibleServings) {
		this.menuItemId = menuItemId;
		this.menuItemName = menuItemName;
		this.manualAvailable = manualAvailable;
		this.available = available;
		this.availabilityReason = availabilityReason;
		this.maxPossibleServings = maxPossibleServings;
	}

	public Long getMenuItemId() {
		return menuItemId;
	}

	public String getMenuItemName() {
		return menuItemName;
	}

	public boolean isManualAvailable() {
		return manualAvailable;
	}

	public boolean isAvailable() {
		return available;
	}

	public String getAvailabilityReason() {
		return availabilityReason;
	}

	public Long getMaxPossibleServings() {
		return maxPossibleServings;
	}
}
