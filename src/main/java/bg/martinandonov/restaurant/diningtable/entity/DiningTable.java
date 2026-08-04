package bg.martinandonov.restaurant.diningtable.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "dining_tables",
		uniqueConstraints = @UniqueConstraint(name = "uk_dining_tables_table_number", columnNames = "table_number"))
public class DiningTable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "table_number", nullable = false)
	private Integer tableNumber;

	@Column(name = "display_name", length = 100)
	private String displayName;

	@Column(name = "capacity", nullable = false)
	private Integer capacity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private DiningTableStatus status = DiningTableStatus.AVAILABLE;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected DiningTable() {
	}

	public DiningTable(Integer tableNumber, String displayName, Integer capacity) {
		this.tableNumber = Objects.requireNonNull(tableNumber, "tableNumber must not be null");
		this.displayName = displayName;
		this.capacity = Objects.requireNonNull(capacity, "capacity must not be null");
		this.status = DiningTableStatus.AVAILABLE;
		this.active = true;
	}

	public Long getId() {
		return id;
	}

	public Integer getTableNumber() {
		return tableNumber;
	}

	public void setTableNumber(Integer tableNumber) {
		this.tableNumber = Objects.requireNonNull(tableNumber, "tableNumber must not be null");
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public void setCapacity(Integer capacity) {
		this.capacity = Objects.requireNonNull(capacity, "capacity must not be null");
	}

	public DiningTableStatus getStatus() {
		return status;
	}

	public void setStatus(DiningTableStatus status) {
		this.status = Objects.requireNonNull(status, "status must not be null");
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Long getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof DiningTable table)) {
			return false;
		}
		return id != null && Objects.equals(id, table.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		return "DiningTable{id=" + id + ", tableNumber=" + tableNumber
				+ ", capacity=" + capacity + ", status=" + status + ", active=" + active + "}";
	}
}
