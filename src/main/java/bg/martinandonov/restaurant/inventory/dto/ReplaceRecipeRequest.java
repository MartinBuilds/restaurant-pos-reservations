package bg.martinandonov.restaurant.inventory.dto;

import java.util.List;

public class ReplaceRecipeRequest {

	private List<RecipeComponentRequest> components;

	public List<RecipeComponentRequest> getComponents() {
		return components;
	}

	public void setComponents(List<RecipeComponentRequest> components) {
		this.components = components;
	}
}
