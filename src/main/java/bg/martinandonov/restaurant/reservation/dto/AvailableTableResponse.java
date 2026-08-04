package bg.martinandonov.restaurant.reservation.dto;

public class AvailableTableResponse {

	private final Long diningTableId;
	private final Integer tableNumber;
	private final String displayName;
	private final Integer capacity;

	public AvailableTableResponse(
			Long diningTableId,
			Integer tableNumber,
			String displayName,
			Integer capacity) {
		this.diningTableId = diningTableId;
		this.tableNumber = tableNumber;
		this.displayName = displayName;
		this.capacity = capacity;
	}

	public Long getDiningTableId() {
		return diningTableId;
	}

	public Integer getTableNumber() {
		return tableNumber;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Integer getCapacity() {
		return capacity;
	}
}
