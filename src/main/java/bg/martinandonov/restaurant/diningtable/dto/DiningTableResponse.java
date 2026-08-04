package bg.martinandonov.restaurant.diningtable.dto;

public class DiningTableResponse {

	private final Long id;
	private final Integer tableNumber;
	private final String displayName;
	private final Integer capacity;
	private final String status;
	private final boolean active;
	private final Long version;

	public DiningTableResponse(
			Long id,
			Integer tableNumber,
			String displayName,
			Integer capacity,
			String status,
			boolean active,
			Long version) {
		this.id = id;
		this.tableNumber = tableNumber;
		this.displayName = displayName;
		this.capacity = capacity;
		this.status = status;
		this.active = active;
		this.version = version;
	}

	public Long getId() {
		return id;
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

	public String getStatus() {
		return status;
	}

	public boolean isActive() {
		return active;
	}

	public Long getVersion() {
		return version;
	}
}
