package bg.martinandonov.restaurant.inventory.dto;

import java.util.List;

public class RecipeResponse {

	private final Long menuItemId;
	private final String menuItemName;
	private final List<RecipeComponentResponse> components;

	public RecipeResponse(Long menuItemId, String menuItemName, List<RecipeComponentResponse> components) {
		this.menuItemId = menuItemId;
		this.menuItemName = menuItemName;
		this.components = components;
	}

	public Long getMenuItemId() {
		return menuItemId;
	}

	public String getMenuItemName() {
		return menuItemName;
	}

	public List<RecipeComponentResponse> getComponents() {
		return components;
	}
}
